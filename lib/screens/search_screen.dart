import 'package:flutter/material.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/app_info.dart';

// Theme and style constants
const _kSearchPadding = EdgeInsets.all(16.0);
const _kItemPadding = EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0);
const _kBorderRadius = 12.0;
const _kFontSize = 18.0;
const _kCursorWidth = 2.0;

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

  @override
  void initState() {
    super.initState();
    _loadInstalledApps();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (widget.autoFocus && mounted) {
      Future.microtask(() => _searchFocusNode.requestFocus());
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    _searchFocusNode.dispose();
    super.dispose();
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

  Widget _buildSearchField() {
    return Padding(
      padding: _kSearchPadding,
      child: TextField(
        controller: _searchController,
        focusNode: _searchFocusNode,
        autofocus: widget.autoFocus,
        showCursor: true,
        cursorColor: Colors.black,
        cursorWidth: _kCursorWidth,
        cursorRadius: const Radius.circular(1),
        cursorOpacityAnimates: false,
        decoration: InputDecoration(
          hintText: 'Search apps...',
          prefixIcon: const Icon(Icons.search),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(_kBorderRadius),
            borderSide: BorderSide.none,
          ),
          filled: true,
          fillColor: Theme.of(context).colorScheme.surfaceContainerHighest,
        ),
        onChanged: _filterApps,
      ),
    );
  }

  Widget _buildAppList() {
    if (_isLoading) {
      return const Center(
          child: Text('Loading...', style: TextStyle(fontSize: _kFontSize)));
    }

    if (_errorMessage != null) {
      return Padding(
        padding: _kSearchPadding,
        child:
            Text(_errorMessage!, style: const TextStyle(color: Colors.black)),
      );
    }

    return Expanded(
      child: ScrollConfiguration(
        behavior: NoGlowScrollBehavior(),
        child: ListView.builder(
          itemCount: _searchResults.length,
          itemBuilder: (context, index) => _buildAppItem(_searchResults[index]),
        ),
      ),
    );
  }

  Widget _buildAppItem(AppInfo app) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => _launchApp(app.packageName),
        splashColor: Colors.transparent,
        highlightColor: Colors.transparent,
        hoverColor: Colors.transparent,
        child: Padding(
          padding: _kItemPadding,
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
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.only(top: 16.0),
        child: Scaffold(
          backgroundColor: Colors.white,
          appBar: AppBar(
            title: const Text('Search apps'),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => Navigator.pop(context),
            ),
          ),
          body: Column(
            children: [
              _buildSearchField(),
              _buildAppList(),
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
