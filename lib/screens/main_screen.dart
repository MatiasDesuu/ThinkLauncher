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
import 'dart:math' as math;
import 'dart:convert';

class MainScreen extends StatefulWidget {
  final SharedPreferences prefs;

  const MainScreen({super.key, required this.prefs});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> with WidgetsBindingObserver {
  // Cache for app information
  static final Map<String, AppInfo> _appInfoCache = {};

  // Gesture handler
  late GestureHandler _gestureHandler;

  // State variables
  late List<String> _selectedApps;
  late int _numColumns;
  late bool _showDateTime;
  late bool _showDate;
  late bool _showTime;
  late bool _showBattery;
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
  late bool _usePagination;
  late bool _useListStyleInColumns;
  late String _singleColumnPosition;
  late String _verticalAlignment;
  late Map<String, String> _customNames;
  int _currentPage = 0;
  int _itemsPerPage = 10; // Valor inicial, se actualizará dinámicamente

  // Constantes para el cálculo de altura
  static const _kItemHeight = 48.0; // Altura fija de cada item de la lista
  static const _kDateTimeHeight =
      120.0; // Altura aproximada del área de fecha/hora
  static const _kPaginationHeight =
      64.0; // Altura aproximada de los controles de paginación

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
    _loadSettings();
    _loadCustomNames();
    _startTimers();
    _setupBatteryListener();
    WidgetsBinding.instance.addObserver(this);
    _gestureHandler = GestureHandler(
      prefs: widget.prefs,
      context: context,
      onNextPage: _nextPage,
      onPreviousPage: _previousPage,
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (mounted) {
      _calculateItemsPerPage();
      // Actualizar el contexto del gesture handler
      _gestureHandler = GestureHandler(
        prefs: widget.prefs,
        context: context,
        onNextPage: _nextPage,
        onPreviousPage: _previousPage,
      );
    }
  }

  void _calculateItemsPerPage() {
    if (!mounted) return;

    // Asegurarnos de que las variables necesarias estén inicializadas
    if (_selectedApps.isEmpty) {
      _itemsPerPage = 10;
      return;
    }

    final mediaQuery = MediaQuery.of(context);
    final screenHeight = mediaQuery.size.height;
    final topPadding = mediaQuery.padding.top;
    final bottomPadding = mediaQuery.padding.bottom;

    // Calcular el espacio disponible para la lista
    double availableHeight = screenHeight - topPadding - bottomPadding;

    // Restar altura de fecha/hora si está visible
    if (_showDateTime) {
      availableHeight -= _kDateTimeHeight;
    }

    // Restar altura de paginación si está habilitada
    if (_usePagination) {
      availableHeight -= _kPaginationHeight;
    }

    // Restar un espacio extra para el padding inferior
    availableHeight -=
        24.0; // 8.0 de padding superior + 16.0 de padding inferior

    // Calcular cuántos items caben en el espacio disponible
    if (_numColumns > 1) {
      if (_useListStyleInColumns) {
        // En modo de múltiples columnas con estilo lista
        final itemHeight = math.max(_appIconSize, _appFontSize * 1.5) + 16.0;
        final itemsThatFit = (availableHeight / itemHeight).floor();
        _itemsPerPage = _usePagination
            ? (itemsThatFit * _numColumns).clamp(5, 100)
            : _selectedApps.length;
      } else {
        // En modo de cuadrícula
        final itemHeight = _appIconSize + _appFontSize + 24.0;
        final rowsThatFit = (availableHeight / itemHeight).floor();
        _itemsPerPage = _usePagination
            ? (rowsThatFit * _numColumns).clamp(5, 100)
            : _selectedApps.length;
      }
    } else {
      // En modo de una columna
      final itemHeight = math.max(_appIconSize, _appFontSize * 1.5) + 16.0;
      _itemsPerPage = _usePagination
          ? (availableHeight / itemHeight).floor()
          : _selectedApps.length;
    }

    // Si estamos en modo paginación, asegurarnos de que la página actual sea válida
    if (_usePagination && _selectedApps.isNotEmpty) {
      final maxPage = (_selectedApps.length / _itemsPerPage).ceil() - 1;
      if (_currentPage > maxPage) {
        _currentPage = maxPage.clamp(0, maxPage);
      }
    } else {
      _currentPage = 0;
    }
  }

  void _loadSettings() {
    _selectedApps = widget.prefs.getStringList('selectedApps') ?? [];
    _numColumns = widget.prefs.getInt('numColumns') ?? 1;
    _showDateTime = widget.prefs.getBool('showDateTime') ?? true;
    _showDate = widget.prefs.getBool('showDate') ?? true;
    _showTime = widget.prefs.getBool('showTime') ?? true;
    _showBattery = widget.prefs.getBool('showBattery') ?? true;
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
    _usePagination = widget.prefs.getBool('usePagination') ?? true;
    _useListStyleInColumns =
        widget.prefs.getBool('useListStyleInColumns') ?? false;
    _verticalAlignment = widget.prefs.getString('verticalAlignment') ?? 'top';
    _singleColumnPosition =
        widget.prefs.getString('singleColumnPosition') ?? 'left';

    _currentTime = _timeFormatter.format(DateTime.now());
    _currentDate = _dateFormatter.format(DateTime.now());
    _batteryLevel = 0;
  }

  Future<void> _loadCustomNames() async {
    final customNamesJson = widget.prefs.getString('customAppNames');
    if (customNamesJson != null) {
      try {
        final Map<String, dynamic> decoded = json.decode(customNamesJson);
        _customNames = Map<String, String>.from(decoded);
      } catch (e) {
        debugPrint('Error loading custom names: $e');
        _customNames = {};
      }
    } else {
      _customNames = {};
    }
  }

  void _startTimers() {
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
    AppInfo.disposeAll();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _updateBattery();
      _updateDateTime();
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onHorizontalDragUpdate: _gestureHandler.handleHorizontalDrag,
      onHorizontalDragEnd: _gestureHandler.handleHorizontalDragEnd,
      onVerticalDragUpdate: _gestureHandler.handleVerticalDrag,
      child: Container(
        color: Colors.white,
        child: Scaffold(
          backgroundColor: Colors.white,
          body: Column(
            children: [
              if (_showDateTime)
                Container(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if (_showTime)
                                Text(
                                  _currentTime,
                                  style: TextStyle(
                                    fontSize: 48,
                                    fontWeight: _useBoldFont
                                        ? FontWeight.bold
                                        : FontWeight.normal,
                                  ),
                                ),
                              if (_showDate)
                                Text(
                                  _currentDate,
                                  style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: _useBoldFont
                                        ? FontWeight.bold
                                        : FontWeight.normal,
                                  ),
                                ),
                            ],
                          ),
                          Row(
                            children: [
                              if (_showBattery) ...[
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
                            ],
                          ),
                        ],
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          if (_showSettingsButton)
                            IconButton(
                              icon:
                                  const Icon(Icons.settings_rounded, size: 28),
                              onPressed: _openSettings,
                              padding: EdgeInsets.zero,
                            ),
                          if (_showSearchButton)
                            IconButton(
                              icon: const Icon(Icons.search_rounded, size: 28),
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
              if (_usePagination && _selectedApps.isNotEmpty)
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

  Widget _buildAppGrid() {
    if (_selectedApps
        .every((packageName) => _appInfoCache.containsKey(packageName))) {
      final apps = _selectedApps
          .map((packageName) => _appInfoCache[packageName]!)
          .toList();

      // Widget base para el contenido
      Widget content;
      if (_numColumns == 1) {
        content = _buildListView(apps);
      } else if (_useListStyleInColumns) {
        content = _buildMultiColumnListView(apps);
      } else {
        content = _buildGridView(apps);
      }

      // Aplicar alineación horizontal
      Widget horizontallyAligned = Align(
        alignment: _getHorizontalAlignment(_singleColumnPosition),
        child: SizedBox(
          width: _numColumns == 1
              ? MediaQuery.of(context).size.width * 0.8
              : MediaQuery.of(context).size.width * 0.95,
          child: content,
        ),
      );

      // Aplicar alineación vertical
      Widget verticallyAligned;
      switch (_verticalAlignment) {
        case 'bottom':
          verticallyAligned = Column(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              const Spacer(),
              Expanded(child: horizontallyAligned),
            ],
          );
          break;
        case 'center':
          verticallyAligned = Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [Expanded(child: horizontallyAligned)],
          );
          break;
        case 'top':
        default:
          verticallyAligned = Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: [horizontallyAligned],
          );
      }

      return verticallyAligned;
    }

    // Si no tenemos todos los iconos, los cargamos
    return StreamBuilder<List<AppInfo>>(
      stream: Stream.fromFuture(_preloadAppInfo()).map((_) => _selectedApps
          .map((packageName) => _appInfoCache[packageName]!)
          .toList()),
      initialData: _selectedApps
          .where((packageName) => _appInfoCache.containsKey(packageName))
          .map((packageName) => _appInfoCache[packageName]!)
          .toList(),
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

        return _buildAppGrid();
      },
    );
  }

  Future<void> _preloadAppInfo() async {
    if (_selectedApps.isEmpty) return;

    // Primero, limpiar el caché de apps que ya no están seleccionadas
    _appInfoCache
        .removeWhere((packageName, _) => !_selectedApps.contains(packageName));

    // Luego, cargar las apps que faltan en el caché
    final futures = _selectedApps
        .where((packageName) => !_appInfoCache.containsKey(packageName))
        .map((packageName) => _getAppInfo(packageName));

    if (futures.isNotEmpty) {
      await Future.wait(futures);
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
      // Precargar el icono procesado
      await appInfo.getProcessedIcon();
      return appInfo;
    } catch (e) {
      debugPrint('Error getting app info for $packageName: $e');
      // Si hay un error, crear un AppInfo básico y guardarlo en el caché
      final appInfo = AppInfo(
        name: packageName,
        packageName: packageName,
        versionName: '',
        versionCode: 0,
        builtWith: BuiltWith.unknown,
        installedTimestamp: 0,
      );
      _appInfoCache[packageName] = appInfo;
      return appInfo;
    }
  }

  void _updateDateTime() {
    if (!mounted || !_showDateTime) return;
    final now = DateTime.now();
    setState(() {
      if (_showTime) {
        _currentTime = _timeFormatter.format(now);
      }
      if (_showDate) {
        _currentDate = _dateFormatter.format(now);
      }
    });
  }

  Future<void> _updateBattery() async {
    if (!mounted || !_showDateTime || !_showBattery) return;
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
      if (mounted) {
        // Recargar toda la pantalla al volver
        setState(() {
          _loadSettings();
          _loadCustomNames();
          _calculateItemsPerPage();
        });
      }
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

  List<AppInfo> get _currentPageItems {
    if (!_usePagination) {
      return _selectedApps
          .where((packageName) => _appInfoCache.containsKey(packageName))
          .map((packageName) => _appInfoCache[packageName]!)
          .toList();
    }

    final apps = _selectedApps
        .where((packageName) => _appInfoCache.containsKey(packageName))
        .map((packageName) => _appInfoCache[packageName]!)
        .toList();

    final startIndex = _currentPage * _itemsPerPage;
    final endIndex = (startIndex + _itemsPerPage).clamp(0, apps.length);
    return apps.sublist(startIndex, endIndex);
  }

  int get _totalPages {
    if (!_usePagination || _selectedApps.isEmpty) return 1;
    return (_selectedApps.length / _itemsPerPage).ceil();
  }

  void _nextPage() {
    if (_currentPage < _totalPages - 1) {
      setState(() => _currentPage++);
    }
  }

  void _previousPage() {
    if (_currentPage > 0) {
      setState(() => _currentPage--);
    }
  }

  Widget _buildListView(List<AppInfo> apps) {
    final bool isPaginated = _usePagination;
    final bool needsScroll =
        (_usePagination ? _currentPageItems.length : apps.length) > 6;

    // Si no necesitamos scroll y estamos en modo bottom, usar Column
    if (!needsScroll && _verticalAlignment == 'bottom') {
      final items = List<Widget>.generate(
        _usePagination ? _currentPageItems.length : apps.length,
        (index) => _buildAppItem(
            _usePagination ? _currentPageItems[index] : apps[index]),
      );
      return Column(
        mainAxisAlignment: MainAxisAlignment.end,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Spacer(), // Añadir un Spacer para empujar los elementos hacia abajo
          ...items,
        ],
      );
    }

    // Para todos los demás casos, usar ListView con scroll
    return ScrollConfiguration(
      behavior: NoGlowScrollBehavior(),
      child: ListView.builder(
        padding: const EdgeInsets.only(bottom: 16.0),
        physics: _enableScroll && !isPaginated
            ? const AlwaysScrollableScrollPhysics()
            : const NeverScrollableScrollPhysics(),
        itemCount: _usePagination ? _currentPageItems.length : apps.length,
        itemBuilder: (context, index) => _buildAppItem(
            _usePagination ? _currentPageItems[index] : apps[index]),
        shrinkWrap: true,
        primary: true,
      ),
    );
  }

  Widget _buildGridView(List<AppInfo> apps) {
    return ScrollConfiguration(
      behavior: NoGlowScrollBehavior(),
      child: GridView.builder(
        padding: const EdgeInsets.only(bottom: 16.0),
        physics: _enableScroll && !_usePagination
            ? const AlwaysScrollableScrollPhysics()
            : const NeverScrollableScrollPhysics(),
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: _numColumns,
          mainAxisSpacing: 8.0,
          crossAxisSpacing: 8.0,
          childAspectRatio: _numColumns == 4
              ? 0.55
              : _numColumns == 3
                  ? 0.7
                  : 0.95,
        ),
        itemCount: _usePagination ? _currentPageItems.length : apps.length,
        itemBuilder: (context, index) => _buildAppItem(
            _usePagination ? _currentPageItems[index] : apps[index]),
        shrinkWrap: true,
      ),
    );
  }

