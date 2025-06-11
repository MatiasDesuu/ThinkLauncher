import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'app_selection_screen.dart';
import 'gesture_settings_screen.dart';
import 'reorder_apps_screen.dart';
import 'homescreen_settings.dart';
import 'rename_apps_screen.dart';

// Constantes para el tema y estilo
const _kFontSize = 18.0;
const _kSubtitleFontSize = 14.0;

// Class to remove any overscroll effect (glow, stretch, bounce)
class NoGlowScrollBehavior extends ScrollBehavior {
  @override
  Widget buildOverscrollIndicator(
      BuildContext context, Widget child, ScrollableDetails details) {
    return child;
  }
}

class SettingsScreen extends StatefulWidget {
  final SharedPreferences prefs;

  const SettingsScreen({super.key, required this.prefs});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  int numApps = 5;
  int numColumns = 1;
  bool showDateTime = true;
  bool showSearchButton = true;
  bool showSettingsButton = true;
  double appFontSize = 18.0;
  double appIconSize = 27.0;
  bool enableScroll = true;
  bool usePagination = true;
  List<String> selectedApps = [];
  bool isLoading = false;
  String? errorMessage;
  bool showSavedMessage = false;
  bool useListStyleInColumns = false;
  bool _hasChanges = false;

  // Variables para paginación
  int _currentPage = 0;
  int _itemsPerPage = 10; // Valor inicial, se actualizará dinámicamente

  @override
  void initState() {
    super.initState();
    _loadSettings();
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
    final bottomInset = mediaQuery.viewInsets.bottom;
    final bottomNavigationBarHeight = mediaQuery.viewPadding.bottom;

    // Calcular el espacio disponible para la lista, considerando todos los elementos de la UI
    final availableHeight = screenHeight -
        appBarHeight -
        paginationHeight -
        topPadding -
        bottomPadding -
        bottomInset -
        bottomNavigationBarHeight -
        8.0; // Margen de seguridad adicional

    // Calcular el número de elementos que caben en la página actual
    double currentHeight = 0;
    int itemsThatFit = 0;

    // Definir las alturas de los diferentes tipos de elementos
    const double titleHeight =
        48.0; // 16.0 padding top + 16.0 padding bottom + 16.0 texto
    const double sliderHeight =
        72.0; // 16.0 padding top + 16.0 padding bottom + 40.0 slider
    const double listTileHeight =
        56.0; // Altura estándar de ListTile y SwitchListTile

    for (final item in _settingsItems) {
      double itemHeight;
      if (item is Padding) {
        if (item.padding == const EdgeInsets.all(16.0)) {
          itemHeight = titleHeight;
        } else {
          itemHeight = sliderHeight;
        }
      } else if (item is SwitchListTile || item is ListTile) {
        itemHeight = listTileHeight;
      } else {
        itemHeight = listTileHeight;
      }

      // Verificar si el elemento actual cabe completamente en la página
      if (currentHeight + itemHeight <= availableHeight) {
        currentHeight += itemHeight;
        itemsThatFit++;
      } else {
        // Si el elemento no cabe completamente, no lo incluimos en la página actual
        break;
      }
    }

    // Asegurarnos de que al menos haya 3 items por página y máximo 20
    _itemsPerPage = itemsThatFit.clamp(3, 20);

    // Si estamos en modo paginación, asegurarnos de que la página actual sea válida
    if (usePagination) {
      final maxPage = (_settingsItems.length / _itemsPerPage).ceil() - 1;
      if (_currentPage > maxPage) {
        _currentPage = maxPage.clamp(0, maxPage);
      }
    }
  }

