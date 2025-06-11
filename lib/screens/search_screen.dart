import 'package:flutter/material.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/app_info.dart';

// Theme and style constants
const _kSearchPadding = EdgeInsets.all(16.0);
const _kFontSize = 18.0;
const _kItemHeight = 48.0; // Altura fija de cada item de la lista

// Static cache for app list
class _AppCache {
  static List<AppInfo>? _cachedApps;
  static String? _lastPackageList;
  static bool _isInitialized = false;

  static bool get isCacheValid {
    return _cachedApps != null && _lastPackageList != null;
  }

  static List<AppInfo>? get cachedApps => isCacheValid ? _cachedApps : null;

  static Future<void> updateCache(List<AppInfo> apps) async {
    _cachedApps = apps;
    _lastPackageList = apps.map((app) => app.packageName).join(',');
    _isInitialized = true;
  }

  static Future<bool> hasAppsChanged() async {
    if (!_isInitialized) return true;

    try {
      final currentApps =
          await InstalledApps.getInstalledApps(false, false, '');
      final currentPackageList =
          currentApps.map((app) => app.packageName).join(',');
      return currentPackageList != _lastPackageList;
    } catch (e) {
      debugPrint('Error checking apps changes: $e');
      return true;
    }
  }
}

class SearchScreen extends StatefulWidget {
  final SharedPreferences prefs;
  final bool autoFocus;

  const SearchScreen({
    super.key,
    required this.prefs,
    this.autoFocus = true,
  });

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _searchFocusNode = FocusNode();
  List<AppInfo> _searchResults = [];
  List<AppInfo> _installedApps = [];
  String? _errorMessage;
  bool _isLoading = true;
  int _currentPage = 0;
  bool _usePagination = true;
  int _appsPerPage = 10;

  @override
  void initState() {
    super.initState();
    _usePagination = widget.prefs.getBool('usePagination') ?? true;
    _searchController.addListener(() {
      if (mounted) setState(() {});
    });
    _loadInstalledApps();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (widget.autoFocus && mounted) {
      Future.microtask(() => _searchFocusNode.requestFocus());
    }
    // Calcular apps por página cuando cambian las dependencias (tamaño de pantalla)
    _calculateAppsPerPage();
  }

  void _calculateAppsPerPage() {
    if (!mounted) return;

    final mediaQuery = MediaQuery.of(context);
    final screenHeight = mediaQuery.size.height;
    final appBarHeight = AppBar().preferredSize.height;
    const searchFieldHeight = 72.0;
    const paginationHeight = 64.0;
    final topPadding = mediaQuery.padding.top;
    final bottomPadding = mediaQuery.padding.bottom;

    final availableHeight = screenHeight -
        appBarHeight -
        searchFieldHeight -
        paginationHeight -
        topPadding -
        bottomPadding;

    final itemsThatFit = (availableHeight / _kItemHeight).floor();

    _appsPerPage = itemsThatFit.clamp(5, 20);
  }

  @override
  void dispose() {
    _searchController.dispose();
    _searchFocusNode.dispose();
    super.dispose();
  }

  List<AppInfo> get _currentPageApps {
    if (!_usePagination) return _searchResults;
    final start = _currentPage * _appsPerPage;
    return _searchResults.skip(start).take(_appsPerPage).toList();
  }

  int get _totalPages => (_searchResults.length / _appsPerPage).ceil();

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

  Future<void> _loadInstalledApps() async {
    if (!mounted) return;

    // Use cache if available
    final cachedApps = _AppCache.cachedApps;
    if (cachedApps != null) {
      setState(() {
        _installedApps = cachedApps;
        _searchResults = List.from(cachedApps);
        _isLoading = false;
      });

      // Check for changes in background
      _checkForAppChanges();
      return;
    }

    await _loadAndCacheApps();
  }

  Future<void> _loadAndCacheApps() async {
    try {
      final installedApps =
          await InstalledApps.getInstalledApps(false, false, '');
      if (!mounted) return;

      final apps = installedApps.map(AppInfo.fromInstalledApps).toList();
      await _AppCache.updateCache(apps);

      setState(() {
        _installedApps = apps;
        _searchResults = List.from(apps);
        _isLoading = false;
      });
    } catch (e) {
      debugPrint('Error loading apps: $e');
      if (!mounted) return;

      setState(() {
        _errorMessage = 'Error loading applications';
        _isLoading = false;
      });
    }
  }

  Future<void> _checkForAppChanges() async {
    final hasChanges = await _AppCache.hasAppsChanged();
    if (hasChanges && mounted) {
      await _loadAndCacheApps();
    }
  }

