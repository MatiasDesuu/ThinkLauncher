package org.matiasdesu.thinklauncherv2.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.WallpaperHelper;

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
    private int screenWidth;
    private int screenHeight;
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
        // Use the real display size (same area the home screen renders into,
        // including system bar zones) so the preview matches the home 1:1
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int[] realDims = getRealScreenDimensions(wm);
        screenWidth = realDims[0];
        screenHeight = realDims[1];
        screenAspectRatio = (float) screenHeight / screenWidth;

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

    private static int[] getRealScreenDimensions(WindowManager wm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Rect bounds = wm.getCurrentWindowMetrics().getBounds();
            return new int[]{bounds.width(), bounds.height()};
        }
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        return new int[]{metrics.widthPixels, metrics.heightPixels};
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
        // Crop against the real screen dimensions using the exact same math
        // as the home screen renderer (WallpaperHelper), so what the user
        // sees here is exactly what gets drawn on the home
        WallpaperHelper.computeCropSrcRect(wallpaperBitmap, screenWidth, screenHeight,
                offsetX, offsetY, scale, srcRectInt);

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

                    // Same crop window as the renderer, so panning speed
                    // matches what the home screen will show
                    WallpaperHelper.computeCropSrcRect(wallpaperBitmap, screenWidth, screenHeight,
                            offsetX, offsetY, scale, srcRectInt);
                    float srcWidth = srcRectInt.width();
                    float srcHeight = srcRectInt.height();
                    float previewWidth = screenRect.width();
                    float previewHeight = screenRect.height();

                    float newOffsetX = offsetX;
                    float newOffsetY = offsetY;

                    float movableWidth = wallpaperBitmap.getWidth() - srcWidth;
                    if (movableWidth > 0) {
                        float deltaX = (dx * srcWidth) / (previewWidth * movableWidth);
                        newOffsetX = Math.max(0, Math.min(1, offsetX - deltaX));
                    }

                    float movableHeight = wallpaperBitmap.getHeight() - srcHeight;
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
