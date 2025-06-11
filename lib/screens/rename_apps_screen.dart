import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:installed_apps/installed_apps.dart';
import '../models/app_info.dart';
import 'dart:convert';

// Theme and style constants
const _kFontSize = 18.0;
const _kItemHeight = 56.0; // Altura fija de cada item de la lista

class RenameAppsScreen extends StatefulWidget {
  final SharedPreferences prefs;
  final List<String> selectedApps;

  const RenameAppsScreen({
    super.key,
    required this.prefs,
    required this.selectedApps,
  });

  @override
  State<RenameAppsScreen> createState() => _RenameAppsScreenState();
}

class _RenameAppsScreenState extends State<RenameAppsScreen> {
  late List<String> apps;
  List<AppInfo> appInfos = [];
  Map<String, String> customNames = {};
  bool isLoading = true;
  String? errorMessage;
  int _currentPage = 0;
  int _itemsPerPage = 10;
  bool _usePagination = true;
  String? _editingPackageName;
  final TextEditingController _editingController = TextEditingController();

  @override
  void initState() {
    super.initState();
    apps = List.from(widget.selectedApps);
    _usePagination = widget.prefs.getBool('usePagination') ?? true;
    _loadAppInfos();
    _loadCustomNames();
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

    final availableHeight = screenHeight -
        appBarHeight -
        paginationHeight -
        topPadding -
        bottomPadding;

    _itemsPerPage = (availableHeight / _kItemHeight).floor().clamp(3, 20);
  }

  Future<void> _loadCustomNames() async {
    final customNamesJson = widget.prefs.getString('customAppNames');
    if (customNamesJson != null) {
      try {
        final Map<String, dynamic> decoded = json.decode(customNamesJson);
        customNames = Map<String, String>.from(decoded);
      } catch (e) {
        debugPrint('Error loading custom names: $e');
        customNames = {};
      }
    } else {
      customNames = {};
    }
  }

  Future<void> _saveCustomNames() async {
    try {
      final jsonString = json.encode(customNames);
      await widget.prefs.setString('customAppNames', jsonString);
      await widget.prefs.reload();
    } catch (e) {
      debugPrint('Error saving custom names: $e');
    }
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

  @override
  void dispose() {
    _editingController.dispose();
    super.dispose();
  }

  void _startEditing(AppInfo app) {
    setState(() {
      _editingPackageName = app.packageName;
      _editingController.text = customNames[app.packageName] ?? app.name;
    });
  }

  void _cancelEditing() {
    setState(() {
      _editingPackageName = null;
      _editingController.clear();
    });
  }

  void _saveEditing() {
    if (_editingPackageName != null &&
        _editingController.text.trim().isNotEmpty) {
      setState(() {
        customNames[_editingPackageName!] = _editingController.text.trim();
        _editingPackageName = null;
        _editingController.clear();
      });
      _saveCustomNames();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Scaffold(
        backgroundColor: Colors.white,
        appBar: AppBar(
          title: const Text('Rename Apps'),
          backgroundColor: Colors.white,
          foregroundColor: Colors.black,
          elevation: 0,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: () => Navigator.pop(context, customNames),
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
                    children: [
                      Expanded(
                        child: ListView.builder(
                          itemCount: _currentPageItems.length,
                          itemBuilder: (context, index) {
                            final app = _currentPageItems[index];
                            final isEditing =
                                _editingPackageName == app.packageName;

                            return ListTile(
                              title: isEditing
                                  ? TextField(
                                      controller: _editingController,
                                      autofocus: true,
                                      style: const TextStyle(
                                        fontSize: _kFontSize,
                                        fontWeight: FontWeight.bold,
                                      ),
                                      decoration: const InputDecoration(
                                        hintText: 'Enter new name',
                                        border: InputBorder.none,
                                      ),
                                    )
                                  : Text(
                                      customNames[app.packageName] ?? app.name,
                                      style: const TextStyle(
                                        fontSize: _kFontSize,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                              trailing: isEditing
                                  ? Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        IconButton(
                                          icon: const Icon(Icons.check_rounded),
                                          onPressed: _saveEditing,
                                        ),
                                        IconButton(
                                          icon: const Icon(Icons.close_rounded),
                                          onPressed: _cancelEditing,
                                        ),
                                      ],
                                    )
                                  : const Icon(Icons.edit_rounded),
                              onTap:
                                  isEditing ? null : () => _startEditing(app),
                            );
                          },
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
                                onPressed:
                                    _currentPage > 0 ? _previousPage : null,
                              ),
                              Text(
                                'Page ${_currentPage + 1} of $_totalPages',
                                style: const TextStyle(
                                  fontSize: _kFontSize,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              IconButton(
                                icon: const Icon(Icons.chevron_right_rounded),
                                onPressed: _currentPage < _totalPages - 1
                                    ? _nextPage
                                    : null,
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
