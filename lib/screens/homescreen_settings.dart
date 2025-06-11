import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Theme and style constants
const _kFontSize = 18.0;
const _kSubtitleFontSize = 12.0;

class DateTimeSettingsScreen extends StatefulWidget {
  final SharedPreferences prefs;

  const DateTimeSettingsScreen({super.key, required this.prefs});

  @override
  State<DateTimeSettingsScreen> createState() => _DateTimeSettingsScreenState();
}

class _DateTimeSettingsScreenState extends State<DateTimeSettingsScreen> {
  bool _showDate = true;
  bool _showTime = true;
  bool _showBattery = true;
  bool _showSearchButton = true;
  bool _showSettingsButton = true;
  bool _useBoldFont = false;
  bool _showIcons = false;
  bool _showAppTitles = true;
  bool _showStatusBar = false;
  bool _useBlackAndWhiteIcons = false;
  String _singleColumnPosition = 'left';
  late int _numColumns;
  bool _usePagination = true;
  int _currentPage = 0;
  int _itemsPerPage = 10;

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
    const double listTileHeight =
        56.0; // Altura estándar de ListTile y SwitchListTile

    for (final item in _getSettingsItems()) {
      double itemHeight = listTileHeight;

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
    if (_usePagination) {
      final maxPage = (_getSettingsItems().length / _itemsPerPage).ceil() - 1;
      if (_currentPage > maxPage) {
        _currentPage = maxPage.clamp(0, maxPage);
      }
    }
  }

  void _loadSettings() {
    setState(() {
      _showDate = widget.prefs.getBool('showDate') ?? true;
      _showTime = widget.prefs.getBool('showTime') ?? true;
      _showBattery = widget.prefs.getBool('showBattery') ?? true;
      _showSearchButton = widget.prefs.getBool('showSearchButton') ?? true;
      _showSettingsButton = widget.prefs.getBool('showSettingsButton') ?? true;
      _useBoldFont = widget.prefs.getBool('useBoldFont') ?? false;
      _showIcons = widget.prefs.getBool('showIcons') ?? false;
      _showAppTitles = widget.prefs.getBool('showAppTitles') ?? true;
      _showStatusBar = widget.prefs.getBool('showStatusBar') ?? false;
      _useBlackAndWhiteIcons =
          widget.prefs.getBool('useBlackAndWhiteIcons') ?? false;
      _numColumns = widget.prefs.getInt('numColumns') ?? 1;
      _singleColumnPosition =
          widget.prefs.getString('singleColumnPosition') ?? 'left';
      _usePagination = widget.prefs.getBool('usePagination') ?? true;
    });
  }

  Future<void> _saveSettings() async {
    await widget.prefs.setBool('showDate', _showDate);
    await widget.prefs.setBool('showTime', _showTime);
    await widget.prefs.setBool('showBattery', _showBattery);
    await widget.prefs
        .setBool('showDateTime', _showDate || _showTime || _showBattery);
    await widget.prefs.setBool('showSearchButton', _showSearchButton);
    await widget.prefs.setBool('showSettingsButton', _showSettingsButton);
    await widget.prefs.setBool('useBoldFont', _useBoldFont);
    await widget.prefs.setBool('showIcons', _showIcons);
    await widget.prefs.setBool('showAppTitles', _showAppTitles);
    await widget.prefs.setBool('showStatusBar', _showStatusBar);
    await widget.prefs.setBool('useBlackAndWhiteIcons', _useBlackAndWhiteIcons);
    await widget.prefs.setInt('numColumns', _numColumns);
    await widget.prefs.setString('singleColumnPosition', _singleColumnPosition);
    await widget.prefs.setBool('usePagination', _usePagination);
  }

