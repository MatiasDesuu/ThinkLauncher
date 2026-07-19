package org.matiasdesu.thinklauncherv2.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class WallpaperPositionView extends View {

    private Bitmap wallpaperBitmap;
    private Paint borderPaint;
    private Paint backgroundPaint;
    private Paint previewPaint;
    private Paint indicatorPaint;
    private RectF screenRect;
    private android.graphics.Rect srcRectInt;
    
    private float offsetX = 0.5f; // 0.0 = left, 1.0 = right
    private float offsetY = 0.5f; // 0.0 = top, 1.0 = bottom
    private float scale = 1f;
    private float screenAspectRatio;
    private OnPositionChangedListener listener;
    private float lastTouchX;
    private float lastTouchY;
    private float lastPinchDist;
    private boolean isPinching;

    public interface OnPositionChangedListener {
        void onPositionChanged(float offsetX, float offsetY);
        void onPositionChangeFinished(float offsetX, float offsetY);
        void onScaleChanged(float scale);
    }

    public WallpaperPositionView(Context context) {
        super(context);
        init(context);
    }

    public WallpaperPositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WallpaperPositionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Get screen aspect ratio
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenAspectRatio = (float) metrics.heightPixels / metrics.widthPixels;

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewPaint.setFilterBitmap(true);
        
        indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setStyle(Paint.Style.STROKE);
        indicatorPaint.setStrokeWidth(3);

        // Apply theme colors
        int theme = context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("theme", 0);
        int textColor = ThemeUtils.getTextColor(theme, context);
        int bgColor = ThemeUtils.getBgColor(theme, context);
        
        borderPaint.setColor(textColor);
        backgroundPaint.setColor(bgColor);
        indicatorPaint.setColor(textColor);

        screenRect = new RectF();
        srcRectInt = new android.graphics.Rect();
    }

    public void setWallpaperBitmap(Bitmap bitmap) {
        this.wallpaperBitmap = bitmap;
        invalidate();
    }

    public void setPosition(float x, float y) {
        this.offsetX = Math.max(0, Math.min(1, x));
        this.offsetY = Math.max(0, Math.min(1, y));
        invalidate();
    }

    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }

    public void setScale(float scale) {
        this.scale = Math.max(1f, scale);
        invalidate();
    }

    public float getScale() { return scale; }

    public void setOnPositionChangedListener(OnPositionChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth == 0 || viewHeight == 0) return;

        // Calculate screen preview rectangle (maintaining aspect ratio)
        float previewHeight = viewHeight - 20;
        float previewWidth = previewHeight / screenAspectRatio;
        
        if (previewWidth > viewWidth - 20) {
            previewWidth = viewWidth - 20;
            previewHeight = previewWidth * screenAspectRatio;
        }

        float left = (viewWidth - previewWidth) / 2;
        float top = (viewHeight - previewHeight) / 2;
        screenRect.set(left, top, left + previewWidth, top + previewHeight);

        // Draw background
        canvas.drawRect(screenRect, backgroundPaint);

        // Draw wallpaper preview if available
        if (wallpaperBitmap != null && !wallpaperBitmap.isRecycled()) {
            drawWallpaperPreview(canvas, previewWidth, previewHeight);
        }

        // Draw border
        canvas.drawRect(screenRect, borderPaint);
    }

    private void drawWallpaperPreview(Canvas canvas, float previewWidth, float previewHeight) {
        float bitmapWidth = wallpaperBitmap.getWidth();
        float bitmapHeight = wallpaperBitmap.getHeight();
        
        float screenRatio = previewWidth / previewHeight;
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

        // Update rectangles
        srcRectInt.set((int)srcLeft, (int)srcTop, (int)(srcLeft + srcWidth), (int)(srcTop + srcHeight));
        
        canvas.save();
        canvas.clipRect(screenRect);
        canvas.drawBitmap(wallpaperBitmap, srcRectInt, screenRect, previewPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (wallpaperBitmap == null) return false;

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isPinching = false;
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                if (event.getPointerCount() >= 2) {
                    isPinching = true;
                    float dx = event.getX(0) - event.getX(1);
                    float dy = event.getY(0) - event.getY(1);
                    lastPinchDist = (float) Math.sqrt(dx * dx + dy * dy);
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isPinching && event.getPointerCount() >= 2) {
                    float dx = event.getX(0) - event.getX(1);
                    float dy = event.getY(0) - event.getY(1);
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (lastPinchDist > 0) {
                        float ratio = dist / lastPinchDist;
                        float newScale = Math.max(1f, Math.min(3f, scale * ratio));
                        if (newScale != scale) {
                            scale = newScale;
                            invalidate();
                            if (listener != null) {
                                listener.onScaleChanged(scale);
                            }
                        }
                    }
                    lastPinchDist = dist;
                    return true;
                }

                if (!isPinching) {
                    float x = event.getX();
                    float y = event.getY();
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    float bitmapWidth = wallpaperBitmap.getWidth();
                    float bitmapHeight = wallpaperBitmap.getHeight();
                    float previewWidth = screenRect.width();
                    float previewHeight = screenRect.height();

                    float screenRatio = previewWidth / previewHeight;
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

                    float newOffsetX = offsetX;
                    float newOffsetY = offsetY;

                    float movableWidth = bitmapWidth - srcWidth;
                    if (movableWidth > 0) {
                        float deltaX = (dx * srcWidth) / (previewWidth * movableWidth);
                        newOffsetX = Math.max(0, Math.min(1, offsetX - deltaX));
                    }

                    float movableHeight = bitmapHeight - srcHeight;
                    if (movableHeight > 0) {
                        float deltaY = (dy * srcHeight) / (previewHeight * movableHeight);
                        newOffsetY = Math.max(0, Math.min(1, offsetY - deltaY));
                    }

                    if (newOffsetX != offsetX || newOffsetY != offsetY) {
                        offsetX = newOffsetX;
                        offsetY = newOffsetY;
                        invalidate();

                        if (listener != null) {
                            listener.onPositionChanged(offsetX, offsetY);
                        }
                    }

                    lastTouchX = x;
                    lastTouchY = y;
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int remainingCount = event.getPointerCount() - 1;

                if (isPinching && remainingCount < 2) {
                    isPinching = false;
                    // Transfer drag tracking to the remaining finger
                    if (remainingCount == 1) {
                        int otherIndex = (pointerIndex == 0) ? 1 : 0;
                        lastTouchX = event.getX(otherIndex);
                        lastTouchY = event.getY(otherIndex);
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (listener != null) {
                    listener.onPositionChangeFinished(offsetX, offsetY);
                }
                isPinching = false;
                performClick();
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
