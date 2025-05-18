import 'dart:typed_data';
import '../services/icon_processor_service.dart';

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

  AppInfo({
    required this.name,
    this.icon,
    required this.packageName,
    required this.versionName,
    required this.versionCode,
    required this.builtWith,
    required this.installedTimestamp,
  });

  Future<Uint8List?> getProcessedIcon() async {
    if (_processedIcon != null) return _processedIcon;
    _processedIcon = await IconProcessorService.getProcessedIcon(packageName);
    return _processedIcon;
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
