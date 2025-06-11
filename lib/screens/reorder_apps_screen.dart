import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:installed_apps/installed_apps.dart';
import '../models/app_info.dart';

// Theme and style constants
const _kFontSize = 18.0;
const _kItemHeight = 56.0; // Altura fija de cada item de la lista

class ReorderAppsScreen extends StatefulWidget {
  final SharedPreferences prefs;
  final List<String> selectedApps;

  const ReorderAppsScreen({
    super.key,
    required this.prefs,
    required this.selectedApps,
  });

  @override
  State<ReorderAppsScreen> createState() => _ReorderAppsScreenState();
}

class _ReorderAppsScreenState extends State<ReorderAppsScreen> {
  late List<String> apps;
  List<AppInfo> appInfos = [];
  bool isLoading = true;
  String? errorMessage;
  int _currentPage = 0;
  int _itemsPerPage = 10;
  bool _usePagination = true;

  @override
  void initState() {
    super.initState();
    apps = List.from(widget.selectedApps);
    _usePagination = widget.prefs.getBool('usePagination') ?? true;
    _loadAppInfos();
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
    const paginationHeight = 64.0;
    final topPadding = mediaQuery.padding.top;
    final bottomPadding = mediaQuery.padding.bottom;

    // Calcular el espacio disponible para la lista
    final availableHeight = screenHeight -
        appBarHeight -
        paginationHeight -
        topPadding -
        bottomPadding;

    // Calcular cuántos items caben en el espacio disponible
    final itemsThatFit = (availableHeight / _kItemHeight).floor();

    // Asegurarnos de que al menos haya 5 items por página y que use todo el espacio disponible
    _itemsPerPage = itemsThatFit.clamp(5, 50);

    // Si estamos en modo paginación, asegurarnos de que la página actual sea válida
    if (appInfos.isNotEmpty) {
      final maxPage = (appInfos.length / _itemsPerPage).ceil() - 1;
      if (_currentPage > maxPage) {
        _currentPage = maxPage.clamp(0, maxPage);
      }
    } else {
      _currentPage = 0;
    }
  }

  void _moveItemUp(int index) {
    if (index > 0) {
      setState(() {
        final item = appInfos.removeAt(index);
        appInfos.insert(index - 1, item);
        apps = appInfos.map((e) => e.packageName).toList();
      });
    }
  }

  void _moveItemDown(int index) {
    if (index < appInfos.length - 1) {
      setState(() {
        final item = appInfos.removeAt(index);
        appInfos.insert(index + 1, item);
        apps = appInfos.map((e) => e.packageName).toList();
      });
    }
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
    if (appInfos.isEmpty) return 0;
    return (appInfos.length / _itemsPerPage).ceil();
  }

  List<AppInfo> get _currentPageItems {
    if (appInfos.isEmpty) return [];
    final start = _currentPage * _itemsPerPage;
    final end = (start + _itemsPerPage).clamp(0, appInfos.length);
    return appInfos.sublist(start, end);
  }

  Future<void> _loadAppInfos() async {
    try {
      setState(() {
        isLoading = true;
        errorMessage = null;
      });

      final futures = apps.map((packageName) async {
        try {
          final app = await InstalledApps.getAppInfo(packageName, null);
          return AppInfo.fromInstalledApps(app);
        } catch (e) {
          debugPrint('Error getting app info for $packageName: $e');
          return AppInfo(
            name: packageName,
            packageName: packageName,
            versionName: '',
            versionCode: 0,
            builtWith: BuiltWith.unknown,
            installedTimestamp: 0,
          );
        }
      });

      final results = await Future.wait(futures);

      if (mounted) {
        setState(() {
          appInfos = results;
          isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading app infos: $e');
      if (mounted) {
        setState(() {
          errorMessage = 'Error loading applications';
          isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Scaffold(
        backgroundColor: Colors.white,
        appBar: AppBar(
          title: const Text('Reorder Apps'),
          backgroundColor: Colors.white,
          foregroundColor: Colors.black,
          elevation: 0,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: () => Navigator.pop(context, apps),
          ),
        ),
        body: isLoading
            ? const Center(
                child: Text(
                  'Loading...',
                  style: TextStyle(fontSize: _kFontSize),
                ),
              )
            : errorMessage != null
                ? Center(
                    child: Text(
                      errorMessage!,
                      style: const TextStyle(fontSize: _kFontSize),
                    ),
                  )
                : Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Expanded(
                        child: ScrollConfiguration(
                          behavior: NoGlowScrollBehavior(),
                          child: ListView.builder(
                            shrinkWrap: true,
                            physics: _usePagination
                                ? const NeverScrollableScrollPhysics()
                                : null,
                            itemCount: _usePagination
                                ? _currentPageItems.length
                                : appInfos.length,
                            itemBuilder: (context, index) {
                              final app = _usePagination
                                  ? _currentPageItems[index]
                                  : appInfos[index];
                              final globalIndex = _usePagination
                                  ? _currentPage * _itemsPerPage + index
                                  : index;

                              return Material(
                                key: ValueKey(app.packageName),
                                color: Colors.transparent,
                                child: SizedBox(
                                  height: _kItemHeight,
                                  child: Padding(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 16.0,
                                    ),
                                    child: Row(
                                      children: [
                                        Container(
                                          width: 40,
                                          alignment: Alignment.center,
                                          child: Text(
                                            '${globalIndex + 1}',
                                            style: const TextStyle(
                                              fontSize: _kFontSize,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ),
                                        const SizedBox(width: 16),
                                        Expanded(
                                          child: Text(
                                            app.name,
                                            style: const TextStyle(
                                              fontSize: _kFontSize,
                                              fontWeight: FontWeight.bold,
                                            ),
                                            overflow: TextOverflow.ellipsis,
                                            maxLines: 1,
                                          ),
                                        ),
                                        IconButton(
                                          icon: const Icon(
                                              Icons.arrow_upward_rounded),
                                          onPressed: globalIndex > 0
                                              ? () => _moveItemUp(globalIndex)
                                              : null,
                                        ),
                                        IconButton(
                                          icon: const Icon(
                                              Icons.arrow_downward_rounded),
                                          onPressed: globalIndex <
                                                  appInfos.length - 1
                                              ? () => _moveItemDown(globalIndex)
                                              : null,
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
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              IconButton(
                                icon: const Icon(Icons.arrow_back_rounded,
                                    size: 32),
                                iconSize: 32,
                                onPressed:
                                    _currentPage > 0 ? _previousPage : null,
                                style: IconButton.styleFrom(
                                  padding: const EdgeInsets.all(12),
                                  minimumSize: const Size(48, 48),
                                ),
                              ),
                              const SizedBox(width: 16),
                              Text(
                                'Page ${_currentPage + 1} of $_totalPages',
                                style: const TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.bold),
                              ),
                              const SizedBox(width: 16),
                              IconButton(
                                icon: const Icon(Icons.arrow_forward_rounded,
                                    size: 32),
                                iconSize: 32,
                                onPressed: _currentPage < _totalPages - 1
                                    ? _nextPage
                                    : null,
                                style: IconButton.styleFrom(
                                  padding: const EdgeInsets.all(12),
                                  minimumSize: const Size(48, 48),
                                ),
                              ),
                            ],
                          ),
                        ),
                    ],
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
