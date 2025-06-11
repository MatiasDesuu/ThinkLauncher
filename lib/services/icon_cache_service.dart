import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/foundation.dart';

class IconCacheService {
  static const String _cacheKey = 'processed_icons_cache';
  static Map<String, String> _cache = {};

  static Future<void> initialize() async {
    final prefs = await SharedPreferences.getInstance();
    final cachedData = prefs.getString(_cacheKey);
    if (cachedData != null) {
      try {
        _cache = Map<String, String>.from(json.decode(cachedData));
      } catch (e) {
        debugPrint('Error loading icon cache: $e');
        _cache = {};
      }
    }
  }

  static Future<void> saveIcon(String packageName, Uint8List iconData) async {
    try {
      _cache[packageName] = base64Encode(iconData);
      await _persistCache();
    } catch (e) {
      debugPrint('Error saving icon to cache: $e');
    }
  }

  static Uint8List? getIcon(String packageName) {
    try {
      final encodedData = _cache[packageName];
      if (encodedData != null) {
        return base64Decode(encodedData);
      }
    } catch (e) {
      debugPrint('Error getting icon from cache: $e');
    }
    return null;
  }

  static bool hasIcon(String packageName) {
    return _cache.containsKey(packageName);
  }

  static Future<void> clearCache() async {
    _cache.clear();
    await _persistCache();
  }

  static Future<void> _persistCache() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_cacheKey, json.encode(_cache));
    } catch (e) {
      debugPrint('Error persisting icon cache: $e');
    }
  }
}
