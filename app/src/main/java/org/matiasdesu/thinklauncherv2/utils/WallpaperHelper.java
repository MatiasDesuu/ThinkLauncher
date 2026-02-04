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
