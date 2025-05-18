import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:intl/intl.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import '../models/app_info.dart';
import '../gestures/gesture_handler.dart';
import 'settings_screen.dart';
import 'search_screen.dart';
import 'package:battery_plus/battery_plus.dart';

class MainScreen extends StatefulWidget {
  final SharedPreferences prefs;

  const MainScreen({super.key, required this.prefs});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> with WidgetsBindingObserver {
  // Cache for app information
  static final Map<String, AppInfo> _appInfoCache = {};

  // State variables
  late List<String> _selectedApps;
  late int _numApps;
  late int _numColumns;
  late bool _showDateTime;
  late bool _showSearchButton;
  late bool _showSettingsButton;
  late bool _useBoldFont;
  late double _appFontSize;
  late bool _enableScroll;
  late bool _showIcons;
  late String _currentTime;
  late String _currentDate;
  late int _batteryLevel;
  late bool _showAppTitles;
  late double _appIconSize;
  late bool _useBlackAndWhiteIcons;

  // Timers
  Timer? _dateTimeTimer;
  Timer? _batteryTimer;
  bool _isNavigating = false;
  final Battery _battery = Battery();

  // Formatters
  static final _timeFormatter = DateFormat('HH:mm');
  static final _dateFormatter = DateFormat('dd, MMMM - yyyy');

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _initializeState();
    _setupTimers();
    _setupBatteryListener();
  }

  void _initializeState() {
    _selectedApps = widget.prefs.getStringList('selectedApps') ?? [];
    _numApps = widget.prefs.getInt('numApps') ?? 5;
    _numColumns = widget.prefs.getInt('numColumns') ?? 1;
    _showDateTime = widget.prefs.getBool('showDateTime') ?? true;
    _showSearchButton = widget.prefs.getBool('showSearchButton') ?? true;
    _showSettingsButton = widget.prefs.getBool('showSettingsButton') ?? true;
    _useBoldFont = widget.prefs.getBool('useBoldFont') ?? false;
    _appFontSize = widget.prefs.getDouble('appFontSize') ?? 18.0;
    _enableScroll = widget.prefs.getBool('enableScroll') ?? true;
    _showIcons = widget.prefs.getBool('showIcons') ?? false;
    _showAppTitles = widget.prefs.getBool('showAppTitles') ?? true;
    _appIconSize = widget.prefs.getDouble('appIconSize') ?? 18.0;
    _useBlackAndWhiteIcons =
        widget.prefs.getBool('useBlackAndWhiteIcons') ?? false;
    _currentTime = _timeFormatter.format(DateTime.now());
    _currentDate = _dateFormatter.format(DateTime.now());
    _batteryLevel = 0;
  }

  void _setupTimers() {
    _dateTimeTimer =
        Timer.periodic(const Duration(minutes: 1), (_) => _updateDateTime());
    _batteryTimer =
        Timer.periodic(const Duration(minutes: 5), (_) => _updateBattery());
  }

