package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WallpaperHelper {

    private static final String WALLPAPER_FILENAME = "custom_wallpaper.png";

    private static final LruCache<String, Bitmap> WALLPAPER_CACHE = new LruCache<String, Bitmap>(getDefaultCacheSize()) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 0 : value.getByteCount();
        }
    };

    private static int getDefaultCacheSize() {
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
        return (maxKb / 16) * 1024; // 1/16th of heap in bytes
    }

    private static String getCacheKey(Context context, int screenWidth, int screenHeight, boolean blur, int blurStrength) {
        File file = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        long lastModified = file.exists() ? file.lastModified() : 0L;
        long length = file.exists() ? file.length() : 0L;

        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        float offsetX = prefs.getFloat("wallpaper_offset_x", 0.5f);
        float offsetY = prefs.getFloat("wallpaper_offset_y", 0.5f);
        float scale = prefs.getFloat("wallpaper_scale", 1f);

        return screenWidth + "x" + screenHeight
                + "|blur=" + (blur ? 1 : 0)
                + "|s=" + blurStrength
                + "|ox=" + offsetX
                + "|oy=" + offsetY
                + "|sc=" + scale
                + "|lm=" + lastModified
                + "|len=" + length;
    }

    /**
     * Convenience overload matching the previous
     * {@link #getWallpaperForScreen(Context, int, int)} behavior, but served
     * from (and populated into) the LRU cache.
     */
    public static Bitmap getWallpaperForScreenCached(Context context, int screenWidth, int screenHeight) {
        return getWallpaperForScreenCached(context, screenWidth, screenHeight, false, 3);
    }

    /**
     * Cached version of {@link #getWallpaperForScreen(Context, int, int, boolean, int)}.
     * Returns null when no wallpaper exists or decoding fails.
     */
    public static Bitmap getWallpaperForScreenCached(Context context, int screenWidth, int screenHeight, boolean blur,
            int blurStrength) {
        if (!hasWallpaper(context)) {
            return null;
        }
        String key = getCacheKey(context, screenWidth, screenHeight, blur, blurStrength);
        Bitmap cached = WALLPAPER_CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap computed = getWallpaperForScreen(context, screenWidth, screenHeight, blur, blurStrength);
        if (computed != null) {
            WALLPAPER_CACHE.put(key, computed);
        }
        return computed;
    }

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
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
                    .putLong("wallpaper_file_modified", file.lastModified())
                    .apply();
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
     * Load the saved wallpaper downsampled to at most the screen size.
     */
    public static Bitmap loadWallpaper(Context context) {
        int[] dims = getScreenDimensions(context);
        return loadWallpaper(context, dims[0], dims[1]);
    }

    /**
     * Load the saved wallpaper bitmap, downsampled to at most the requested size
     * so we never hold a full-resolution decode in memory. On high-density
     * e-ink panels this can cut RAM usage by an order of magnitude.
     */
    public static Bitmap loadWallpaper(Context context, int maxWidth, int maxHeight) {
        File file = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        if (!file.exists()) {
            return null;
        }

        int width = Math.max(maxWidth, 1);
        int height = Math.max(maxHeight, 1);

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= width
                && bounds.outHeight / (sample * 2) >= height) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getPath(), opts);
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
        Bitmap wallpaper = loadWallpaper(context, screenWidth, screenHeight);
        if (wallpaper == null) {
            return null;
        }

        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        float offsetX = prefs.getFloat("wallpaper_offset_x", 0.5f);
        float offsetY = prefs.getFloat("wallpaper_offset_y", 0.5f);
        float scale = prefs.getFloat("wallpaper_scale", 1f);

        return cropWallpaperForScreen(wallpaper, screenWidth, screenHeight, offsetX, offsetY, scale);
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
     * Crop and scale wallpaper to fit screen with given offset and zoom
     */
    public static Bitmap cropWallpaperForScreen(Bitmap bitmap, int screenWidth, int screenHeight, 
                                                 float offsetX, float offsetY) {
        return cropWallpaperForScreen(bitmap, screenWidth, screenHeight, offsetX, offsetY, 1f);
    }

    /**
     * Compute the region of {@code bitmap} that covers a screen of the given
     * size, honoring pan offsets and zoom. This is the single source of truth
     * shared by the wallpaper settings preview and the home screen renderer,
     * so both always display exactly the same portion of the image.
     */
    public static android.graphics.Rect computeCropSrcRect(Bitmap bitmap, int screenWidth, int screenHeight,
                                                           float offsetX, float offsetY, float scale) {
        android.graphics.Rect out = new android.graphics.Rect();
        computeCropSrcRect(bitmap, screenWidth, screenHeight, offsetX, offsetY, scale, out);
        return out;
    }

    /**
     * Allocation-free variant that writes the result into {@code outRect},
     * safe to call from layout/draw passes.
     */
    public static void computeCropSrcRect(Bitmap bitmap, int screenWidth, int screenHeight,
                                          float offsetX, float offsetY, float scale,
                                          android.graphics.Rect outRect) {
        if (scale < 1f) scale = 1f;

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();

        float screenRatio = (float) screenWidth / screenHeight;
        float bitmapRatio = bitmapWidth / bitmapHeight;

        float baseWidth, baseHeight;
        if (bitmapRatio > screenRatio) {
            baseWidth = bitmapHeight * screenRatio;
            baseHeight = bitmapHeight;
        } else {
            baseWidth = bitmapWidth;
            baseHeight = bitmapWidth / screenRatio;
        }

        float srcWidth = baseWidth / scale;
        float srcHeight = baseHeight / scale;
        float srcLeft = (bitmapWidth - srcWidth) * offsetX;
        float srcTop = (bitmapHeight - srcHeight) * offsetY;

        // Clamp to bitmap bounds
        if (srcLeft < 0) srcLeft = 0;
        if (srcTop < 0) srcTop = 0;
        if (srcLeft + srcWidth > bitmapWidth) srcLeft = bitmapWidth - srcWidth;
        if (srcTop + srcHeight > bitmapHeight) srcTop = bitmapHeight - srcHeight;

        outRect.set((int) srcLeft, (int) srcTop,
                (int) (srcLeft + srcWidth), (int) (srcTop + srcHeight));
    }

    /**
     * Crop and scale wallpaper to fit screen with given offset and zoom
     */
    public static Bitmap cropWallpaperForScreen(Bitmap bitmap, int screenWidth, int screenHeight,
                                                 float offsetX, float offsetY, float scale) {
        if (bitmap == null) return null;

        android.graphics.Rect srcRect = computeCropSrcRect(bitmap, screenWidth, screenHeight,
                offsetX, offsetY, scale);

        // Create output bitmap. Use RGB_565 when the source has no alpha:
        // e-ink panels are grayscale anyway, and this halves memory usage.
        Bitmap.Config config = (!bitmap.hasAlpha() || bitmap.getConfig() == Bitmap.Config.RGB_565)
                ? Bitmap.Config.RGB_565
                : Bitmap.Config.ARGB_8888;
        Bitmap result = Bitmap.createBitmap(screenWidth, screenHeight, config);
        Canvas canvas = new Canvas(result);

        // Draw the cropped portion of the wallpaper
        RectF dstRect = new RectF(0, 0, screenWidth, screenHeight);

        canvas.drawBitmap(bitmap, srcRect,
            new android.graphics.Rect((int)dstRect.left, (int)dstRect.top, (int)dstRect.right, (int)dstRect.bottom),
            null);

        return result;
    }

    public static Bitmap blurBitmap(Bitmap source, int radius) {
        if (source == null || radius < 1) {
            return source;
        }

        // Blur at a capped resolution (e.g. 640px) and scale back up.
        // The stack-blur below allocates ~4 full int arrays, so blurring a
        // full-screen bitmap directly can eat tens of MB on high-density
        // e-ink panels. Visually the result is equivalent after upscaling.
        final int MAX_BLUR_DIMENSION = 640;
        Bitmap working = source;
        int scaleFactor = 1;
        if (source.getWidth() > MAX_BLUR_DIMENSION || source.getHeight() > MAX_BLUR_DIMENSION) {
            float maxDim = Math.max(source.getWidth(), source.getHeight());
            scaleFactor = (int) Math.ceil(maxDim / MAX_BLUR_DIMENSION);
            int w = Math.max(1, source.getWidth() / scaleFactor);
            int h = Math.max(1, source.getHeight() / scaleFactor);
            working = Bitmap.createScaledBitmap(source, w, h, true);
            radius = Math.max(1, radius / scaleFactor);
        }

        Bitmap bitmap = working.copy(Bitmap.Config.ARGB_8888, true);
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
        if (working != source) {
            Bitmap scaledBack = Bitmap.createScaledBitmap(bitmap, source.getWidth(), source.getHeight(), true);
            if (scaledBack != bitmap) {
                bitmap.recycle();
                return scaledBack;
            }
        }
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
            case 6:
                return 36;
            case 7:
                return 42;
            case 8:
                return 48;
            case 9:
                return 54;
            case 10:
                return 60;
            default:
                return 18;
        }
    }

    public static int applyOpacity(int color, int opacityPercent) {
        int clamped = Math.max(0, Math.min(opacityPercent, 100));
        int alpha = Math.round(255f * clamped / 100f);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
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
