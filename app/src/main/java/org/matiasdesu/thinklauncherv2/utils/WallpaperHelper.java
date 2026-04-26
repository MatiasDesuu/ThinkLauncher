package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WallpaperHelper {

    private static final String WALLPAPER_FILENAME = "custom_wallpaper.png";

    /**
     * Save a wallpaper bitmap to internal storage
     */
    public static void saveWallpaper(Context context, Bitmap bitmap) {
        File file = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Load the saved wallpaper bitmap
     */
    public static Bitmap loadWallpaper(Context context) {
        File file = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        if (!file.exists()) {
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return BitmapFactory.decodeStream(fis);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Check if a custom wallpaper exists
     */
    public static boolean hasWallpaper(Context context) {
        File file = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        return file.exists();
    }

    /**
     * Remove the custom wallpaper
     */
    public static void removeWallpaper(Context context) {
        File file = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Get the wallpaper scaled and positioned for the screen
     * @param context Context
     * @param screenWidth Target screen width
     * @param screenHeight Target screen height
     * @return Bitmap cropped and scaled for the screen, or null if no wallpaper
     */
    public static Bitmap getWallpaperForScreen(Context context, int screenWidth, int screenHeight) {
        Bitmap wallpaper = loadWallpaper(context);
        if (wallpaper == null) {
            return null;
        }

        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        float offsetX = prefs.getFloat("wallpaper_offset_x", 0.5f);
        float offsetY = prefs.getFloat("wallpaper_offset_y", 0.5f);

        return cropWallpaperForScreen(wallpaper, screenWidth, screenHeight, offsetX, offsetY);
    }

    public static Bitmap getWallpaperForScreen(Context context, int screenWidth, int screenHeight, boolean blur) {
        return getWallpaperForScreen(context, screenWidth, screenHeight, blur, 3);
    }

    public static Bitmap getWallpaperForScreen(Context context, int screenWidth, int screenHeight, boolean blur,
            int blurStrength) {
        Bitmap wallpaper = getWallpaperForScreen(context, screenWidth, screenHeight);
        if (wallpaper == null || !blur) {
            return wallpaper;
        }
        return blurBitmap(wallpaper, getBlurRadiusForStrength(blurStrength));
    }

    /**
     * Crop and scale wallpaper to fit screen with given offset
     */
    public static Bitmap cropWallpaperForScreen(Bitmap bitmap, int screenWidth, int screenHeight, 
                                                 float offsetX, float offsetY) {
        if (bitmap == null) return null;

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();
        
        float screenRatio = (float) screenWidth / screenHeight;
        float bitmapRatio = bitmapWidth / bitmapHeight;

        float srcLeft, srcTop, srcWidth, srcHeight;

        if (bitmapRatio > screenRatio) {
            // Bitmap is wider than screen - crop horizontally
            srcHeight = bitmapHeight;
            srcWidth = bitmapHeight * screenRatio;
            srcTop = 0;
            srcLeft = (bitmapWidth - srcWidth) * offsetX;
        } else {
            // Bitmap is taller than screen - crop vertically
            srcWidth = bitmapWidth;
            srcHeight = bitmapWidth / screenRatio;
            srcLeft = 0;
            srcTop = (bitmapHeight - srcHeight) * offsetY;
        }

        // Create output bitmap
        Bitmap result = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // Draw the cropped portion of the wallpaper
        RectF srcRect = new RectF(srcLeft, srcTop, srcLeft + srcWidth, srcTop + srcHeight);
        RectF dstRect = new RectF(0, 0, screenWidth, screenHeight);
        
        canvas.drawBitmap(bitmap,
            new android.graphics.Rect((int)srcRect.left, (int)srcRect.top, (int)srcRect.right, (int)srcRect.bottom),
            new android.graphics.Rect((int)dstRect.left, (int)dstRect.top, (int)dstRect.right, (int)dstRect.bottom),
            null);

        return result;
    }

    public static Bitmap blurBitmap(Bitmap source, int radius) {
        if (source == null || radius < 1) {
            return source;
        }

        Bitmap bitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int wm = width - 1;
        int hm = height - 1;
        int wh = width * height;
        int div = radius + radius + 1;

        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int[] vmin = new int[Math.max(width, height)];
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (int i = 0; i < dv.length; i++) {
            dv[i] = i / divsum;
        }

        int yi = 0;
        int yw = 0;
        int[][] stack = new int[div][3];

        for (int y = 0; y < height; y++) {
            int rinsum = 0, ginsum = 0, binsum = 0;
            int routsum = 0, goutsum = 0, boutsum = 0;
            int rsum = 0, gsum = 0, bsum = 0;
            for (int i = -radius; i <= radius; i++) {
                int pixel = pixels[yi + Math.min(wm, Math.max(i, 0))];
                int[] sir = stack[i + radius];
                sir[0] = (pixel & 0xff0000) >> 16;
                sir[1] = (pixel & 0x00ff00) >> 8;
                sir[2] = pixel & 0x0000ff;
                int rbs = radius + 1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            int stackPointer = radius;
            for (int x = 0; x < width; x++) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                int stackStart = stackPointer - radius + div;
                int[] sir = stack[stackStart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                int pixel = pixels[yw + vmin[x]];

                sir[0] = (pixel & 0xff0000) >> 16;
                sir[1] = (pixel & 0x00ff00) >> 8;
                sir[2] = pixel & 0x0000ff;

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackPointer = (stackPointer + 1) % div;
                sir = stack[stackPointer % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += width;
        }

        for (int x = 0; x < width; x++) {
            int rinsum = 0, ginsum = 0, binsum = 0;
            int routsum = 0, goutsum = 0, boutsum = 0;
            int rsum = 0, gsum = 0, bsum = 0;
            int yp = -radius * width;
            for (int i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                int[] sir = stack[i + radius];
                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];
                int rbs = radius + 1 - Math.abs(i);
                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                if (i < hm) {
                    yp += width;
                }
            }
            yi = x;
            int stackPointer = radius;
            for (int y = 0; y < height; y++) {
                pixels[yi] = (pixels[yi] & 0xff000000) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                int stackStart = stackPointer - radius + div;
                int[] sir = stack[stackStart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + radius + 1, hm) * width;
                }
                int p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackPointer = (stackPointer + 1) % div;
                sir = stack[stackPointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += width;
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static int getBlurRadiusForStrength(int strength) {
        switch (strength) {
            case 1:
                return 6;
            case 2:
                return 12;
            case 3:
                return 18;
            case 4:
                return 24;
            case 5:
                return 30;
            default:
                return 18;
        }
    }

    /**
     * Get screen dimensions
     */
    public static int[] getScreenDimensions(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return new int[]{metrics.widthPixels, metrics.heightPixels};
    }
}
