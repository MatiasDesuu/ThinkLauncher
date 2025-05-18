package com.desu.think_launcher

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.ByteArrayOutputStream

class IconProcessor(private val packageManager: PackageManager) : MethodCallHandler {
    companion object {
        private const val CHANNEL = "com.example.thinklauncher/icon_processor"
        private const val BRIGHTNESS_THRESHOLD = 128 // Umbral para separar claro/oscuro
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getProcessedIcon" -> {
                val packageName = call.argument<String>("packageName")
                if (packageName == null) {
                    result.error("INVALID_ARGUMENT", "Package name is required", null)
                    return
                }
                try {
                    val iconBytes = processAppIcon(packageName)
                    if (iconBytes != null) {
                        result.success(iconBytes)
                    } else {
                        result.error("ICON_PROCESSING_ERROR", "Failed to process icon", null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    result.error("ICON_PROCESSING_ERROR", e.message ?: "Unknown error", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun isSimpleIcon(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val colorSet = mutableSetOf<Int>()
        var opaqueCount = 0
        var transparentCount = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha < 128) {
                    transparentCount++
                } else {
                    opaqueCount++
                    colorSet.add(Color.rgb(Color.red(pixel), Color.green(pixel), Color.blue(pixel)))
                }
            }
        }

        // Si hay muy pocos píxeles opacos (líneas finas)
        if (opaqueCount < width * height * 0.25) return true
        // Si hay solo 2 o 3 colores dominantes (monocromático)
        if (colorSet.size <= 3) return true
        return false
    }

    private fun processAppIcon(packageName: String): ByteArray? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val drawable = packageManager.getApplicationIcon(appInfo)
            val bitmap = drawableToBitmap(drawable)
            // Detectar si el ícono ya es simple
            if (isSimpleIcon(bitmap)) {
                return bitmapToByteArray(bitmap)
            }
            val processedBitmap = convertToBlackAndWhite(bitmap)
            bitmapToByteArray(processedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun convertToBlackAndWhite(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val windowSize = 15
        val thresholdOffset = 15

        val gray = Array(height) { IntArray(width) }

        // Convertir a escala de grises y marcar transparencia
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha < 128) {
                    gray[y][x] = -1 // Marcamos como transparente
                } else {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    gray[y][x] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                }
            }
        }

        // Binarización adaptativa respetando la transparencia
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (gray[y][x] == -1) {
                    resultBitmap.setPixel(x, y, Color.TRANSPARENT)
                    continue
                }
                var sum = 0
                var count = 0
                for (dy in -windowSize/2..windowSize/2) {
                    for (dx in -windowSize/2..windowSize/2) {
                        val ny = y + dy
                        val nx = x + dx
                        if (nx in 0 until width && ny in 0 until height && gray[ny][nx] != -1) {
                            sum += gray[ny][nx]
                            count++
                        }
                    }
                }
                val localMean = if (count > 0) sum / count else gray[y][x]
                val value = if (gray[y][x] < localMean - thresholdOffset) Color.BLACK else Color.WHITE
                resultBitmap.setPixel(x, y, Color.argb(255, Color.red(value), Color.green(value), Color.blue(value)))
            }
        }
        return resultBitmap
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
} 