  void _loadSettings() {
    setState(() {
      numApps = widget.prefs.getInt('numApps') ?? 5;
      numColumns = widget.prefs.getInt('numColumns') ?? 1;
      showDateTime = widget.prefs.getBool('showDateTime') ?? true;
      showSearchButton = widget.prefs.getBool('showSearchButton') ?? true;
      showSettingsButton = widget.prefs.getBool('showSettingsButton') ?? true;
      appFontSize = widget.prefs.getDouble('appFontSize') ?? 18.0;
      appIconSize = widget.prefs.getDouble('appIconSize') ?? 27.0;
      enableScroll = widget.prefs.getBool('enableScroll') ?? true;
      usePagination = widget.prefs.getBool('usePagination') ?? true;
      selectedApps = widget.prefs.getStringList('selectedApps') ?? [];
      useListStyleInColumns =
          widget.prefs.getBool('useListStyleInColumns') ?? false;
      _hasChanges = false;

      // Asegurarnos de que la lista de apps seleccionadas no exceda el número máximo
      if (selectedApps.length > numApps) {
        selectedApps = selectedApps.sublist(0, numApps);
        widget.prefs.setStringList('selectedApps', selectedApps);
      }

      // Check if both are disabled and enable settings button if necessary
      final enableLongPressGesture =
          widget.prefs.getBool('enableLongPressGesture') ?? true;
      if (!showSettingsButton && !enableLongPressGesture) {
        showSettingsButton = true;
        widget.prefs.setBool('showSettingsButton', true);
      }

      // Desactivar gestos verticales si el list scrolling está activado
      if (enableScroll) {
        final enableSwipeUp = widget.prefs.getBool('enableSwipeUp') ?? true;
        final enableSwipeDown = widget.prefs.getBool('enableSwipeDown') ?? true;

        if (enableSwipeUp) {
          widget.prefs.setBool('enableSwipeUp', false);
        }
        if (enableSwipeDown) {
          widget.prefs.setBool('enableSwipeDown', false);
        }
      }
    });
  }

