import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:flutter/services.dart';
import '../models/app_info.dart';
import 'app_selection_screen.dart';
import 'gesture_settings_screen.dart';

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
  bool useBoldFont = false;
  double appFontSize = 18.0;
  double appIconSize = 27.0;
  bool enableScroll = true;
  bool showIcons = false;
  bool showAppTitles = true;
  bool showStatusBar = false;
  bool usePagination = false;
  List<String> selectedApps = [];
  bool isLoading = false;
  String? errorMessage;
  bool showSavedMessage = false;
  bool useBlackAndWhiteIcons = false;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  void _loadSettings() {
    setState(() {
      numApps = widget.prefs.getInt('numApps') ?? 5;
      numColumns = widget.prefs.getInt('numColumns') ?? 1;
      showDateTime = widget.prefs.getBool('showDateTime') ?? true;
      showSearchButton = widget.prefs.getBool('showSearchButton') ?? true;
      showSettingsButton = widget.prefs.getBool('showSettingsButton') ?? true;
      useBoldFont = widget.prefs.getBool('useBoldFont') ?? false;
      appFontSize = widget.prefs.getDouble('appFontSize') ?? 18.0;
      appIconSize = widget.prefs.getDouble('appIconSize') ?? 27.0;
      enableScroll = widget.prefs.getBool('enableScroll') ?? true;
      showIcons = widget.prefs.getBool('showIcons') ?? false;
      showAppTitles = widget.prefs.getBool('showAppTitles') ?? true;
      showStatusBar = widget.prefs.getBool('showStatusBar') ?? false;
      usePagination = widget.prefs.getBool('usePagination') ?? false;
      selectedApps = widget.prefs.getStringList('selectedApps') ?? [];
      useBlackAndWhiteIcons =
          widget.prefs.getBool('useBlackAndWhiteIcons') ?? false;

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

  Future<void> _saveSettings() async {
    try {
      setState(() {
        isLoading = true;
        errorMessage = null;
        showSavedMessage = false;
      });

      // Validate that there are no more selected apps than the maximum number
      if (selectedApps.length > numApps) {
        selectedApps = selectedApps.sublist(0, numApps);
      }

      // Check if both are disabled and enable settings button if necessary
      final enableLongPressGesture =
          widget.prefs.getBool('enableLongPressGesture') ?? true;
      if (!showSettingsButton && !enableLongPressGesture) {
        showSettingsButton = true;
      }

      await widget.prefs.setInt('numApps', numApps);
      await widget.prefs.setInt('numColumns', numColumns);
      await widget.prefs.setBool('showDateTime', showDateTime);
      await widget.prefs.setBool('showSearchButton', showSearchButton);
      await widget.prefs.setBool('showSettingsButton', showSettingsButton);
      await widget.prefs.setBool('useBoldFont', useBoldFont);
      await widget.prefs.setDouble('appFontSize', appFontSize);
      await widget.prefs.setDouble('appIconSize', appIconSize);
      await widget.prefs.setBool('enableScroll', enableScroll);
      await widget.prefs.setBool('showIcons', showIcons);
      await widget.prefs.setBool('showAppTitles', showAppTitles);
      await widget.prefs.setBool('showStatusBar', showStatusBar);
      await widget.prefs.setBool('usePagination', usePagination);
      await widget.prefs.setStringList('selectedApps', selectedApps);
      await widget.prefs
          .setBool('useBlackAndWhiteIcons', useBlackAndWhiteIcons);

      // Update status bar visibility
      SystemChrome.setEnabledSystemUIMode(
        SystemUiMode.manual,
        overlays: showStatusBar
            ? [SystemUiOverlay.top, SystemUiOverlay.bottom]
            : [SystemUiOverlay.bottom],
      );

      if (mounted) {
        setState(() {
          showSavedMessage = true;
        });
        Future.delayed(const Duration(seconds: 1), () {
          if (mounted) {
            setState(() {
              showSavedMessage = false;
            });
          }
        });
      }
    } catch (e) {
      debugPrint('Error saving settings: $e');
      if (mounted) {
        setState(() {
          errorMessage = 'Error saving settings';
        });
      }
    } finally {
      if (mounted) {
        setState(() {
          isLoading = false;
        });
      }
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

  @override
  Widget build(BuildContext context) {
    final enableLongPressGesture =
        widget.prefs.getBool('enableLongPressGesture') ?? true;
    final enableSwipeUp = widget.prefs.getBool('enableSwipeUp') ?? true;
    final enableSwipeDown = widget.prefs.getBool('enableSwipeDown') ?? true;
    final hasVerticalGestures = enableSwipeUp || enableSwipeDown;

    return Container(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.only(top: 16.0),
        child: Scaffold(
          backgroundColor: Colors.white,
          appBar: AppBar(
            title: const Text('Settings'),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => Navigator.pop(context),
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
          body: Stack(
            children: [
              Theme(
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
                      // 1. Number of apps
                      const Padding(
                        padding: EdgeInsets.all(16.0),
                        child: Text(
                          'Number of apps',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
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
                                          selectedApps =
                                              selectedApps.sublist(0, numApps);
                                        }
                                      });
                                    }
                                  : null,
                              icon: const Icon(Icons.remove),
                            ),
                            Expanded(
                              child: SliderTheme(
                                data: SliderTheme.of(context).copyWith(
                                  overlayShape: SliderComponentShape.noOverlay,
                                  valueIndicatorColor: Colors.transparent,
                                  valueIndicatorTextStyle:
                                      const TextStyle(color: Colors.black),
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
                                  max: 20,
                                  divisions: 19,
                                  label: numApps.toString(),
                                  onChanged: (value) {
                                    setState(() {
                                      numApps = value.toInt();
                                      if (selectedApps.length > numApps) {
                                        selectedApps =
                                            selectedApps.sublist(0, numApps);
                                      }
                                    });
                                  },
                                ),
                              ),
                            ),
                            IconButton(
                              onPressed: numApps < 20
                                  ? () {
                                      setState(() {
                                        numApps++;
                                      });
                                    }
                                  : null,
                              icon: const Icon(Icons.add),
                            ),
                          ],
                        ),
                      ),

                      // 2. Number of columns
                      const Padding(
                        padding: EdgeInsets.all(16.0),
                        child: Text(
                          'Number of columns',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
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
                              icon: const Icon(Icons.remove),
                            ),
                            Expanded(
                              child: SliderTheme(
                                data: SliderTheme.of(context).copyWith(
                                  overlayShape: SliderComponentShape.noOverlay,
                                  valueIndicatorColor: Colors.transparent,
                                  valueIndicatorTextStyle:
                                      const TextStyle(color: Colors.black),
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
                              icon: const Icon(Icons.add),
                            ),
                          ],
                        ),
                      ),

                      // 3. Show date, time and battery
                      SwitchListTile(
                        title: const Text(
                          'Show date, time and battery',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: showDateTime,
                        onChanged: (value) {
                          setState(() {
                            showDateTime = value;
                          });
                        },
                      ),

                      // 4. Show search button
                      SwitchListTile(
                        title: const Text(
                          'Show search button',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: showSearchButton,
                        onChanged: (value) {
                          setState(() {
                            showSearchButton = value;
                          });
                        },
                      ),

                      // 5. Show settings button
                      SwitchListTile(
                        title: const Text(
                          'Show settings button',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: showSettingsButton,
                        onChanged: !enableLongPressGesture
                            ? null
                            : (value) {
                                setState(() {
                                  showSettingsButton = value;
                                });
                              },
                      ),
                      if (!enableLongPressGesture)
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16.0),
                          child: Text(
                            'Long press gesture is disabled. Enable it in gesture settings to hide this button.',
                            style: TextStyle(
                              color: Theme.of(context).colorScheme.error,
                              fontSize: 12,
                            ),
                          ),
                        ),

                      // 6. Use bold font
                      SwitchListTile(
                        title: const Text(
                          'Use bold font',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: useBoldFont,
                        onChanged: (value) {
                          setState(() {
                            useBoldFont = value;
                          });
                        },
                      ),

                      // 7. Enable list scrolling
                      SwitchListTile(
                        title: const Text(
                          'Enable List Scrolling',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: enableScroll,
                        onChanged: hasVerticalGestures
                            ? null
                            : (value) async {
                                setState(() {
                                  enableScroll = value;
                                });
                                if (value) {
                                  // Desactivar gestos verticales si se activa el list scrolling
                                  await widget.prefs
                                      .setBool('enableSwipeUp', false);
                                  await widget.prefs
                                      .setBool('enableSwipeDown', false);
                                }
                              },
                      ),
                      if (hasVerticalGestures)
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16.0),
                          child: Text(
                            'Vertical swipe gestures are enabled. Disable them in gesture settings to use list scrolling.',
                            style: TextStyle(
                              color: Theme.of(context).colorScheme.error,
                              fontSize: 12,
                            ),
                          ),
                        ),

                      // 8. Show icons
                      SwitchListTile(
                        title: const Text(
                          'Show icons',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: showIcons,
                        onChanged: (value) {
                          setState(() {
                            showIcons = value;
                          });
                        },
                      ),

                      // 9. Show app titles
                      SwitchListTile(
                        title: const Text(
                          'Show app titles',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: showAppTitles,
                        onChanged: (value) {
                          setState(() {
                            showAppTitles = value;
                          });
                        },
                      ),

                      // 10. Show status bar
                      SwitchListTile(
                        title: const Text(
                          'Show status bar',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        value: showStatusBar,
                        onChanged: (value) {
                          setState(() {
                            showStatusBar = value;
                          });
                        },
                      ),

                      // 11. Pagination option
                      SwitchListTile(
                        title: const Text(
                          'Use pagination in search',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        subtitle: const Text(
                          'Show search results in pages instead of a scrollable list',
                          style: TextStyle(fontSize: 14),
                        ),
                        value: usePagination,
                        onChanged: (value) {
                          setState(() {
                            usePagination = value;
                          });
                        },
                      ),

                      // 12. App font size
                      const Padding(
                        padding: EdgeInsets.all(16.0),
                        child: Text(
                          'App font size',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
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
                              icon: const Icon(Icons.remove),
                            ),
                            Expanded(
                              child: SliderTheme(
                                data: SliderTheme.of(context).copyWith(
                                  overlayShape: SliderComponentShape.noOverlay,
                                  valueIndicatorColor: Colors.transparent,
                                  valueIndicatorTextStyle:
                                      const TextStyle(color: Colors.black),
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
                              icon: const Icon(Icons.add),
                            ),
                          ],
                        ),
                      ),

                      // 13. App icon size
                      const Padding(
                        padding: EdgeInsets.all(16.0),
                        child: Text(
                          'App icon size',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
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
                              icon: const Icon(Icons.remove),
                            ),
                            Expanded(
                              child: SliderTheme(
                                data: SliderTheme.of(context).copyWith(
                                  overlayShape: SliderComponentShape.noOverlay,
                                  valueIndicatorColor: Colors.transparent,
                                  valueIndicatorTextStyle:
                                      const TextStyle(color: Colors.black),
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
                              icon: const Icon(Icons.add),
                            ),
                          ],
                        ),
                      ),

                      // 14. App list
                      ListTile(
                        title: const Text(
                          'App list',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        subtitle: Text(
                            '${selectedApps.length} of $numApps apps selected'),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: _selectApps,
                      ),

                      // 15. Reorder apps
                      ListTile(
                        title: const Text(
                          'Reorder apps',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: selectedApps.isEmpty ? null : _reorderApps,
                      ),

                      // 16. Gestures
                      ListTile(
                        title: const Text(
                          'Gestures',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        subtitle: const Text('Configure application gestures'),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () {
                          Navigator.push(
                            context,
                            PageRouteBuilder(
                              pageBuilder: (context, animation,
                                      secondaryAnimation) =>
                                  GestureSettingsScreen(prefs: widget.prefs),
                              transitionDuration: Duration.zero,
                              reverseTransitionDuration: Duration.zero,
                            ),
                          );
                        },
                      ),

                      // 17. Black & White icons
                      SwitchListTile(
                        title: const Text(
                          'Black & White icons',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        subtitle: const Text(
                          'Experimental feature: may look pixelated or lose details in some icons.',
                          style: TextStyle(fontSize: 14),
                        ),
                        value: useBlackAndWhiteIcons,
                        onChanged: (value) {
                          setState(() {
                            useBlackAndWhiteIcons = value;
                          });
                        },
                      ),
                    ],
                  ),
                ),
              ),
              if (errorMessage != null)
                Positioned(
                  bottom: 16,
                  left: 16,
                  right: 16,
                  child: Material(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.black),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      padding: const EdgeInsets.all(16.0),
                      child: Row(
                        children: [
                          const Icon(Icons.error_outline, color: Colors.black),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              errorMessage!,
                              style: const TextStyle(color: Colors.black),
                            ),
                          ),
                          IconButton(
                            icon: const Icon(Icons.close, color: Colors.black),
                            onPressed: () {
                              setState(() {
                                errorMessage = null;
                              });
                            },
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class ReorderAppsScreen extends StatefulWidget {
  final List<String> selectedApps;

  const ReorderAppsScreen({super.key, required this.selectedApps});

  @override
  State<ReorderAppsScreen> createState() => _ReorderAppsScreenState();
}

class _ReorderAppsScreenState extends State<ReorderAppsScreen> {
  late List<String> apps;
  List<AppInfo> appInfos = [];
  bool isLoading = true;
  String? errorMessage;

  @override
  void initState() {
    super.initState();
    apps = List.from(widget.selectedApps);
    _loadAppInfos();
  }

  Future<void> _loadAppInfos() async {
    try {
      setState(() {
        errorMessage = null;
      });

      final futures = apps.map((packageName) =>
          InstalledApps.getAppInfo(packageName, null)
              .then((app) => AppInfo.fromInstalledApps(app)));

      final results = await Future.wait(futures);

      if (mounted) {
        setState(() {
          appInfos = results;
          isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading app information: $e');
      if (mounted) {
        setState(() {
          errorMessage = 'Error loading app information';
          isLoading = false;
        });
      }
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
            title: const Text('Reorder apps'),
            backgroundColor: Colors.white,
            foregroundColor: Colors.black,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () {
                setState(() {
                  // Update selected apps list with new order
                  widget.selectedApps.clear();
                  widget.selectedApps.addAll(apps);
                });
                Navigator.pop(context);
              },
            ),
          ),
          body: Stack(
            children: [
              if (errorMessage != null)
                Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        errorMessage!,
                        style: const TextStyle(fontSize: 16),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadAppInfos,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.black,
                          foregroundColor: Colors.white,
                        ),
                        child: const Text('Reattempt'),
                      ),
                    ],
                  ),
                )
              else
                ScrollConfiguration(
                  behavior: NoGlowScrollBehavior(),
                  child: ReorderableListView(
                    proxyDecorator: (child, index, animation) {
                      return Material(
                        color: Colors.white,
                        child: child,
                      );
                    },
                    buildDefaultDragHandles: false,
                    children: appInfos.map((app) {
                      return ListTile(
                        key: ValueKey(app.packageName),
                        leading: ReorderableDragStartListener(
                          index: appInfos.indexOf(app),
                          child: const Icon(Icons.drag_handle),
                        ),
                        title: Text(
                          app.name,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      );
                    }).toList(),
                    onReorder: (oldIndex, newIndex) {
                      setState(() {
                        if (newIndex > oldIndex) {
                          newIndex -= 1;
                        }
                        final item = apps.removeAt(oldIndex);
                        final appInfo = appInfos.removeAt(oldIndex);
                        apps.insert(newIndex, item);
                        appInfos.insert(newIndex, appInfo);
                      });
                    },
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
