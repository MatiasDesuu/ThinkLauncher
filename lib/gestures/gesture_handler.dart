import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:installed_apps/installed_apps.dart';
import '../screens/search_screen.dart';

class GestureHandler {
  final SharedPreferences prefs;
  final BuildContext context;

  GestureHandler({
    required this.prefs,
    required this.context,
  });

  void handleVerticalDrag(DragUpdateDetails details) {
    if (details.delta.dy > 0) {
      // Swipe down
      final isSwipeDownEnabled = prefs.getBool('enableSwipeDown') ?? true;
      if (!isSwipeDownEnabled) return;

      final useSearchForSwipeDown =
          prefs.getBool('useSearchForSwipeDown') ?? true;
      if (useSearchForSwipeDown) {
        _openSearch();
      } else {
        final appPackage = prefs.getString('swipeDownApp');
        if (appPackage != null) {
          InstalledApps.startApp(appPackage);
        }
      }
    } else if (details.delta.dy < 0) {
      // Swipe up
      final isSwipeUpEnabled = prefs.getBool('enableSwipeUp') ?? true;
      if (!isSwipeUpEnabled) return;

      final useSearchForSwipeUp = prefs.getBool('useSearchForSwipeUp') ?? true;
      if (useSearchForSwipeUp) {
        _openSearch();
      } else {
        final appPackage = prefs.getString('swipeUpApp');
        if (appPackage != null) {
          InstalledApps.startApp(appPackage);
        }
      }
    }
  }

  void handleHorizontalDrag(DragUpdateDetails details) {
    if (details.delta.dx > 0) {
      // Left to right
      final isSwipeRightEnabled = prefs.getBool('enableSwipeRight') ?? true;
      if (!isSwipeRightEnabled) return;

      final useSearchForSwipeRight =
          prefs.getBool('useSearchForSwipeRight') ?? true;
      if (useSearchForSwipeRight) {
        _openSearch();
      } else {
        final appPackage = prefs.getString('swipeRightApp');
        if (appPackage != null) {
          InstalledApps.startApp(appPackage);
        }
      }
    } else {
      // Right to left
      final isSwipeLeftEnabled = prefs.getBool('enableSwipeLeft') ?? true;
      if (!isSwipeLeftEnabled) return;

      final useSearchForSwipeLeft =
          prefs.getBool('useSearchForSwipeLeft') ?? true;
      if (useSearchForSwipeLeft) {
        _openSearch();
      } else {
        final appPackage = prefs.getString('swipeLeftApp');
        if (appPackage != null) {
          InstalledApps.startApp(appPackage);
        }
      }
    }
  }

  Future<void> _openSearch() async {
    final autoFocus = prefs.getBool('autoFocusSearch') ?? true;
    if (!context.mounted) return;

    await Navigator.push(
      context,
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) => SearchScreen(
          prefs: prefs,
          autoFocus: autoFocus,
        ),
        transitionDuration: Duration.zero,
        reverseTransitionDuration: Duration.zero,
      ),
    );
  }
}