  void _filterApps(String query) {
    if (!mounted) return;

    setState(() {
      _searchResults = query.isEmpty
          ? List.from(_installedApps)
          : _installedApps
              .where(
                  (app) => app.name.toLowerCase().contains(query.toLowerCase()))
              .toList();
      _currentPage = 0;
    });
  }

  Future<void> _launchApp(String packageName) async {
    try {
      await InstalledApps.startApp(packageName);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      debugPrint('Error opening app: $e');
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Could not open the application'),
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Scaffold(
        backgroundColor: Colors.white,
        appBar: AppBar(
          scrolledUnderElevation: 0,
          title: const Text('Search'),
          backgroundColor: Colors.white,
          foregroundColor: Colors.black,
          elevation: 0,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: () => Navigator.pop(context),
          ),
        ),
        body: Column(
          children: [
            Padding(
              padding: _kSearchPadding,
              child: TextField(
                controller: _searchController,
                focusNode: _searchFocusNode,
                autofocus: widget.autoFocus,
                showCursor: true,
                cursorColor: Colors.black,
                cursorWidth: 2,
                cursorRadius: const Radius.circular(1),
                cursorOpacityAnimates: false,
                decoration: InputDecoration(
                  hintText: 'Search Apps...',
                  prefixIcon: const Icon(Icons.search_rounded),
                  suffixIcon: _searchController.text.isNotEmpty
                      ? IconButton(
                          icon: const Icon(Icons.clear_rounded),
                          onPressed: () {
                            _searchController.clear();
                            _filterApps('');
                          },
                        )
                      : null,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                  filled: true,
                  fillColor:
                      Theme.of(context).colorScheme.surfaceContainerHighest,
                ),
                onChanged: _filterApps,
              ),
            ),
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: Text(
                        'Loading...',
                        style: TextStyle(fontSize: _kFontSize),
                      ),
                    )
                  : _errorMessage != null
                      ? Center(
                          child: Text(
                            _errorMessage!,
                            style: const TextStyle(fontSize: _kFontSize),
                          ),
                        )
                      : _searchResults.isEmpty
                          ? const Center(
                              child: Text(
                                'No apps found',
                                style: TextStyle(fontSize: _kFontSize),
                              ),
                            )
                          : Column(
                              children: [
                                Expanded(
                                  child: ScrollConfiguration(
                                    behavior: NoGlowScrollBehavior(),
                                    child: ListView.builder(
                                      physics: _usePagination
                                          ? const NeverScrollableScrollPhysics()
                                          : null,
                                      itemCount: _usePagination
                                          ? _currentPageApps.length
                                          : _searchResults.length,
                                      itemBuilder: (context, index) {
                                        final app = _usePagination
                                            ? _currentPageApps[index]
                                            : _searchResults[index];
                                        return Material(
                                          color: Colors.transparent,
                                          child: InkWell(
                                            onTap: () {
                                              _launchApp(app.packageName);
                                            },
                                            splashColor: Colors.transparent,
                                            highlightColor: Colors.transparent,
                                            hoverColor: Colors.transparent,
                                            child: Padding(
                                              padding:
                                                  const EdgeInsets.symmetric(
                                                horizontal: 16.0,
                                                vertical: 8.0,
                                              ),
                                              child: Row(
                                                children: [
                                                  Expanded(
                                                    child: Text(
                                                      app.name,
                                                      style: const TextStyle(
                                                        fontSize: _kFontSize,
                                                        fontWeight:
                                                            FontWeight.bold,
                                                      ),
                                                      overflow:
                                                          TextOverflow.ellipsis,
                                                      maxLines: 1,
                                                    ),
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
                                  Padding(
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 16.0, vertical: 8.0),
                                    child: Row(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: [
                                        IconButton(
                                          icon: const Icon(
                                              Icons.arrow_back_rounded,
                                              size: 32),
                                          iconSize: 32,
                                          onPressed: _currentPage > 0
                                              ? _previousPage
                                              : null,
                                          style: IconButton.styleFrom(
                                            padding: const EdgeInsets.all(12),
                                            minimumSize: const Size(48, 48),
                                          ),
                                        ),
                                        const SizedBox(width: 16),
                                        Text(
                                          'Page ${_currentPage + 1} of $_totalPages',
                                          style: const TextStyle(
                                              fontSize: 18,
                                              fontWeight: FontWeight.bold),
                                        ),
                                        const SizedBox(width: 16),
                                        IconButton(
                                          icon: const Icon(
                                              Icons.arrow_forward_rounded,
                                              size: 32),
                                          iconSize: 32,
                                          onPressed:
                                              _currentPage < _totalPages - 1
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