  void _setupBatteryListener() {
    _battery.onBatteryStateChanged.listen((_) => _updateBattery());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _dateTimeTimer?.cancel();
    _batteryTimer?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _updateBattery();
      _updateDateTime();
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

  Future<void> _preloadAppInfo() async {
    if (_selectedApps.isEmpty) return;
    final futures = _selectedApps
        .where((packageName) => !_appInfoCache.containsKey(packageName))
        .map((packageName) => _getAppInfo(packageName));
    await Future.wait(futures);
  }

  void _loadSettings() {
    if (!mounted) return;

    Future.microtask(() async {
      if (!mounted) return;

      final newNumApps = widget.prefs.getInt('numApps') ?? 5;
      final newNumColumns = widget.prefs.getInt('numColumns') ?? 1;
      final newShowDateTime = widget.prefs.getBool('showDateTime') ?? true;
      final newShowSearchButton =
          widget.prefs.getBool('showSearchButton') ?? true;
      final newShowSettingsButton =
          widget.prefs.getBool('showSettingsButton') ?? true;
      final newUseBoldFont = widget.prefs.getBool('useBoldFont') ?? false;
      final newAppFontSize = widget.prefs.getDouble('appFontSize') ?? 18.0;
      final newEnableScroll = widget.prefs.getBool('enableScroll') ?? true;
      final newShowIcons = widget.prefs.getBool('showIcons') ?? false;
      final newShowAppTitles = widget.prefs.getBool('showAppTitles') ?? true;
      final newSelectedApps = widget.prefs.getStringList('selectedApps') ?? [];
      final newAppIconSize = widget.prefs.getDouble('appIconSize') ?? 18.0;
      final newUseBlackAndWhiteIcons =
          widget.prefs.getBool('useBlackAndWhiteIcons') ?? false;

      final hasChanges = _numApps != newNumApps ||
          _numColumns != newNumColumns ||
          _showDateTime != newShowDateTime ||
          _showSearchButton != newShowSearchButton ||
          _showSettingsButton != newShowSettingsButton ||
          _useBoldFont != newUseBoldFont ||
          _appFontSize != newAppFontSize ||
          _enableScroll != newEnableScroll ||
          _showIcons != newShowIcons ||
          _showAppTitles != newShowAppTitles ||
          !listEquals(_selectedApps, newSelectedApps) ||
          _appIconSize != newAppIconSize ||
          _useBlackAndWhiteIcons != newUseBlackAndWhiteIcons;

      if (hasChanges) {
        setState(() {
          _numApps = newNumApps;
          _numColumns = newNumColumns;
          _showDateTime = newShowDateTime;
          _showSearchButton = newShowSearchButton;
          _showSettingsButton = newShowSettingsButton;
          _useBoldFont = newUseBoldFont;
          _appFontSize = newAppFontSize;
          _enableScroll = newEnableScroll;
          _showIcons = newShowIcons;
          _showAppTitles = newShowAppTitles;
          _selectedApps = newSelectedApps;
          _appIconSize = newAppIconSize;
          _useBlackAndWhiteIcons = newUseBlackAndWhiteIcons;
        });

        final showStatusBar = widget.prefs.getBool('showStatusBar') ?? false;
        SystemChrome.setEnabledSystemUIMode(
          SystemUiMode.manual,
          overlays: showStatusBar
              ? [SystemUiOverlay.top, SystemUiOverlay.bottom]
              : [SystemUiOverlay.bottom],
        );

        await _preloadAppInfo();
      }
    });
  }

  void _updateDateTime() {
    if (!mounted || !_showDateTime) return;
    final now = DateTime.now();
    setState(() {
      _currentTime = _timeFormatter.format(now);
      _currentDate = _dateFormatter.format(now);
    });
  }

  Future<void> _updateBattery() async {
    if (!mounted || !_showDateTime) return;
    try {
      final level = await _battery.batteryLevel;
      if (mounted) {
        setState(() => _batteryLevel = level);
      }
    } catch (e) {
      // Error silently
    }
  }

  Future<void> _openSettings() async {
    if (_isNavigating) return;
    _isNavigating = true;
    try {
      if (!mounted) return;
      await Navigator.push(
        context,
        PageRouteBuilder(
          pageBuilder: (context, animation, secondaryAnimation) =>
              SettingsScreen(prefs: widget.prefs),
          transitionDuration: Duration.zero,
          reverseTransitionDuration: Duration.zero,
        ),
      );
      if (mounted) _loadSettings();
    } catch (e) {
      // Error silently
    } finally {
      _isNavigating = false;
    }
  }

  Future<void> _openSearch() async {
    if (_isNavigating) return;
    _isNavigating = true;
    try {
      if (!mounted) return;
      final autoFocus = widget.prefs.getBool('autoFocusSearch') ?? true;
      await Navigator.push(
        context,
        PageRouteBuilder(
          pageBuilder: (context, animation, secondaryAnimation) => SearchScreen(
            prefs: widget.prefs,
            autoFocus: autoFocus,
          ),
          transitionDuration: Duration.zero,
          reverseTransitionDuration: Duration.zero,
        ),
      );
    } catch (e) {
      // Error silently
    } finally {
      _isNavigating = false;
    }
  }

  Widget _buildAppGrid() {
    return FutureBuilder<List<AppInfo>>(
      future: Future.wait(
          _selectedApps.map((packageName) => _getAppInfo(packageName))),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting &&
            _appInfoCache.isEmpty) {
          return const Center(
              child: Text('Loading...', style: TextStyle(fontSize: 18)));
        }

        if (snapshot.hasError) {
          debugPrint('Error loading apps: ${snapshot.error}');
          return Center(
            child: Text(
              'Error loading applications',
              style: TextStyle(
                fontSize: _appFontSize,
                fontWeight: _useBoldFont ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          );
        }

        final apps = snapshot.data ?? [];
        if (apps.isEmpty) {
          return Center(
            child: Text(
              'No applications selected',
              style: TextStyle(
                fontSize: _appFontSize,
                fontWeight: _useBoldFont ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          );
        }

        return _numColumns == 1 ? _buildListView(apps) : _buildGridView(apps);
      },
    );
  }

  Widget _buildListView(List<AppInfo> apps) {
    return ScrollConfiguration(
      behavior: NoGlowScrollBehavior(),
      child: ListView.builder(
        padding: EdgeInsets.zero,
        physics: _enableScroll
            ? const AlwaysScrollableScrollPhysics()
            : const NeverScrollableScrollPhysics(),
        itemCount: apps.length,
        itemBuilder: (context, index) => _buildAppItem(apps[index]),
      ),
    );
  }

  Widget _buildGridView(List<AppInfo> apps) {
    return ScrollConfiguration(
      behavior: NoGlowScrollBehavior(),
      child: GridView.builder(
        padding: EdgeInsets.zero,
        physics: _enableScroll
            ? const AlwaysScrollableScrollPhysics()
            : const NeverScrollableScrollPhysics(),
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: _numColumns,
          mainAxisSpacing: 0,
          crossAxisSpacing: 0,
          childAspectRatio: _numColumns == 4
              ? 0.55
              : _numColumns == 3
                  ? 0.7
                  : 0.95,
        ),
        itemCount: apps.length,
        itemBuilder: (context, index) => _buildAppItem(apps[index]),
      ),
    );
  }

  Widget _buildAppItem(AppInfo app) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => InstalledApps.startApp(app.packageName),
        splashColor: Colors.transparent,
        highlightColor: Colors.transparent,
        hoverColor: Colors.transparent,
        child: Container(
          constraints: BoxConstraints(
            minHeight:
                _appIconSize + (_numColumns == 1 ? 16.0 : _appFontSize + 24.0),
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: 16.0,
            vertical: 8.0,
          ),
          child: _numColumns == 1
              ? _buildListAppItem(app)
              : _buildGridAppItem(app),
        ),
      ),
    );
  }

  Widget _buildListAppItem(AppInfo app) {
    return Row(
      children: [
        if (_showIcons && app.icon != null)
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: _useBlackAndWhiteIcons
                ? FutureBuilder<Uint8List?>(
                    future: app.getProcessedIcon(),
                    builder: (context, snapshot) {
                      if (snapshot.hasData && snapshot.data != null) {
                        return Image.memory(
                          snapshot.data!,
                          width: _appIconSize,
                          height: _appIconSize,
                          fit: BoxFit.contain,
                        );
                      }
                      return Image.memory(
                        app.icon!,
                        width: _appIconSize,
                        height: _appIconSize,
                        fit: BoxFit.contain,
                      );
                    },
                  )
                : Image.memory(
                    app.icon!,
                    width: _appIconSize,
                    height: _appIconSize,
                    fit: BoxFit.contain,
                  ),
          ),
        if (_showAppTitles)
          Expanded(
            child: Text(
              app.name,
              style: TextStyle(
                fontSize: _appFontSize,
                fontWeight: _useBoldFont ? FontWeight.bold : FontWeight.normal,
              ),
              overflow: TextOverflow.ellipsis,
              maxLines: 1,
            ),
          ),
      ],
    );
  }

  Widget _buildGridAppItem(AppInfo app) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        if (_showIcons && app.icon != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: _useBlackAndWhiteIcons
                ? FutureBuilder<Uint8List?>(
                    future: app.getProcessedIcon(),
                    builder: (context, snapshot) {
                      if (snapshot.hasData && snapshot.data != null) {
                        return Image.memory(
                          snapshot.data!,
                          width: _appIconSize,
                          height: _appIconSize,
                          fit: BoxFit.contain,
                        );
                      }
                      return Image.memory(
                        app.icon!,
                        width: _appIconSize,
                        height: _appIconSize,
                        fit: BoxFit.contain,
                      );
                    },
                  )
                : Image.memory(
                    app.icon!,
                    width: _appIconSize,
                    height: _appIconSize,
                    fit: BoxFit.contain,
                  ),
          ),
        if (_showAppTitles)
          Flexible(
            child: Text(
              app.name,
              style: TextStyle(
                fontSize: _appFontSize,
                fontWeight: _useBoldFont ? FontWeight.bold : FontWeight.normal,
              ),
              overflow: TextOverflow.ellipsis,
              maxLines: 1,
              softWrap: false,
              textAlign: TextAlign.center,
            ),
          ),
      ],
    );
  }

  IconData _getBatteryIcon(int level) {
    if (level >= 90) return Icons.battery_full;
    if (level >= 70) return Icons.battery_6_bar;
    if (level >= 50) return Icons.battery_5_bar;
    if (level >= 30) return Icons.battery_4_bar;
    if (level >= 20) return Icons.battery_3_bar;
    if (level >= 10) return Icons.battery_2_bar;
    return Icons.battery_alert;
  }

  @override
  Widget build(BuildContext context) {
    final gestureHandler =
        GestureHandler(prefs: widget.prefs, context: context);

    return GestureDetector(
      onVerticalDragUpdate: gestureHandler.handleVerticalDrag,
      onHorizontalDragUpdate: gestureHandler.handleHorizontalDrag,
      onLongPress: widget.prefs.getBool('enableLongPressGesture') ?? true
          ? _openSettings
          : null,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: Stack(
          children: [
            Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      if (_showDateTime)
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _currentTime,
                              style: TextStyle(
                                fontSize: 64,
                                fontWeight: _useBoldFont
                                    ? FontWeight.bold
                                    : FontWeight.normal,
                              ),
                            ),
                            Row(
                              children: [
                                Text(
                                  _currentDate,
                                  style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: _useBoldFont
                                        ? FontWeight.bold
                                        : FontWeight.normal,
                                  ),
                                ),
                                const Text(
                                  ' | ',
                                  style: TextStyle(fontSize: 18),
                                ),
                                Icon(_getBatteryIcon(_batteryLevel), size: 18),
                                const SizedBox(width: 4),
                                Text(
                                  '$_batteryLevel%',
                                  style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: _useBoldFont
                                        ? FontWeight.bold
                                        : FontWeight.normal,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        )
                      else
                        const SizedBox(),
                      Row(
                        children: [
                          if (_showSettingsButton)
                            IconButton(
                              icon: const Icon(Icons.settings, size: 28),
                              onPressed: _openSettings,
                              padding: EdgeInsets.zero,
                            ),
                          if (_showSearchButton)
                            IconButton(
                              icon: const Icon(Icons.search, size: 28),
                              onPressed: _openSearch,
                              padding: EdgeInsets.zero,
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Stack(
                    children: [
                      if (_selectedApps.isEmpty)
                        const Center(
                          child: Text(
                            'Press the settings button to start',
                            style: TextStyle(fontSize: 18),
                          ),
                        )
                      else
                        _buildAppGrid(),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class NoGlowScrollBehavior extends ScrollBehavior {
  @override
  Widget buildOverscrollIndicator(
      BuildContext context, Widget child, ScrollableDetails details) {
    return child;
  }
}
