import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

class IconProcessorService {
  static const MethodChannel _channel =
      MethodChannel('com.example.thinklauncher/icon_processor');

  static Future<Uint8List?> getProcessedIcon(String packageName) async {
    try {
      final result =
          await _channel.invokeMethod<Uint8List>('getProcessedIcon', {
        'packageName': packageName,
      });
      return result;
    } on PlatformException catch (e) {
      debugPrint('Error processing icon: ${e.message}');
      return null;
    } catch (e) {
      debugPrint('Unexpected error processing icon: $e');
      return null;
    }
  }
}