  List<Widget> _getSettingsItems() {
    return [
      SwitchListTile(
        title: const Text(
          'Show Status Bar',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display the system status bar',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showStatusBar,
        onChanged: (value) {
          setState(() {
            _showStatusBar = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Show Date',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display the current date',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showDate,
        onChanged: (value) {
          setState(() {
            _showDate = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Show Time',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display the current time',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showTime,
        onChanged: (value) {
          setState(() {
            _showTime = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Show Battery',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display the battery level',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showBattery,
        onChanged: (value) {
          setState(() {
            _showBattery = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Show Search Button',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display the search button',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showSearchButton,
        onChanged: (value) {
          setState(() {
            _showSearchButton = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Show Settings Button',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display the settings button',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showSettingsButton,
        onChanged: !(widget.prefs.getBool('enableLongPressGesture') ?? true)
            ? null
            : (value) {
                setState(() {
                  _showSettingsButton = value;
                });
                _saveSettings();
              },
      ),
      SwitchListTile(
        title: const Text(
          'Show Icons',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display app icons',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showIcons,
        onChanged: (value) {
          setState(() {
            _showIcons = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Show App Titles',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Display app names',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _showAppTitles,
        onChanged: (value) {
          setState(() {
            _showAppTitles = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Black & White Icons',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Experimental feature: may look pixelated or lose details in some icons.',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _useBlackAndWhiteIcons,
        onChanged: (value) {
          setState(() {
            _useBlackAndWhiteIcons = value;
          });
          _saveSettings();
        },
      ),
      SwitchListTile(
        title: const Text(
          'Use Bold Font',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: const Text(
          'Use bold font for app titles',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
        value: _useBoldFont,
        onChanged: (value) {
          setState(() {
            _useBoldFont = value;
          });
          _saveSettings();
        },
      ),
      const ListTile(
        title: Text(
          'Horizontal Alignment',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: Text(
          'Set the horizontal position of the app list',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
      ),
      RadioListTile<String>(
        title: const Text(
          'Left',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        value: 'left',
        groupValue: _singleColumnPosition,
        activeColor: Colors.black,
        fillColor: WidgetStateProperty.all(Colors.black),
        controlAffinity: ListTileControlAffinity.trailing,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0),
        dense: true,
        visualDensity: VisualDensity.compact,
        onChanged: (value) {
          setState(() {
            _singleColumnPosition = value!;
          });
          _saveSettings();
        },
      ),
      RadioListTile<String>(
        title: const Text(
          'Center',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        value: 'center',
        groupValue: _singleColumnPosition,
        activeColor: Colors.black,
        fillColor: WidgetStateProperty.all(Colors.black),
        controlAffinity: ListTileControlAffinity.trailing,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0),
        dense: true,
        visualDensity: VisualDensity.compact,
        onChanged: (value) {
          setState(() {
            _singleColumnPosition = value!;
          });
          _saveSettings();
        },
      ),
      RadioListTile<String>(
        title: const Text(
          'Right',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        value: 'right',
        groupValue: _singleColumnPosition,
        activeColor: Colors.black,
        fillColor: WidgetStateProperty.all(Colors.black),
        controlAffinity: ListTileControlAffinity.trailing,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0),
        dense: true,
        visualDensity: VisualDensity.compact,
        onChanged: (value) {
          setState(() {
            _singleColumnPosition = value!;
          });
          _saveSettings();
        },
      ),
      const ListTile(
        title: Text(
          'Vertical Alignment',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: Text(
          'Set the vertical position of the app list',
          style: TextStyle(fontSize: _kSubtitleFontSize),
        ),
      ),
      RadioListTile<String>(
        title: const Text(
          'Top',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        value: 'top',
        groupValue: widget.prefs.getString('verticalAlignment') ?? 'top',
        activeColor: Colors.black,
        fillColor: WidgetStateProperty.all(Colors.black),
        controlAffinity: ListTileControlAffinity.trailing,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0),
        dense: true,
        visualDensity: VisualDensity.compact,
        onChanged: (value) {
          setState(() {
            widget.prefs.setString('verticalAlignment', value!);
          });
        },
      ),
      RadioListTile<String>(
        title: const Text(
          'Center',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        value: 'center',
        groupValue: widget.prefs.getString('verticalAlignment') ?? 'top',
        activeColor: Colors.black,
        fillColor: WidgetStateProperty.all(Colors.black),
        controlAffinity: ListTileControlAffinity.trailing,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0),
        dense: true,
        visualDensity: VisualDensity.compact,
        onChanged: (value) {
          setState(() {
            widget.prefs.setString('verticalAlignment', value!);
          });
        },
      ),
      RadioListTile<String>(
        title: const Text(
          'Bottom',
          style: TextStyle(
            fontSize: _kFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        value: 'bottom',
        groupValue: widget.prefs.getString('verticalAlignment') ?? 'top',
        activeColor: Colors.black,
        fillColor: WidgetStateProperty.all(Colors.black),
        controlAffinity: ListTileControlAffinity.trailing,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0),
        dense: true,
        visualDensity: VisualDensity.compact,
        onChanged: (value) {
          setState(() {
            widget.prefs.setString('verticalAlignment', value!);
          });
        },
      ),
    ];
  }

  List<Widget> get _currentPageItems {
    if (!_usePagination) return _getSettingsItems();

    final start = _currentPage * _itemsPerPage;
    final end = (start + _itemsPerPage).clamp(0, _getSettingsItems().length);
    return _getSettingsItems().sublist(start, end);
  }

  int get _totalPages => (_getSettingsItems().length / _itemsPerPage).ceil();

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
    return Container(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.only(top: 16.0),
        child: Scaffold(
          backgroundColor: Colors.white,
          appBar: AppBar(
            title: const Text('Home Screen Settings'),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back_rounded),
              onPressed: () => Navigator.pop(context),
            ),
          ),
          body: Theme(
            data: Theme.of(context).copyWith(
              splashColor: Colors.transparent,
              highlightColor: Colors.transparent,
              hoverColor: Colors.transparent,
              focusColor: Colors.transparent,
            ),
            child: Column(
              children: [
                Expanded(
                  child: ScrollConfiguration(
                    behavior: NoGlowScrollBehavior(),
                    child: ListView(
                      physics: _usePagination
                          ? const NeverScrollableScrollPhysics()
                          : const ClampingScrollPhysics(),
                      children: _currentPageItems,
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
}

// Class to remove any overscroll effect (glow, stretch, bounce)
class NoGlowScrollBehavior extends ScrollBehavior {
  @override
  Widget buildOverscrollIndicator(
      BuildContext context, Widget child, ScrollableDetails details) {
    return child;
  }
}
