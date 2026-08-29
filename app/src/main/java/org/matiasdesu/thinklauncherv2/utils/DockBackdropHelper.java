package org.matiasdesu.thinklauncherv2.utils;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class DockBackdropHelper {

    private static final long RETRY_DELAY_MS = 100L;
    private static final int MAX_ATTEMPTS = 50;
    private static final int EXPAND = 64;

    private static final ExecutorService CAPTURE_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "TL-DockBackdrop");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    });

    private static final class Params {
        final ViewGroup parent;
        final SharedPreferences prefs;
        final String prefix;
        final int surfaceColor;
        final boolean borderEnabled;
        final int borderColor;
        final float borderWidthPx;
        final float cornerRadiusPx;

        Params(ViewGroup parent, SharedPreferences prefs, String prefix, int surfaceColor,
                boolean borderEnabled, int borderColor, float borderWidthPx, float cornerRadiusPx) {
            this.parent = parent;
            this.prefs = prefs;
            this.prefix = prefix;
            this.surfaceColor = surfaceColor;
            this.borderEnabled = borderEnabled;
            this.borderColor = borderColor;
            this.borderWidthPx = borderWidthPx;
            this.cornerRadiusPx = cornerRadiusPx;
        }
    }

    private static final Map<View, Params> BACKDROPS = Collections.synchronizedMap(new WeakHashMap<>());

    private DockBackdropHelper() {
    }

    public static void applyBackdrop(View dockView, ViewGroup parent, SharedPreferences prefs, String prefix,
            int surfaceColor, boolean borderEnabled, int borderColor, float borderWidthPx, float cornerRadiusPx) {
        if (dockView == null || parent == null) {
            return;
        }
        BACKDROPS.put(dockView, new Params(parent, prefs, prefix, surfaceColor, borderEnabled, borderColor,
                borderWidthPx, cornerRadiusPx));
        recapture(dockView, 0);
    }

    public static void reapply(View dockView) {
        Params params = BACKDROPS.get(dockView);
        if (params != null) {
            recapture(dockView, 0);
        }
    }

    public static void reapplyAll(ViewGroup parent) {
        List<View> views;
        synchronized (BACKDROPS) {
            views = new ArrayList<>(BACKDROPS.keySet());
        }
        for (View view : views) {
            if (view.getParent() == parent) {
                recapture(view, 0);
            }
        }
    }

    private static void recapture(final View dockView, final int attempt) {
        final Params params = BACKDROPS.get(dockView);
        if (params == null || attempt >= MAX_ATTEMPTS) {
            return;
        }
        if (attempt == 0) {
            dockView.post(() -> tryCapture(dockView, params, attempt));
        } else {
            dockView.postDelayed(() -> tryCapture(dockView, params, attempt), RETRY_DELAY_MS);
        }
    }

    private static void tryCapture(final View dockView, final Params params, final int attempt) {
        if (dockView.getParent() != params.parent) {
            return;
        }
        if (params.prefs.getInt(params.prefix + "_backdrop_opacity", 0) != 1) {
            return;
        }
        if (params.parent.getVisibility() != View.VISIBLE || dockView.getVisibility() != View.VISIBLE) {
            recapture(dockView, attempt + 1);
            return;
        }

        final int left = dockView.getLeft();
        final int top = dockView.getTop();
        final int width = dockView.getWidth();
        final int height = dockView.getHeight();
        if (width <= 0 || height <= 0) {
            recapture(dockView, attempt + 1);
            return;
        }

        int minPad = Math.round(dockView.getResources().getDisplayMetrics().density * 6f);
        if (dockView.getPaddingLeft() < minPad) {
            dockView.setPadding(minPad, minPad, minPad, minPad);
            if (dockView instanceof ViewGroup) {
                ((ViewGroup) dockView).setClipToPadding(true);
            }
        }

        final boolean blurEnabled = params.prefs.getInt(params.prefix + "_backdrop_blur", 0) == 1;
        final int opacityPercent = params.prefs.getInt("app_launcher_bg_opacity", 100);
        final int blurStrength = params.prefs.getInt("app_launcher_bg_blur_strength", 3);
        final int rootWidth = params.parent.getWidth();
        final int rootHeight = params.parent.getHeight();

        CAPTURE_EXECUTOR.execute(() -> {
            Bitmap wallpaper = WallpaperHelper.getWallpaperForScreenCached(dockView.getContext(), rootWidth,
                    rootHeight, blurEnabled, blurStrength);
            final Drawable backdrop;
            if (wallpaper == null) {
                backdrop = buildSolidBackground(params, opacityPercent);
            } else {
                backdrop = buildWallpaperBackdrop(wallpaper, left, top, width, height,
                        rootWidth, rootHeight, params, opacityPercent);
            }
            dockView.post(() -> {
                if (dockView.getParent() == params.parent) {
                    dockView.setBackground(backdrop);
                }
            });
        });
    }

    private static GradientDrawable buildSolidBackground(Params params, int opacityPercent) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        if (params.borderEnabled && params.borderWidthPx > 0f) {
            gd.setStroke(Math.max(1, Math.round(params.borderWidthPx)), params.borderColor);
        }
        gd.setColor(WallpaperHelper.applyOpacity(params.surfaceColor, opacityPercent));
        gd.setCornerRadius(params.cornerRadiusPx);
        return gd;
    }

    private static Drawable buildWallpaperBackdrop(Bitmap wallpaper, int left, int top, int width, int height,
            int rootWidth, int rootHeight, Params params, int opacityPercent) {
        int bitmapWidth = width + 2 * EXPAND;
        int bitmapHeight = height + 2 * EXPAND;

        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float drawScale = Math.max(rootWidth / (float) wallpaper.getWidth(),
                rootHeight / (float) wallpaper.getHeight());
        float drawWidth = wallpaper.getWidth() * drawScale;
        float drawHeight = wallpaper.getHeight() * drawScale;
        float drawLeft = (rootWidth - drawWidth) / 2f;
        float drawTop = (rootHeight - drawHeight) / 2f;

        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postScale(drawScale, drawScale);
        matrix.postTranslate(drawLeft + EXPAND - left, drawTop + EXPAND - top);
        canvas.drawBitmap(wallpaper, matrix, null);

        canvas.drawColor(WallpaperHelper.applyOpacity(params.surfaceColor, opacityPercent));

        if (params.borderEnabled && params.borderWidthPx > 0f) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(params.borderWidthPx);
            paint.setColor(params.borderColor);
            float inset = EXPAND + params.borderWidthPx / 2f;
            canvas.drawRoundRect(inset, inset, inset + width - params.borderWidthPx,
                    inset + height - params.borderWidthPx, params.cornerRadiusPx, params.cornerRadiusPx, paint);
        }

        return new AlignedBitmapDrawable(bitmap, EXPAND, params.cornerRadiusPx);
    }

    private static final class AlignedBitmapDrawable extends Drawable {

        private final Bitmap bitmap;
        private final int offsetX;
        private final int offsetY;
        private final float cornerRadius;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Path clipPath = new Path();

        AlignedBitmapDrawable(Bitmap bitmap, int offset, float cornerRadius) {
            this.bitmap = bitmap;
            this.offsetX = offset;
            this.offsetY = offset;
            this.cornerRadius = cornerRadius;
        }

        @Override
        public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            clipPath.rewind();
            clipPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clipPath);
            canvas.drawBitmap(bitmap, bounds.left - offsetX, bounds.top - offsetY, paint);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }
}
