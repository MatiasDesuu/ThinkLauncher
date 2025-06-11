import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/services.dart';
import 'screens/main_screen.dart';
import 'services/icon_cache_service.dart';

const _kPrimaryColor = Colors.black;
const _kSurfaceColor = Colors.white;
const _kBorderRadius = 12.0;
const _kBorderWidth = 1.0;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();

  await IconCacheService.initialize();

  _configureSystemUI(prefs);
  timeDilation = 0.0001;

  runApp(MyApp(prefs: prefs));
}

void _configureSystemUI(SharedPreferences prefs) {
  final showStatusBar = prefs.getBool('showStatusBar') ?? false;
  SystemChrome.setEnabledSystemUIMode(
    SystemUiMode.manual,
    overlays: showStatusBar
        ? [SystemUiOverlay.top, SystemUiOverlay.bottom]
        : [SystemUiOverlay.bottom],
  );

  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: _kSurfaceColor,
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );
}

class MyApp extends StatelessWidget {
  final SharedPreferences prefs;
  const MyApp({super.key, required this.prefs});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'InkLauncher',
      debugShowCheckedModeBanner: false,
      theme: _buildTheme(),
      home: MainScreen(prefs: prefs),
    );
  }

  ThemeData _buildTheme() {
    return ThemeData(
      useMaterial3: true,
      colorScheme: const ColorScheme.light(
        primary: _kPrimaryColor,
        onPrimary: _kSurfaceColor,
        secondary: _kPrimaryColor,
        onSecondary: _kSurfaceColor,
        surface: _kSurfaceColor,
        onSurface: _kPrimaryColor,
        error: _kPrimaryColor,
        onError: _kSurfaceColor,
        surfaceContainerHighest: _kSurfaceColor,
        onSurfaceVariant: _kPrimaryColor,
        outline: _kPrimaryColor,
        outlineVariant: _kPrimaryColor,
      ),
      appBarTheme: const AppBarTheme(
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        backgroundColor: _kSurfaceColor,
        foregroundColor: _kPrimaryColor,
        elevation: 0,
      ),
      cardTheme: const CardTheme(
        elevation: 0,
        color: _kSurfaceColor,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(_kBorderRadius)),
          side: BorderSide(color: _kPrimaryColor, width: _kBorderWidth),
        ),
      ),
      iconTheme: const IconThemeData(color: _kPrimaryColor),
      textTheme: const TextTheme(
        bodyLarge: TextStyle(color: _kPrimaryColor),
        bodyMedium: TextStyle(color: _kPrimaryColor),
        titleLarge: TextStyle(color: _kPrimaryColor),
      ),
      dividerTheme: const DividerThemeData(color: _kPrimaryColor),
      listTileTheme: const ListTileThemeData(
        textColor: _kPrimaryColor,
        iconColor: _kPrimaryColor,
      ),
      filledButtonTheme: _buildFilledButtonTheme(),
      outlinedButtonTheme: _buildOutlinedButtonTheme(),
      textButtonTheme: _buildTextButtonTheme(),
      inputDecorationTheme: _buildInputDecorationTheme(),
      splashColor: Colors.transparent,
      highlightColor: Colors.transparent,
      hoverColor: Colors.transparent,
      focusColor: Colors.transparent,
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: NoTransitionsBuilder(),
          TargetPlatform.iOS: NoTransitionsBuilder(),
        },
      ),
    );
  }

  FilledButtonThemeData _buildFilledButtonTheme() {
    return FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: _kPrimaryColor,
        foregroundColor: _kSurfaceColor,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(_kBorderRadius),
        ),
      ),
    );
  }

  OutlinedButtonThemeData _buildOutlinedButtonTheme() {
    return OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: _kPrimaryColor,
        side: const BorderSide(color: _kPrimaryColor),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(_kBorderRadius),
        ),
      ),
    );
  }

  TextButtonThemeData _buildTextButtonTheme() {
    return TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: _kPrimaryColor,
      ),
    );
  }

  InputDecorationTheme _buildInputDecorationTheme() {
    return InputDecorationTheme(
      filled: true,
      fillColor: _kSurfaceColor,
      border: _buildInputBorder(),
      enabledBorder: _buildInputBorder(),
      focusedBorder: _buildInputBorder(width: 2),
      errorBorder: _buildInputBorder(),
      focusedErrorBorder: _buildInputBorder(width: 2),
    );
  }

  OutlineInputBorder _buildInputBorder({double width = 1}) {
    return OutlineInputBorder(
      borderRadius: BorderRadius.circular(_kBorderRadius),
      borderSide: BorderSide(color: _kPrimaryColor, width: width),
    );
  }
}

class NoTransitionsBuilder extends PageTransitionsBuilder {
  const NoTransitionsBuilder();

  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    return child;
  }
}
