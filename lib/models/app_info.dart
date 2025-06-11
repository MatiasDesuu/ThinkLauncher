import 'dart:async';
import '../services/icon_processor_service.dart';
import '../services/icon_cache_service.dart';
import 'package:flutter/foundation.dart';

enum BuiltWith {
  flutter,
  native,
  unknown,
}

class AppInfo {
  final String name;
  final Uint8List? icon;
  final String packageName;
  final String versionName;
  final int versionCode;
  final BuiltWith builtWith;
  final int installedTimestamp;
  Uint8List? _processedIcon;
  static final Map<String, Uint8List> _iconCache = {};
  static final Map<String, StreamController<Uint8List?>> _streamControllers =
      {};

  AppInfo({
    required this.name,
    this.icon,
    required this.packageName,
    required this.versionName,
    required this.versionCode,
    required this.builtWith,
    required this.installedTimestamp,
  }) {
    // Intentar cargar el icono procesado inmediatamente
    _initializeProcessedIcon();
  }

  Future<void> _initializeProcessedIcon() async {
    // Primero intentamos obtener el icono de la caché estática
    if (_iconCache.containsKey(packageName)) {
      _processedIcon = _iconCache[packageName];
      return;
    }

    // Luego intentamos obtener el icono de la caché persistente
    final cachedIcon = IconCacheService.getIcon(packageName);
    if (cachedIcon != null) {
      _processedIcon = cachedIcon;
      _iconCache[packageName] = cachedIcon;
      return;
    }

    // Si no está en caché, procesamos el icono y lo guardamos
    _processedIcon = await IconProcessorService.getProcessedIcon(packageName);
    if (_processedIcon != null) {
      await IconCacheService.saveIcon(packageName, _processedIcon!);
      _iconCache[packageName] = _processedIcon!;
    }
  }

  Stream<Uint8List?> getProcessedIconStream() {
    // Si ya tenemos el icono procesado en caché, lo devolvemos inmediatamente
    if (_processedIcon != null) {
      return Stream.value(_processedIcon);
    }

    // Si el icono está en la caché estática, lo devolvemos inmediatamente
    if (_iconCache.containsKey(packageName)) {
      _processedIcon = _iconCache[packageName];
      return Stream.value(_processedIcon);
    }

    // Si no hay un stream controller para este icono, lo creamos
    if (!_streamControllers.containsKey(packageName)) {
      _streamControllers[packageName] = StreamController<Uint8List?>();
      _loadProcessedIcon();
    }

    return _streamControllers[packageName]!.stream;
  }

  Future<void> _loadProcessedIcon() async {
    try {
      // Primero intentamos obtener el icono de la caché en memoria
      if (_processedIcon != null) {
        _streamControllers[packageName]?.add(_processedIcon);
        return;
      }

      // Luego intentamos obtener el icono de la caché persistente
      final cachedIcon = IconCacheService.getIcon(packageName);
      if (cachedIcon != null) {
        _processedIcon = cachedIcon;
        _iconCache[packageName] = cachedIcon;
        _streamControllers[packageName]?.add(_processedIcon);
        return;
      }

      // Si no está en caché, procesamos el icono y lo guardamos
      _processedIcon = await IconProcessorService.getProcessedIcon(packageName);
      if (_processedIcon != null) {
        await IconCacheService.saveIcon(packageName, _processedIcon!);
        _iconCache[packageName] = _processedIcon!;
      }
      _streamControllers[packageName]?.add(_processedIcon ?? icon);
    } catch (e) {
      debugPrint('Error processing icon for $packageName: $e');
      _streamControllers[packageName]?.add(icon);
    }
  }

  Future<Uint8List?> getProcessedIcon() async {
    // Primero intentamos obtener el icono de la caché en memoria
    if (_processedIcon != null) return _processedIcon;

    // Luego intentamos obtener el icono de la caché estática
    if (_iconCache.containsKey(packageName)) {
      _processedIcon = _iconCache[packageName];
      return _processedIcon;
    }

    // Luego intentamos obtener el icono de la caché persistente
    final cachedIcon = IconCacheService.getIcon(packageName);
    if (cachedIcon != null) {
      _processedIcon = cachedIcon;
      _iconCache[packageName] = cachedIcon;
      return _processedIcon;
    }

    // Si no está en caché, procesamos el icono y lo guardamos
    _processedIcon = await IconProcessorService.getProcessedIcon(packageName);
    if (_processedIcon != null) {
      await IconCacheService.saveIcon(packageName, _processedIcon!);
      _iconCache[packageName] = _processedIcon!;
    }
    return _processedIcon;
  }

  static void disposeAll() {
    for (final controller in _streamControllers.values) {
      controller.close();
    }
    _streamControllers.clear();
  }

  factory AppInfo.fromInstalledApps(dynamic app) {
    return AppInfo(
      name: app.name as String,
      icon: app.icon as Uint8List?,
      packageName: app.packageName as String,
      versionName: app.versionName as String,
      versionCode: app.versionCode as int,
      builtWith: BuiltWith.values.firstWhere(
        (e) => e.toString() == 'BuiltWith.${app.builtWith}',
        orElse: () => BuiltWith.unknown,
      ),
      installedTimestamp: app.installedTimestamp as int,
    );
  }

  factory AppInfo.fromJson(Map<String, dynamic> json) {
    return AppInfo(
      name: json['name'] as String,
      icon: json['icon'] as Uint8List?,
      packageName: json['packageName'] as String,
      versionName: json['versionName'] as String,
      versionCode: json['versionCode'] as int,
      builtWith: BuiltWith.values.firstWhere(
        (e) => e.toString() == 'BuiltWith.${json['builtWith']}',
        orElse: () => BuiltWith.unknown,
      ),
      installedTimestamp: json['installedTimestamp'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'icon': icon,
      'packageName': packageName,
      'versionName': versionName,
      'versionCode': versionCode,
      'builtWith': builtWith.toString().split('.').last,
      'installedTimestamp': installedTimestamp,
    };
  }
}
