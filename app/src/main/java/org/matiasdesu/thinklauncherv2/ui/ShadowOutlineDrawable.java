package org.matiasdesu.thinklauncherv2.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Build;

public class ShadowOutlineDrawable extends Drawable {
    private final Drawable mDrawable;
    private final int mEffect; // 1 = Shadow, 2 = Outline
    private final int mColor;
    private final float mOffset;
    private ColorFilter mColorFilter;
    private Bitmap mShadowBitmap;
    private float mFillScale = 1f;

    public ShadowOutlineDrawable(Drawable drawable, int effect, int color, float offset) {
        mDrawable = drawable;
        mEffect = effect;
        mColor = color;
        mOffset = offset;
    }

    public Drawable getInnerDrawable() {
        return mDrawable;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0)
            return;

        mDrawable.setBounds(bounds);

        if (mEffect != 0) {
            // Prepare Bitmap if needed
            if (mShadowBitmap == null || mShadowBitmap.getWidth() != bounds.width()
                    || mShadowBitmap.getHeight() != bounds.height()) {
                if (mShadowBitmap != null)
                    mShadowBitmap.recycle();
                try {
                    mShadowBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(mShadowBitmap);
                    c.translate(-bounds.left, -bounds.top);
                    mDrawable.draw(c);
                    mFillScale = computeFillScale(mShadowBitmap);
                } catch (OutOfMemoryError e) {
                    mShadowBitmap = null;
                }
            }

            if (mShadowBitmap != null) {
                android.graphics.Paint effectPaint = new android.graphics.Paint(
                        android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
                effectPaint.setColorFilter(
                        new android.graphics.PorterDuffColorFilter(mColor, android.graphics.PorterDuff.Mode.SRC_IN));

                // Scale the effect offset to the size of the actual silhouette so the shadow and
                // outline look proportionally the same on every icon type. Launcher vector glyphs
                // (settings, folder, gallery...) occupy a small part of the view, while app
                // adaptive tiles fill most of it; using the same dp offset on both makes the
                // effect look much heavier on the small glyphs. Applying fill-based scaling makes
                // the gap proportional to the silhouette instead.
                float effOffset = mOffset * mFillScale;
                // Full-bleed icons (app tiles filling the whole slot) can still read a little
                // finer than the denser launcher glyphs, so give them a small boost so apps and
                // system icons end up with the same shadow/outline weight.
                if (mFillScale >= 0.85f) {
                    effOffset = mOffset * 1.25f;
                }

                if (mEffect == 2) { // Outline (Stroke)
                    for (int i = 0; i < 12; i++) {
                        double angle = i * (Math.PI / 6); // 30 degrees
                        float dx = (float) (Math.cos(angle) * effOffset);
                        float dy = (float) (Math.sin(angle) * effOffset);
                        canvas.drawBitmap(mShadowBitmap, bounds.left + dx, bounds.top + dy, effectPaint);
                    }
                } else if (mEffect == 1) { // Shadow
                    float[][] shadowOffsets = {
                            { effOffset * 0.5f, effOffset * 0.5f },
                            { effOffset * 0.75f, effOffset * 0.75f },
                            { effOffset, effOffset }
                    };
                    for (float[] off : shadowOffsets) {
                        canvas.drawBitmap(mShadowBitmap, bounds.left + off[0], bounds.top + off[1], effectPaint);
                    }
                }
            }
        }

        // Draw original drawable exactly as it is, preserved and with its own filters
        // intact
        if (mColorFilter != null) {
            android.graphics.Paint p = new android.graphics.Paint();
            p.setColorFilter(mColorFilter);
            int sc = canvas.saveLayer(new android.graphics.RectF(bounds), p);
            mDrawable.draw(canvas);
            canvas.restoreToCount(sc);
        } else {
            mDrawable.draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mShadowBitmap = null;
        mFillScale = 1f;
    }

    /**
     * Measure how much of the drawable bounds the visible (opaque enough) silhouette
     * occupies, used to scale the shadow/outline offset proportionally to the icon size
     * so the effect looks equally heavy on small launcher glyphs and full app tiles.
     */
    private static float computeFillScale(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= 0 || h <= 0) {
            return 1f;
        }
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            int rowStart = y * w;
            for (int x = 0; x < w; x++) {
                if ((pixels[rowStart + x] >>> 24) > 0x08) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return 1f;
        }

        int bw = maxX - minX + 1;
        int bh = maxY - minY + 1;
        float scale = Math.max(bw, bh) / (float) Math.max(w, h);

        // Same proportional rule for every icon: the effect offset scales with the silhouette
        // size so the shadow/outline gap stays a constant fraction of each icon, making app
        // icons and launcher system icons look identical. The floor only protects very sparse
        // glyphs from losing the effect entirely, so keep it low enough not to inflate the
        // ratio (a raised floor made smaller glyphs look heavier than app tiles).
        if (scale <= 0f) return 1f;
        return Math.max(0.30f, Math.min(1f, scale));
    }

    @Override
    public void setAlpha(int alpha) {
        mDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mColorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isStateful() {
        return mDrawable.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = mDrawable.setState(state);
        if (changed) {
            mShadowBitmap = null;
            invalidateSelf();
        }
        return changed;
    }

    @Override
    public int getIntrinsicWidth() {
        return mDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mDrawable.getIntrinsicHeight();
    }
}
