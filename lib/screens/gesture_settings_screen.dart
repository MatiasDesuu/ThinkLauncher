import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:installed_apps/installed_apps.dart';
import '../models/app_info.dart';

// Theme and style constants
const _kFontSize = 18.0;
const _kSubtitleFontSize = 12.0;
const _kPadding = EdgeInsets.all(16.0);
const _kHorizontalPadding = EdgeInsets.symmetric(horizontal: 16.0);

class GestureSettingsScreen extends StatefulWidget {
  final SharedPreferences prefs;

  const GestureSettingsScreen({super.key, required this.prefs});

  @override
  State<GestureSettingsScreen> createState() => _GestureSettingsScreenState();
}

class _GestureSettingsScreenState extends State<GestureSettingsScreen> {
  bool _enableSearchGesture = true;
  bool _autoFocusSearch = true;
  bool _enableLongPressGesture = true;
  String? _swipeLeftApp;
  String? _swipeRightApp;
  String? _swipeDownApp;
  String? _swipeUpApp;
  bool _useSearchForSwipeLeft = true;
  bool _useSearchForSwipeRight = true;
  bool _useSearchForSwipeDown = true;
  bool _useSearchForSwipeUp = true;
  final Map<String, AppInfo> _appInfoCache = {};
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
    _preloadAppInfo();
  }

  Future<void> _preloadAppInfo() async {
    setState(() {
      _isLoading = true;
    });

    try {
      if (_swipeLeftApp != null) {
        await _getAppInfo(_swipeLeftApp!);
      }
      if (_swipeRightApp != null) {
        await _getAppInfo(_swipeRightApp!);
      }
      if (_swipeDownApp != null) {
        await _getAppInfo(_swipeDownApp!);
      }
      if (_swipeUpApp != null) {
        await _getAppInfo(_swipeUpApp!);
      }
    } catch (e) {
      debugPrint('Error preloading app info: $e');
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<AppInfo> _getAppInfo(String packageName) async {
    if (_appInfoCache.containsKey(packageName)) {
      return _appInfoCache[packageName]!;
    }

    try {
      final app = await InstalledApps.getAppInfo(packageName, null);
      final appInfo = AppInfo.fromInstalledApps(app);
      _appInfoCache[packageName] = appInfo;
      return appInfo;
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
  }

  void _loadSettings() {
    setState(() {
      _enableSearchGesture =
          widget.prefs.getBool('enableSearchGesture') ?? true;
      _autoFocusSearch = widget.prefs.getBool('autoFocusSearch') ?? true;
      _enableLongPressGesture =
          widget.prefs.getBool('enableLongPressGesture') ?? true;
      _swipeLeftApp = widget.prefs.getString('swipeLeftApp');
      _swipeRightApp = widget.prefs.getString('swipeRightApp');
      _swipeDownApp = widget.prefs.getString('swipeDownApp');
      _swipeUpApp = widget.prefs.getString('swipeUpApp');
      _useSearchForSwipeLeft =
          widget.prefs.getBool('useSearchForSwipeLeft') ?? true;
      _useSearchForSwipeRight =
          widget.prefs.getBool('useSearchForSwipeRight') ?? true;
      _useSearchForSwipeDown =
          widget.prefs.getBool('useSearchForSwipeDown') ?? true;
      _useSearchForSwipeUp =
          widget.prefs.getBool('useSearchForSwipeUp') ?? true;
    });
  }

  Future<void> _saveSettings() async {
    await widget.prefs.setBool('enableSearchGesture', _enableSearchGesture);
    await widget.prefs.setBool('autoFocusSearch', _autoFocusSearch);
    await widget.prefs
        .setBool('enableLongPressGesture', _enableLongPressGesture);
    await widget.prefs.setBool('useSearchForSwipeLeft', _useSearchForSwipeLeft);
    await widget.prefs
        .setBool('useSearchForSwipeRight', _useSearchForSwipeRight);
    await widget.prefs.setBool('useSearchForSwipeDown', _useSearchForSwipeDown);
    await widget.prefs.setBool('useSearchForSwipeUp', _useSearchForSwipeUp);
    if (_swipeLeftApp != null) {
      await widget.prefs.setString('swipeLeftApp', _swipeLeftApp!);
    }
    if (_swipeRightApp != null) {
      await widget.prefs.setString('swipeRightApp', _swipeRightApp!);
    }
    if (_swipeDownApp != null) {
      await widget.prefs.setString('swipeDownApp', _swipeDownApp!);
    }
    if (_swipeUpApp != null) {
      await widget.prefs.setString('swipeUpApp', _swipeUpApp!);
    }
  }

  Future<void> _selectSwipeLeftApp() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => SwipeAppSelectionScreen(
          prefs: widget.prefs,
          selectedApp: _swipeLeftApp,
          useSearch: _useSearchForSwipeLeft,
          direction: 'SwipeLeft',
          title: 'Swipe left action',
        ),
      ),
    );

    if (result != null) {
      setState(() {
        _useSearchForSwipeLeft = result['useSearch'] as bool;
        _swipeLeftApp = result['app'] as String?;
        widget.prefs.setBool('useSearchForSwipeLeft', _useSearchForSwipeLeft);
        if (_swipeLeftApp != null) {
          widget.prefs.setString('swipeLeftApp', _swipeLeftApp!);
          _getAppInfo(_swipeLeftApp!);
        }
      });
    }
  }

  Future<void> _selectSwipeRightApp() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => SwipeAppSelectionScreen(
          prefs: widget.prefs,
          selectedApp: _swipeRightApp,
          useSearch: _useSearchForSwipeRight,
          direction: 'SwipeRight',
          title: 'Swipe right action',
        ),
      ),
    );

    if (result != null) {
      setState(() {
        _useSearchForSwipeRight = result['useSearch'] as bool;
        _swipeRightApp = result['app'] as String?;
        widget.prefs.setBool('useSearchForSwipeRight', _useSearchForSwipeRight);
        if (_swipeRightApp != null) {
          widget.prefs.setString('swipeRightApp', _swipeRightApp!);
          _getAppInfo(_swipeRightApp!);
        }
      });
    }
  }

  Future<void> _selectSwipeDownApp() async {
    final result = await Navigator.push(
      context,
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) =>
            SwipeAppSelectionScreen(
          prefs: widget.prefs,
          selectedApp: _swipeDownApp,
          useSearch: _useSearchForSwipeDown,
          direction: 'SwipeDown',
          title: 'Swipe down action',
        ),
        transitionDuration: Duration.zero,
        reverseTransitionDuration: Duration.zero,
      ),
    );

    if (result != null && result is Map<String, dynamic>) {
      setState(() {
        _useSearchForSwipeDown = result['useSearch'] as bool;
        _swipeDownApp = result['app'] as String?;
        final isEnabled = result['enabled'] as bool;
        widget.prefs.setBool('enableSwipeDown', isEnabled);
      });
      _saveSettings();
      if (_swipeDownApp != null) {
        await _getAppInfo(_swipeDownApp!);
      }
    }
  }

  Future<void> _selectSwipeUpApp() async {
    final result = await Navigator.push(
      context,
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) =>
            SwipeAppSelectionScreen(
          prefs: widget.prefs,
          selectedApp: _swipeUpApp,
          useSearch: _useSearchForSwipeUp,
          direction: 'SwipeUp',
          title: 'Swipe up action',
        ),
        transitionDuration: Duration.zero,
        reverseTransitionDuration: Duration.zero,
      ),
    );

    if (result != null && result is Map<String, dynamic>) {
      setState(() {
        _useSearchForSwipeUp = result['useSearch'] as bool;
        _swipeUpApp = result['app'] as String?;
        final isEnabled = result['enabled'] as bool;
        widget.prefs.setBool('enableSwipeUp', isEnabled);
      });
      _saveSettings();
      if (_swipeUpApp != null) {
        await _getAppInfo(_swipeUpApp!);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final showSettingsButton =
        widget.prefs.getBool('showSettingsButton') ?? true;

    return Container(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.only(top: 16.0),
        child: Scaffold(
          backgroundColor: Colors.white,
          appBar: AppBar(
            title: const Text('Gestures'),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => Navigator.pop(context),
            ),
          ),
          body: _isLoading
              ? const Center(
                  child: Text(
                    'Loading...',
                    style: TextStyle(fontSize: _kFontSize),
                  ),
                )
              : Theme(
                  data: Theme.of(context).copyWith(
                    splashColor: Colors.transparent,
                    highlightColor: Colors.transparent,
                    hoverColor: Colors.transparent,
                    focusColor: Colors.transparent,
                  ),
                  child: ScrollConfiguration(
                    behavior: NoGlowScrollBehavior(),
                    child: ListView(
                      physics: const ClampingScrollPhysics(),
                      children: [
                        ListTile(
                          title: const Text(
                            'Swipe Down',
                            style: TextStyle(
                              fontSize: _kFontSize,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          subtitle: !(widget.prefs.getBool('enableSwipeDown') ??
                                  true)
                              ? const Text(
                                  'Disabled',
                                  style:
                                      TextStyle(fontSize: _kSubtitleFontSize),
                                )
                              : _useSearchForSwipeDown
                                  ? const Text(
                                      'Open search screen',
                                      style: TextStyle(
                                          fontSize: _kSubtitleFontSize),
                                    )
                                  : _swipeDownApp != null &&
                                          _appInfoCache
                                              .containsKey(_swipeDownApp!)
                                      ? Text(
                                          _appInfoCache[_swipeDownApp!]!.name,
                                          style: const TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        )
                                      : const Text(
                                          'Not selected',
                                          style: TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        ),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => _selectSwipeDownApp(),
                        ),
                        ListTile(
                          title: const Text(
                            'Swipe Up',
                            style: TextStyle(
                              fontSize: _kFontSize,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          subtitle: !(widget.prefs.getBool('enableSwipeUp') ??
                                  true)
                              ? const Text(
                                  'Disabled',
                                  style:
                                      TextStyle(fontSize: _kSubtitleFontSize),
                                )
                              : _useSearchForSwipeUp
                                  ? const Text(
                                      'Open search screen',
                                      style: TextStyle(
                                          fontSize: _kSubtitleFontSize),
                                    )
                                  : _swipeUpApp != null &&
                                          _appInfoCache
                                              .containsKey(_swipeUpApp!)
                                      ? Text(
                                          _appInfoCache[_swipeUpApp!]!.name,
                                          style: const TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        )
                                      : const Text(
                                          'Not selected',
                                          style: TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        ),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => _selectSwipeUpApp(),
                        ),
                        ListTile(
                          title: const Text(
                            'Swipe Left',
                            style: TextStyle(
                              fontSize: _kFontSize,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          subtitle: !(widget.prefs.getBool('enableSwipeLeft') ??
                                  true)
                              ? const Text(
                                  'Disabled',
                                  style:
                                      TextStyle(fontSize: _kSubtitleFontSize),
                                )
                              : _useSearchForSwipeLeft
                                  ? const Text(
                                      'Open search screen',
                                      style: TextStyle(
                                          fontSize: _kSubtitleFontSize),
                                    )
                                  : _swipeLeftApp != null &&
                                          _appInfoCache
                                              .containsKey(_swipeLeftApp!)
                                      ? Text(
                                          _appInfoCache[_swipeLeftApp!]!.name,
                                          style: const TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        )
                                      : const Text(
                                          'Not selected',
                                          style: TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        ),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => _selectSwipeLeftApp(),
                        ),
                        ListTile(
                          title: const Text(
                            'Swipe Right',
                            style: TextStyle(
                              fontSize: _kFontSize,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          subtitle: !(widget.prefs
                                      .getBool('enableSwipeRight') ??
                                  true)
                              ? const Text(
                                  'Disabled',
                                  style:
                                      TextStyle(fontSize: _kSubtitleFontSize),
                                )
                              : _useSearchForSwipeRight
                                  ? const Text(
                                      'Open search screen',
                                      style: TextStyle(
                                          fontSize: _kSubtitleFontSize),
                                    )
                                  : _swipeRightApp != null &&
                                          _appInfoCache
                                              .containsKey(_swipeRightApp!)
                                      ? Text(
                                          _appInfoCache[_swipeRightApp!]!.name,
                                          style: const TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        )
                                      : const Text(
                                          'Not selected',
                                          style: TextStyle(
                                              fontSize: _kSubtitleFontSize),
                                        ),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => _selectSwipeRightApp(),
                        ),
                        SwitchListTile(
                          title: const Text(
                            'Auto Focus Search',
                            style: TextStyle(
                              fontSize: _kFontSize,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          subtitle: const Text(
                            'Cursor will be positioned in the search field when opened',
                            style: TextStyle(fontSize: _kSubtitleFontSize),
                          ),
                          value: _autoFocusSearch,
                          onChanged: (value) {
                            setState(() {
                              _autoFocusSearch = value;
                            });
                            _saveSettings();
                          },
                        ),
                        SwitchListTile(
                          title: const Text(
                            'Enable Long Press Gesture',
                            style: TextStyle(
                              fontSize: _kFontSize,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          subtitle: const Text(
                            'Long press to open settings',
                            style: TextStyle(fontSize: _kSubtitleFontSize),
                          ),
                          value: _enableLongPressGesture,
                          onChanged: showSettingsButton
                              ? (value) {
                                  setState(() {
                                    _enableLongPressGesture = value;
                                  });
                                  _saveSettings();
                                }
                              : null,
                        ),
                        if (!showSettingsButton)
                          Padding(
                            padding: _kHorizontalPadding,
                            child: Text(
                              'Settings button is disabled. Enable it in settings to use this gesture.',
                              style: TextStyle(
                                color: Theme.of(context).colorScheme.error,
                                fontSize: _kSubtitleFontSize,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
        ),
      ),
    );
  }
}

class SwipeAppSelectionScreen extends StatefulWidget {
  final SharedPreferences prefs;
  final String? selectedApp;
  final bool useSearch;
  final String direction;
  final String title;

  const SwipeAppSelectionScreen({
    super.key,
    required this.prefs,
    this.selectedApp,
    required this.useSearch,
    required this.direction,
    required this.title,
  });

  @override
  State<SwipeAppSelectionScreen> createState() =>
      _SwipeAppSelectionScreenState();
}

class _SwipeAppSelectionScreenState extends State<SwipeAppSelectionScreen> {
  List<AppInfo> _apps = [];
  List<AppInfo> _filteredApps = [];
  final TextEditingController _searchController = TextEditingController();
  String? _selectedApp;
  bool _isLoading = true;
  String? _errorMessage;
  bool _useSearch = true;
  bool _isEnabled = true;
  bool _isVerticalGesture = false;

  @override
  void initState() {
    super.initState();
    _selectedApp = widget.selectedApp;
    _useSearch = widget.useSearch;
    _isEnabled = widget.prefs.getBool('enable${widget.direction}') ?? true;
    _isVerticalGesture =
        widget.direction == 'SwipeUp' || widget.direction == 'SwipeDown';
    _loadApps();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadApps() async {
    try {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });

      final installedApps = await InstalledApps.getInstalledApps(
        false, // includeSystemApps
        false, // withIcon
        '', // packageNamePrefix
      );

      final appInfos =
          installedApps.map((app) => AppInfo.fromInstalledApps(app)).toList();
      appInfos.sort((a, b) => a.name.compareTo(b.name));

      if (mounted) {
        setState(() {
          _apps = appInfos;
          _filteredApps = List.from(appInfos);
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading apps: $e');
      if (mounted) {
        setState(() {
          _errorMessage = 'Error loading applications';
          _isLoading = false;
        });
      }
    }
  }

  void _filterApps(String query) {
    setState(() {
      if (query.isEmpty) {
        _filteredApps = List.from(_apps);
      } else {
        _filteredApps = _apps
            .where(
                (app) => app.name.toLowerCase().contains(query.toLowerCase()))
            .toList();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final enableScroll = widget.prefs.getBool('enableScroll') ?? true;
    final canEnableGesture = !_isVerticalGesture || !enableScroll;

    return Container(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.only(top: 16.0),
        child: Scaffold(
          backgroundColor: Colors.white,
          appBar: AppBar(
            title: Text(widget.title),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => Navigator.pop(context, {
                'useSearch': _useSearch,
                'app': _selectedApp,
                'enabled': _isEnabled,
              }),
            ),
          ),
          body: Column(
            children: [
              SwitchListTile(
                title: Text(
                  'Enable ${widget.direction.replaceAll('Swipe', 'Swipe ')}',
                  style: const TextStyle(
                    fontSize: _kFontSize,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                subtitle: Text(
                  'Enable or disable the ${widget.direction.toLowerCase()} gesture',
                  style: const TextStyle(fontSize: _kSubtitleFontSize),
                ),
                value: _isEnabled,
                onChanged: canEnableGesture
                    ? (value) {
                        setState(() {
                          _isEnabled = value;
                        });
                        widget.prefs
                            .setBool('enable${widget.direction}', value);
                      }
                    : null,
              ),
              if (!canEnableGesture)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16.0),
                  child: Text(
                    'List scrolling is enabled. Disable it in settings to use vertical swipe gestures.',
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                      fontSize: _kSubtitleFontSize,
                    ),
                  ),
                ),
              if (_isEnabled && canEnableGesture) ...[
                SwitchListTile(
                  title: const Text(
                    'Use Search Screen',
                    style: TextStyle(
                      fontSize: _kFontSize,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  subtitle: Text(
                    'Open search screen when swiping ${widget.direction.toLowerCase().replaceAll('swipe', '')}',
                    style: const TextStyle(fontSize: _kSubtitleFontSize),
                  ),
                  value: _useSearch,
                  onChanged: (value) {
                    setState(() {
                      _useSearch = value;
                      if (value) {
                        _selectedApp = null;
                      }
                    });
                  },
                ),
                if (!_useSearch) ...[
                  Padding(
                    padding: _kPadding,
                    child: TextField(
                      controller: _searchController,
                      autofocus: false,
                      showCursor: true,
                      cursorColor: Colors.black,
                      cursorWidth: 2,
                      cursorRadius: const Radius.circular(1),
                      cursorOpacityAnimates: false,
                      decoration: InputDecoration(
                        hintText: 'Search Apps...',
                        prefixIcon: const Icon(Icons.search),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide.none,
                        ),
                        filled: true,
                        fillColor: Theme.of(context)
                            .colorScheme
                            .surfaceContainerHighest,
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
                            : ScrollConfiguration(
                                behavior: NoGlowScrollBehavior(),
                                child: ListView.builder(
                                  physics: const ClampingScrollPhysics(),
                                  itemCount: _filteredApps.length,
                                  itemBuilder: (context, index) {
                                    final app = _filteredApps[index];
                                    final isSelected =
                                        app.packageName == _selectedApp;
                                    return Material(
                                      color: Colors.transparent,
                                      child: InkWell(
                                        onTap: () {
                                          setState(() {
                                            _selectedApp = app.packageName;
                                          });
                                          Navigator.pop(context, {
                                            'useSearch': _useSearch,
                                            'app': _selectedApp,
                                            'enabled': _isEnabled,
                                          });
                                        },
                                        splashColor: Colors.transparent,
                                        highlightColor: Colors.transparent,
                                        hoverColor: Colors.transparent,
                                        child: Padding(
                                          padding: const EdgeInsets.symmetric(
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
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                  overflow:
                                                      TextOverflow.ellipsis,
                                                  maxLines: 1,
                                                ),
                                              ),
                                              if (isSelected)
                                                const Icon(
                                                  Icons.check_circle,
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
                ],
              ],
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
