package org.matiasdesu.thinklauncherv2.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private static final float MAX_SCALE = 4.0f;

    private Matrix matrix;
    private int viewWidth;
    private int viewHeight;
    private float currentScale = 1.0f;
    private float minScale = 1.0f;

    private PointF lastTouch = new PointF();
    private float lastPinchDist = 0;
    private boolean isPinching = false;
    private boolean isDragging = false;

    private OnSwipeListener onSwipeListener;
    private float downX;
    private float downY;
    private boolean couldBeFling;

    private long lastTapTime = 0;
    private float lastTapX = 0;
    private float lastTapY = 0;
    private int doubleTapSlop;

    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    public ZoomableImageView(Context context) {
        super(context);
        init();
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.onSwipeListener = listener;
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        matrix = new Matrix();
        doubleTapSlop = ViewConfiguration.get(getContext()).getScaledDoubleTapSlop();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        resetZoom();
    }

    public void resetZoom() {
        Drawable d = getDrawable();
        if (d == null || viewWidth == 0 || viewHeight == 0) return;

        float drawableWidth = d.getIntrinsicWidth();
        float drawableHeight = d.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) return;

        float scaleX = (float) viewWidth / drawableWidth;
        float scaleY = (float) viewHeight / drawableHeight;
        minScale = Math.min(scaleX, scaleY);
        currentScale = minScale;

        float dx = (viewWidth - drawableWidth * minScale) / 2f;
        float dy = (viewHeight - drawableHeight * minScale) / 2f;

        matrix.reset();
        matrix.setScale(minScale, minScale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null) return false;

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(event.getX(), event.getY());
                isDragging = false;
                couldBeFling = true;
                downX = event.getX();
                downY = event.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    isPinching = true;
                    isDragging = false;
                    couldBeFling = false;
                    float dx = event.getX(0) - event.getX(1);
                    float dy = event.getY(0) - event.getY(1);
                    lastPinchDist = (float) Math.sqrt(dx * dx + dy * dy);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isPinching && event.getPointerCount() >= 2) {
                    float dx = event.getX(0) - event.getX(1);
                    float dy = event.getY(0) - event.getY(1);
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (lastPinchDist > 0) {
                        float ratio = dist / lastPinchDist;
                        float newScale = currentScale * ratio;
                        newScale = Math.max(minScale, Math.min(MAX_SCALE, newScale));

                        float focusX = (event.getX(0) + event.getX(1)) / 2f;
                        float focusY = (event.getY(0) + event.getY(1)) / 2f;

                        float scaleFactor = newScale / currentScale;
                        matrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                        currentScale = newScale;
                        constrainMatrix();
                        setImageMatrix(matrix);
                    }
                    lastPinchDist = dist;
                } else if (!isPinching) {
                    float x = event.getX();
                    float y = event.getY();
                    float dx = x - lastTouch.x;
                    float dy = y - lastTouch.y;

                    if (currentScale > minScale) {
                        if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                            isDragging = true;
                        }
                        matrix.postTranslate(dx, dy);
                        constrainMatrix();
                        setImageMatrix(matrix);
                    } else {
                        if (Math.abs(x - downX) > doubleTapSlop) {
                            isDragging = true;
                        }
                    }

                    lastTouch.set(x, y);
                }
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = event.getActionIndex();
                int remainingCount = event.getPointerCount() - 1;

                if (isPinching && remainingCount < 2) {
                    isPinching = false;
                    if (remainingCount == 1) {
                        int otherIndex = (pointerIndex == 0) ? 1 : 0;
                        lastTouch.set(event.getX(otherIndex), event.getY(otherIndex));
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float upX = event.getX();
                float upY = event.getY();

                if (!isPinching) {
                    long now = System.currentTimeMillis();

                    if (!isDragging) {
                        float dxTap = upX - lastTapX;
                        float dyTap = upY - lastTapY;
                        float thisMoveX = upX - downX;
                        float thisMoveY = upY - downY;
                        boolean isDoubleTap = (now - lastTapTime) < 300
                                && Math.abs(dxTap) < doubleTapSlop
                                && Math.abs(dyTap) < doubleTapSlop
                                && Math.abs(thisMoveX) < doubleTapSlop
                                && Math.abs(thisMoveY) < doubleTapSlop;

                        if (isDoubleTap) {
                            handleDoubleTap(upX, upY);
                            lastTapTime = 0;
                        } else {
                            lastTapTime = now;
                            lastTapX = upX;
                            lastTapY = upY;
                        }
                    } else if (onSwipeListener != null && currentScale <= minScale) {
                        float flingDist = upX - downX;
                        if (Math.abs(flingDist) > 80) {
                            if (flingDist < 0) {
                                onSwipeListener.onSwipeLeft();
                            } else {
                                onSwipeListener.onSwipeRight();
                            }
                        }
                    }
                }

                isPinching = false;
                isDragging = false;
                couldBeFling = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void handleDoubleTap(float x, float y) {
        if (currentScale > minScale) {
            resetZoom();
        } else {
            float targetScale = Math.min(MAX_SCALE / 2f, minScale * 3f);
            float factor = targetScale / currentScale;
            matrix.postScale(factor, factor, x, y);
            currentScale = targetScale;
            constrainMatrix();
            setImageMatrix(matrix);
        }
    }

    private void constrainMatrix() {
        Drawable d = getDrawable();
        if (d == null || viewWidth == 0 || viewHeight == 0) return;

        float[] values = new float[9];
        matrix.getValues(values);
        float scaleX = values[Matrix.MSCALE_X];
        float translateX = values[Matrix.MTRANS_X];

        float drawableWidth = d.getIntrinsicWidth() * scaleX;
        float dx = 0;
        if (drawableWidth <= viewWidth) {
            dx = (viewWidth - drawableWidth) / 2f - translateX;
        } else {
            if (translateX > 0) dx = -translateX;
            if (translateX + drawableWidth < viewWidth) dx = viewWidth - translateX - drawableWidth;
        }

        float translateY = values[Matrix.MTRANS_Y];
        float drawableHeight = d.getIntrinsicHeight() * scaleX;
        float dy = 0;
        if (drawableHeight <= viewHeight) {
            dy = (viewHeight - drawableHeight) / 2f - translateY;
        } else {
            if (translateY > 0) dy = -translateY;
            if (translateY + drawableHeight < viewHeight) dy = viewHeight - translateY - drawableHeight;
        }

        if (dx != 0 || dy != 0) {
            matrix.postTranslate(dx, dy);
        }
    }

    public float getCurrentScale() {
        return currentScale;
    }
}