  List<Widget> get _settingsItems {
    final items = <Widget>[];

    // 1. Number of apps
    items.add(
      const Padding(
        padding: EdgeInsets.all(16.0),
        child: Text(
          'Number of apps',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
    items.add(
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: Row(
          children: [
            IconButton(
              onPressed: numApps > 1
                  ? () {
                      setState(() {
                        numApps--;
                        if (selectedApps.length > numApps) {
                          selectedApps = selectedApps.sublist(0, numApps);
                          widget.prefs
                              .setStringList('selectedApps', selectedApps);
                        }
                      });
                      _onSettingChanged();
                    }
                  : null,
              icon: const Icon(Icons.remove_rounded),
            ),
            Expanded(
              child: SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  overlayShape: SliderComponentShape.noOverlay,
                  valueIndicatorColor: Colors.transparent,
                  valueIndicatorTextStyle: const TextStyle(color: Colors.black),
                  thumbShape: const RoundSliderThumbShape(
                    enabledThumbRadius: 12,
                    elevation: 0,
                    pressedElevation: 0,
                  ),
                  trackHeight: 2,
                  activeTrackColor: Colors.black,
                  inactiveTrackColor: Colors.grey,
                  thumbColor: Colors.black,
                  overlayColor: Colors.transparent,
                ),
                child: Slider(
                  value: numApps.toDouble(),
                  min: 1,
                  max: 50,
                  divisions: 49,
                  label: numApps.toString(),
                  onChanged: (value) {
                    setState(() {
                      numApps = value.toInt();
                      if (selectedApps.length > numApps) {
                        selectedApps = selectedApps.sublist(0, numApps);
                        widget.prefs
                            .setStringList('selectedApps', selectedApps);
                      }
                    });
                    _onSettingChanged();
                  },
                ),
              ),
            ),
            IconButton(
              onPressed: numApps < 50
                  ? () {
                      setState(() {
                        numApps++;
                      });
                      _onSettingChanged();
                    }
                  : null,
              icon: const Icon(Icons.add_rounded),
            ),
          ],
        ),
      ),
    );

    // 2. Number of columns
    items.add(
      const Padding(
        padding: EdgeInsets.all(16.0),
        child: Text(
          'Number of columns',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
    items.add(
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: Row(
          children: [
            IconButton(
              onPressed: numColumns > 1
                  ? () {
                      setState(() {
                        numColumns--;
                      });
                    }
                  : null,
              icon: const Icon(Icons.remove_rounded),
            ),
            Expanded(
              child: SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  overlayShape: SliderComponentShape.noOverlay,
                  valueIndicatorColor: Colors.transparent,
                  valueIndicatorTextStyle: const TextStyle(color: Colors.black),
                  thumbShape: const RoundSliderThumbShape(
                    enabledThumbRadius: 12,
                    elevation: 0,
                    pressedElevation: 0,
                  ),
                  trackHeight: 2,
                  activeTrackColor: Colors.black,
                  inactiveTrackColor: Colors.grey,
                  thumbColor: Colors.black,
                  overlayColor: Colors.transparent,
                ),
                child: Slider(
                  value: numColumns.toDouble(),
                  min: 1,
                  max: 4,
                  divisions: 3,
                  label: numColumns.toString(),
                  onChanged: (value) {
                    setState(() {
                      numColumns = value.toInt();
                    });
                    _onSettingChanged();
                  },
                ),
              ),
            ),
            IconButton(
              onPressed: numColumns < 4
                  ? () {
                      setState(() {
                        numColumns++;
                      });
                    }
                  : null,
              icon: const Icon(Icons.add_rounded),
            ),
          ],
        ),
      ),
    );

    // 3. Show date, time and battery
    items.add(
      ListTile(
        title: const Text(
          'Home Screen Settings',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Configure home screen options',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        trailing: const Icon(Icons.chevron_right_rounded),
        onTap: () async {
          await Navigator.push(
            context,
            PageRouteBuilder(
              pageBuilder: (context, animation, secondaryAnimation) =>
                  DateTimeSettingsScreen(prefs: widget.prefs),
              transitionDuration: Duration.zero,
              reverseTransitionDuration: Duration.zero,
            ),
          );
          // Actualizar las variables de estado cuando volvemos
          setState(() {
            showDateTime = widget.prefs.getBool('showDateTime') ?? true;
            showSearchButton = widget.prefs.getBool('showSearchButton') ?? true;
            showSettingsButton =
                widget.prefs.getBool('showSettingsButton') ?? true;
            _onSettingChanged();
          });
        },
      ),
    );
    items.add(
      SwitchListTile(
        title: const Text(
          'Use Pagination',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Show settings and search results in pages instead of a scrollable list',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: usePagination,
        onChanged: (value) {
          setState(() {
            usePagination = value;
          });
          _onSettingChanged();
        },
      ),
    );
    if (!usePagination) {
      items.add(
        SwitchListTile(
          title: const Text(
            'Enable List Scrolling',
            style: TextStyle(
              fontSize: _kFontSize,
              fontWeight: FontWeight.bold,
            ),
          ),
          value: enableScroll,
          onChanged: (widget.prefs.getBool('enableSwipeUp') ?? true) ||
                  (widget.prefs.getBool('enableSwipeDown') ?? true)
              ? null
              : (value) async {
                  setState(() {
                    enableScroll = value;
                  });
                  _onSettingChanged();
                  if (value) {
                    await widget.prefs.setBool('enableSwipeUp', false);
                    await widget.prefs.setBool('enableSwipeDown', false);
                  }
                },
        ),
      );
    }

    items.add(
      const Padding(
        padding: EdgeInsets.all(16.0),
        child: Text(
          'App font size',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
    items.add(
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: Row(
          children: [
            IconButton(
              onPressed: appFontSize > 14
                  ? () {
                      setState(() {
                        appFontSize--;
                      });
                    }
                  : null,
              icon: const Icon(Icons.remove_rounded),
            ),
            Expanded(
              child: SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  overlayShape: SliderComponentShape.noOverlay,
                  valueIndicatorColor: Colors.transparent,
                  valueIndicatorTextStyle: const TextStyle(color: Colors.black),
                  thumbShape: const RoundSliderThumbShape(
                    enabledThumbRadius: 12,
                    elevation: 0,
                    pressedElevation: 0,
                  ),
                  trackHeight: 2,
                  activeTrackColor: Colors.black,
                  inactiveTrackColor: Colors.grey,
                  thumbColor: Colors.black,
                  overlayColor: Colors.transparent,
                ),
                child: Slider(
                  value: appFontSize,
                  min: 14,
                  max: 32,
                  divisions: 18,
                  label: appFontSize.toStringAsFixed(0),
                  onChanged: (value) {
                    setState(() {
                      appFontSize = value;
                    });
                    _onSettingChanged();
                  },
                ),
              ),
            ),
            IconButton(
              onPressed: appFontSize < 32
                  ? () {
                      setState(() {
                        appFontSize++;
                      });
                    }
                  : null,
              icon: const Icon(Icons.add_rounded),
            ),
          ],
        ),
      ),
    );

    // 7. App icon size
    items.add(
      const Padding(
        padding: EdgeInsets.all(16.0),
        child: Text(
          'App icon size',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
    items.add(
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: Row(
          children: [
            IconButton(
              onPressed: appIconSize > 16
                  ? () {
                      setState(() {
                        appIconSize--;
                      });
                    }
                  : null,
              icon: const Icon(Icons.remove_rounded),
            ),
            Expanded(
              child: SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  overlayShape: SliderComponentShape.noOverlay,
                  valueIndicatorColor: Colors.transparent,
                  valueIndicatorTextStyle: const TextStyle(color: Colors.black),
                  thumbShape: const RoundSliderThumbShape(
                    enabledThumbRadius: 12,
                    elevation: 0,
                    pressedElevation: 0,
                  ),
                  trackHeight: 2,
                  activeTrackColor: Colors.black,
                  inactiveTrackColor: Colors.grey,
                  thumbColor: Colors.black,
                  overlayColor: Colors.transparent,
                ),
                child: Slider(
                  value: appIconSize,
                  min: 16,
                  max: 128,
                  divisions: 112,
                  label: appIconSize.toStringAsFixed(0),
                  onChanged: (value) {
                    setState(() {
                      appIconSize = value;
                    });
                    _onSettingChanged();
                  },
                ),
              ),
            ),
            IconButton(
              onPressed: appIconSize < 128
                  ? () {
                      setState(() {
                        appIconSize++;
                      });
                    }
                  : null,
              icon: const Icon(Icons.add_rounded),
            ),
          ],
        ),
      ),
    );

    // 8. App list
    items.add(
      ListTile(
        title: const Text(
          'App List',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: Text('${selectedApps.length} of $numApps apps selected'),
        trailing: const Icon(Icons.chevron_right_rounded),
        onTap: _selectApps,
      ),
    );

    // 9. Reorder apps
    items.add(
      ListTile(
        title: const Text(
          'Reorder Apps',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        trailing: const Icon(Icons.chevron_right_rounded),
        onTap: selectedApps.isEmpty ? null : _reorderApps,
      ),
    );

    // 10. Rename apps
    items.add(
      ListTile(
        title: const Text(
          'Rename Apps',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        trailing: const Icon(Icons.chevron_right_rounded),
        onTap: selectedApps.isEmpty ? null : _renameApps,
      ),
    );

    // 11. Gestures
    items.add(
      ListTile(
        title: const Text(
          'Gestures',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text('Configure application gestures'),
        trailing: const Icon(Icons.chevron_right_rounded),
        onTap: () {
          Navigator.push(
            context,
            PageRouteBuilder(
              pageBuilder: (context, animation, secondaryAnimation) =>
                  GestureSettingsScreen(prefs: widget.prefs),
              transitionDuration: Duration.zero,
              reverseTransitionDuration: Duration.zero,
            ),
          );
        },
      ),
    );

    // 12. Use list style in columns
    items.add(
      SwitchListTile(
        title: const Text(
          'Use list style in columns',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Show apps in list style even when using multiple columns',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: useListStyleInColumns,
        onChanged: (value) {
          setState(() {
            useListStyleInColumns = value;
          });
          _onSettingChanged();
        },
      ),
    );

    return items;
  }

  List<Widget> get _currentPageItems {
    if (!usePagination) return _settingsItems;

    final start = _currentPage * _itemsPerPage;
    final end = (start + _itemsPerPage).clamp(0, _settingsItems.length);
    return _settingsItems.sublist(start, end);
  }

  int get _totalPages => (_settingsItems.length / _itemsPerPage).ceil();

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

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: _onWillPop,
      child: Container(
        color: Colors.white,
        child: Padding(
          padding: const EdgeInsets.only(top: 16.0),
          child: Scaffold(
            backgroundColor: Colors.white,
            appBar: AppBar(
              scrolledUnderElevation: 0,
              title: const Text('Settings'),
              backgroundColor: Colors.white,
              foregroundColor: Colors.black,
              elevation: 0,
              leading: IconButton(
                icon: const Icon(Icons.arrow_back_rounded),
                onPressed: () async {
                  if (await _onWillPop()) {
                    if (mounted) {
                      Navigator.pop(context);
                    }
                  }
                },
              ),
              actions: [
                if (showSavedMessage)
                  const Padding(
                    padding: EdgeInsets.only(right: 16.0),
                    child: Text(
                      'Saved',
                      style: TextStyle(
                        color: Colors.black,
                        fontSize: 16,
                      ),
                    ),
                  ),
                Padding(
                  padding: const EdgeInsets.only(right: 16.0),
                  child: FilledButton.tonal(
                    onPressed: _saveSettings,
                    style: FilledButton.styleFrom(
                      backgroundColor: Colors.black,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text('Save'),
                  ),
                ),
              ],
            ),
            body: Column(
              children: [
                Expanded(
                  child: ListView(
                    physics: usePagination
                        ? const NeverScrollableScrollPhysics()
                        : null,
                    children: _currentPageItems,
                  ),
                ),
                if (usePagination && _totalPages > 1)
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
                            fontSize: _kFontSize,
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
      ),
    );
  }

  Future<bool> _onWillPop() async {
    if (!_hasChanges) return true;

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Unsaved Changes'),
        content: const Text('Do you want to save your changes?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Discard'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Save'),
          ),
        ],
      ),
    );

    if (result == null) return false;
    if (result) {
      await _saveSettings();
    }
    return true;
  }

  Future<void> _saveSettings() async {
    setState(() {
      isLoading = true;
      errorMessage = null;
    });

    try {
      await widget.prefs.setInt('numApps', numApps);
      await widget.prefs.setInt('numColumns', numColumns);
      await widget.prefs.setBool('showDateTime', showDateTime);
      await widget.prefs.setBool('showSearchButton', showSearchButton);
      await widget.prefs.setBool('showSettingsButton', showSettingsButton);
      await widget.prefs.setDouble('appFontSize', appFontSize);
      await widget.prefs.setDouble('appIconSize', appIconSize);
      await widget.prefs.setBool('enableScroll', enableScroll);
      await widget.prefs.setBool('usePagination', usePagination);
      await widget.prefs.setStringList('selectedApps', selectedApps);
      await widget.prefs
          .setBool('useListStyleInColumns', useListStyleInColumns);

      setState(() {
        showSavedMessage = true;
        _hasChanges = false;
      });

      Future.delayed(const Duration(seconds: 2), () {
        if (mounted) {
          setState(() {
            showSavedMessage = false;
          });
        }
      });
    } catch (e) {
      setState(() {
        errorMessage = 'Error saving settings: $e';
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoading = false;
        });
      }
    }
  }

  void _onSettingChanged() {
    if (!_hasChanges) {
      setState(() {
        _hasChanges = true;
      });
    }
  }

  Future<void> _selectApps() async {
    final result = await Navigator.push(
      context,
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) =>
            AppSelectionScreen(
          prefs: widget.prefs,
          selectedApps: selectedApps,
          maxApps: numApps,
        ),
        transitionDuration: Duration.zero,
        reverseTransitionDuration: Duration.zero,
      ),
    );

    if (result != null && result is List<String>) {
      setState(() {
        selectedApps = result;
      });
    }
  }

  Future<void> _reorderApps() async {
    if (selectedApps.isEmpty) return;

    final result = await Navigator.push(
      context,
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) =>
            ReorderAppsScreen(
          prefs: widget.prefs,
          selectedApps: selectedApps,
        ),
        transitionDuration: Duration.zero,
        reverseTransitionDuration: Duration.zero,
      ),
    );

    if (result != null && result is List<String>) {
      setState(() {
        selectedApps = result;
      });
    }
  }

  Future<void> _renameApps() async {
    if (selectedApps.isEmpty) return;

    final result = await Navigator.push(
      context,
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) =>
            RenameAppsScreen(
          prefs: widget.prefs,
          selectedApps: selectedApps,
        ),
        transitionDuration: Duration.zero,
        reverseTransitionDuration: Duration.zero,
      ),
    );

    if (result != null && result is Map<String, String>) {
      _onSettingChanged();
    }
  }
}
