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
                } catch (OutOfMemoryError e) {
                    mShadowBitmap = null;
                }
            }

            if (mShadowBitmap != null) {
                android.graphics.Paint effectPaint = new android.graphics.Paint(
                        android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
                effectPaint.setColorFilter(
                        new android.graphics.PorterDuffColorFilter(mColor, android.graphics.PorterDuff.Mode.SRC_IN));

                if (mEffect == 2) { // Outline (Stroke)
                    for (int i = 0; i < 12; i++) {
                        double angle = i * (Math.PI / 6); // 30 degrees
                        float dx = (float) (Math.cos(angle) * mOffset);
                        float dy = (float) (Math.sin(angle) * mOffset);
                        canvas.drawBitmap(mShadowBitmap, bounds.left + dx, bounds.top + dy, effectPaint);
                    }
                } else if (mEffect == 1) { // Shadow
                    float[][] shadowOffsets = {
                            { mOffset * 0.5f, mOffset * 0.5f },
                            { mOffset * 0.75f, mOffset * 0.75f },
                            { mOffset, mOffset }
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
