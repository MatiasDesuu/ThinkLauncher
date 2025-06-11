import 'package:flutter/material.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/app_info.dart';

// Theme and style constants
const _kFontSize = 18.0;
const _kItemHeight = 48.0; // Altura fija de cada item de la lista

class AppSelectionScreen extends StatefulWidget {
  final SharedPreferences prefs;
  final List<String> selectedApps;
  final int maxApps;

  const AppSelectionScreen({
    super.key,
    required this.prefs,
    required this.selectedApps,
    required this.maxApps,
  });

  @override
  State<AppSelectionScreen> createState() => _AppSelectionScreenState();
}

class _AppSelectionScreenState extends State<AppSelectionScreen> {
  List<AppInfo> apps = [];
  List<AppInfo> filteredApps = [];
  final TextEditingController _searchController = TextEditingController();
  String? errorMessage;
  bool isLoading = false;
  int _currentPage = 0;
  int _itemsPerPage = 10;
  bool _usePagination = true;

  @override
  void initState() {
    super.initState();
    _usePagination = widget.prefs.getBool('usePagination') ?? true;
    _loadApps();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _calculateItemsPerPage();
  }

  void _calculateItemsPerPage() {
    if (!mounted) return;

    final mediaQuery = MediaQuery.of(context);
    final screenHeight = mediaQuery.size.height;
    final appBarHeight = AppBar().preferredSize.height;
    const searchBarHeight = 72.0; // Altura del campo de búsqueda
    const paginationHeight = 64.0;
    final topPadding = mediaQuery.padding.top;
    final bottomPadding = mediaQuery.padding.bottom;

    // Calcular el espacio disponible para la lista
    final availableHeight = screenHeight -
        appBarHeight -
        searchBarHeight -
        paginationHeight -
        topPadding -
        bottomPadding;

    // Calcular cuántos items caben en el espacio disponible
    final itemsThatFit = (availableHeight / _kItemHeight).floor();

    // Asegurarnos de que al menos haya 5 items por página
    _itemsPerPage = itemsThatFit.clamp(5, 20);

    // Si estamos en modo paginación, asegurarnos de que la página actual sea válida
    if (filteredApps.isNotEmpty) {
      final maxPage = (filteredApps.length / _itemsPerPage).ceil() - 1;
      if (_currentPage > maxPage) {
        _currentPage = maxPage.clamp(0, maxPage);
      }
    } else {
      _currentPage = 0;
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadApps() async {
    try {
      setState(() {
        isLoading = true;
        errorMessage = null;
      });

      final installedApps = await InstalledApps.getInstalledApps(
        false, // includeSystemApps
        false, // withIcon
        '', // packageNamePrefix
      );

      final appInfos =
          installedApps.map((app) => AppInfo.fromInstalledApps(app)).toList();

      // Sort apps by name
      appInfos.sort((a, b) => a.name.compareTo(b.name));

      if (mounted) {
        setState(() {
          apps = appInfos;
          filteredApps = List.from(appInfos);
          isLoading = false;
        });
        _calculateItemsPerPage();
      }
    } catch (e) {
      debugPrint('Error loading apps: $e');
      if (mounted) {
        setState(() {
          errorMessage = 'Error loading applications';
          isLoading = false;
        });
      }
    }
  }

  void _filterApps(String query) {
    setState(() {
      if (query.isEmpty) {
        filteredApps = List.from(apps);
      } else {
        filteredApps = apps
            .where(
                (app) => app.name.toLowerCase().contains(query.toLowerCase()))
            .toList();
      }
      _currentPage = 0; // Resetear a la primera página al filtrar
      _calculateItemsPerPage();
    });
  }

  void _selectApp(String packageName) {
    setState(() {
      if (widget.selectedApps.contains(packageName)) {
        widget.selectedApps.remove(packageName);
      } else if (widget.selectedApps.length < widget.maxApps) {
        widget.selectedApps.add(packageName);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
                'You have already selected the maximum of ${widget.maxApps} apps'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    });
  }

  void _nextPage() {
    if (_currentPage < _totalPages - 1) {
      setState(() {
        _currentPage++;
      });
    }
  }

  void _previousPage() {
    if (_currentPage > 0) {
      setState(() {
        _currentPage--;
      });
    }
  }

  int get _totalPages {
    if (filteredApps.isEmpty) return 0;
    return (filteredApps.length / _itemsPerPage).ceil();
  }

  List<AppInfo> get _currentPageItems {
    if (filteredApps.isEmpty) return [];
    final start = _currentPage * _itemsPerPage;
    final end = (start + _itemsPerPage).clamp(0, filteredApps.length);
    return filteredApps.sublist(start, end);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.only(top: 16.0),
        child: Scaffold(
          appBar: AppBar(
            title: const Text('Select apps'),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back_rounded),
              onPressed: () => Navigator.pop(context, widget.selectedApps),
            ),
          ),
          body: Column(
            children: [
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: TextField(
                  controller: _searchController,
                  autofocus: false,
                  showCursor: true,
                  cursorColor: Colors.black,
                  cursorWidth: 2,
                  cursorRadius: const Radius.circular(1),
                  cursorOpacityAnimates: false,
                  decoration: InputDecoration(
                    hintText: 'Search apps...',
                    prefixIcon: const Icon(Icons.search_rounded),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: BorderSide.none,
                    ),
                    filled: true,
                    fillColor:
                        Theme.of(context).colorScheme.surfaceContainerHighest,
                  ),
                  onChanged: _filterApps,
                ),
              ),
              if (errorMessage != null)
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Text(
                    errorMessage!,
                    style: const TextStyle(color: Colors.black),
                  ),
                )
              else if (isLoading)
                const Expanded(
                  child: Center(
                    child: Text(
                      'Loading...',
                      style: TextStyle(
                        fontSize: _kFontSize,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                )
              else if (filteredApps.isEmpty)
                const Expanded(
                  child: Center(
                    child: Text(
                      'No apps found',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                )
              else
                Expanded(
                  child: ScrollConfiguration(
                    behavior: NoGlowScrollBehavior(),
                    child: ListView.builder(
                      physics: _usePagination
                          ? const NeverScrollableScrollPhysics()
                          : null,
                      itemCount: _usePagination
                          ? _currentPageItems.length
                          : filteredApps.length,
                      itemBuilder: (context, index) {
                        final app = _usePagination
                            ? _currentPageItems[index]
                            : filteredApps[index];
                        final isSelected =
                            widget.selectedApps.contains(app.packageName);
                        final isMaxReached =
                            widget.selectedApps.length >= widget.maxApps &&
                                !isSelected;
                        return Material(
                          color: Colors.transparent,
                          child: InkWell(
                            onTap: isMaxReached
                                ? null
                                : () => _selectApp(app.packageName),
                            splashColor: Colors.transparent,
                            highlightColor: Colors.transparent,
                            hoverColor: Colors.transparent,
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 16.0, vertical: 8.0),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      app.name,
                                      style: TextStyle(
                                        fontSize: 18,
                                        fontWeight: FontWeight.bold,
                                        color: isMaxReached
                                            ? Colors.black.withAlpha(127)
                                            : Colors.black,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                      maxLines: 1,
                                    ),
                                  ),
                                  if (isSelected)
                                    const Icon(
                                      Icons.check_circle_rounded,
                                      color: Colors.black,
                                    ),
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
              if (_usePagination && _totalPages > 1)
                Container(
                  padding: const EdgeInsets.symmetric(vertical: 8.0),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surface,
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.chevron_left_rounded),
                        onPressed: _currentPage > 0 ? _previousPage : null,
                      ),
                      Text(
                        'Page ${_currentPage + 1} of $_totalPages',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.chevron_right_rounded),
                        onPressed:
                            _currentPage < _totalPages - 1 ? _nextPage : null,
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

// Class to remove any overscroll effect (glow, stretch, bounce)
class NoGlowScrollBehavior extends ScrollBehavior {
  @override
  Widget buildOverscrollIndicator(
      BuildContext context, Widget child, ScrollableDetails details) {
    return child;
  }
}