  Widget _buildMultiColumnListView(List<AppInfo> apps) {
    final appsToShow = _usePagination ? _currentPageItems : apps;
    final columns = List.generate(_numColumns, (columnIndex) {
      final columnApps = <AppInfo>[];
      for (var i = 0; i < appsToShow.length; i++) {
        if (i % _numColumns == columnIndex) {
          columnApps.add(appsToShow[i]);
        }
      }
      return columnApps;
    });

    // Crear un ScrollController compartido para sincronizar el scroll
    final scrollController = ScrollController();

    return ScrollConfiguration(
      behavior: NoGlowScrollBehavior(),
      child: SingleChildScrollView(
        controller: scrollController,
        physics: _enableScroll && !_usePagination
            ? const AlwaysScrollableScrollPhysics()
            : const NeverScrollableScrollPhysics(),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: columns.map((columnApps) {
            return Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.only(bottom: 16.0),
                physics: const NeverScrollableScrollPhysics(),
                itemCount: columnApps.length,
                itemBuilder: (context, index) =>
                    _buildAppItem(columnApps[index]),
                shrinkWrap: true,
                primary: false,
              ),
            );
          }).toList(),
        ),
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
          height: _numColumns == 1 || _useListStyleInColumns
              ? math.max(_appIconSize, _appFontSize * 1.5) +
                  16.0 // Altura máxima entre icono y texto + padding
              : _kItemHeight,
          padding: const EdgeInsets.symmetric(
            horizontal: 16.0,
            vertical: 8.0,
          ),
          child: _numColumns == 1 || _useListStyleInColumns
              ? _buildListAppItem(app)
              : _buildGridAppItem(app),
        ),
      ),
    );
  }

  Widget _buildListAppItem(AppInfo app) {
    final horizontalAlignment = _getHorizontalAlignment(_singleColumnPosition);
    _getVerticalAlignment(_verticalAlignment);
    final isLeft = horizontalAlignment == Alignment.centerLeft;
    final isRight = horizontalAlignment == Alignment.centerRight;
    final isCenter = horizontalAlignment == Alignment.center;

    Widget buildIcon() {
      if (!_showIcons || app.icon == null) return const SizedBox.shrink();
      return Padding(
        padding: EdgeInsets.only(
          right: isCenter ? 8.0 : 16.0,
          left: isRight ? 16.0 : 0,
        ),
        child: SizedBox(
          width: _appIconSize,
          height: _appIconSize,
          child: _useBlackAndWhiteIcons
              ? StreamBuilder<Uint8List?>(
                  stream: app.getProcessedIconStream(),
                  initialData: app.icon,
                  builder: (context, snapshot) {
                    return Image.memory(
                      snapshot.data ?? app.icon!,
                      fit: BoxFit.contain,
                    );
                  },
                )
              : Image.memory(
                  app.icon!,
                  fit: BoxFit.contain,
                ),
        ),
      );
    }

    Widget buildText() {
      if (!_showAppTitles) return const SizedBox.shrink();
      return Text(
        _customNames[app.packageName] ?? app.name,
        style: TextStyle(
          fontSize: _appFontSize,
          fontWeight: _useBoldFont ? FontWeight.bold : FontWeight.normal,
          height: 1.2,
        ),
        overflow: TextOverflow.ellipsis,
        maxLines: 1,
        textAlign: isLeft
            ? TextAlign.left
            : isRight
                ? TextAlign.right
                : TextAlign.center,
      );
    }

    if (isCenter) {
      return Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          buildIcon(),
          if (_showAppTitles) buildText(),
        ],
      );
    }

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisAlignment:
          isLeft ? MainAxisAlignment.start : MainAxisAlignment.end,
      children: [
        if (!isRight) buildIcon(),
        if (_showAppTitles)
          Expanded(
            child: buildText(),
          ),
        if (isRight) buildIcon(),
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
            child: SizedBox(
              width: _appIconSize,
              height: _appIconSize,
              child: _useBlackAndWhiteIcons
                  ? StreamBuilder<Uint8List?>(
                      stream: app.getProcessedIconStream(),
                      initialData: app.icon,
                      builder: (context, snapshot) {
                        return Image.memory(
                          snapshot.data ?? app.icon!,
                          fit: BoxFit.contain,
                        );
                      },
                    )
                  : Image.memory(
                      app.icon!,
                      fit: BoxFit.contain,
                    ),
            ),
          ),
        if (_showAppTitles)
          Flexible(
            child: Text(
              _customNames[app.packageName] ?? app.name,
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
    if (level >= 90) return Icons.battery_full_rounded;
    if (level >= 70) return Icons.battery_6_bar_rounded;
    if (level >= 50) return Icons.battery_5_bar_rounded;
    if (level >= 30) return Icons.battery_4_bar_rounded;
    if (level >= 20) return Icons.battery_2_bar_rounded;
    if (level >= 10) return Icons.battery_2_bar_rounded;
    return Icons.battery_alert;
  }

  Alignment _getHorizontalAlignment(String position) {
    switch (position) {
      case 'left':
        return Alignment.centerLeft;
      case 'right':
        return Alignment.centerRight;
      case 'center':
      default:
        return Alignment.center;
    }
  }

  Alignment _getVerticalAlignment(String position) {
    switch (position) {
      case 'bottom':
        return Alignment.bottomCenter;
      case 'top':
      default:
        return Alignment.topCenter;
    }
  }
}

class NoGlowScrollBehavior extends ScrollBehavior {
  @override
  Widget buildOverscrollIndicator(
      BuildContext context, Widget child, ScrollableDetails details) {
    return child;
  }
}
