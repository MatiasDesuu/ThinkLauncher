package org.matiasdesu.thinklauncherv2.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 4.0f;

    private Matrix matrix;
    private Matrix savedMatrix;
    private int viewWidth;
    private int viewHeight;
    private float currentScale = 1.0f;
    private float minScale = 1.0f;
    private PointF lastTouch = new PointF();
    private PointF mid = new PointF();
    private int mode = NONE;

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private Scroller scroller;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        matrix = new Matrix();
        savedMatrix = new Matrix();
        scroller = new Scroller(context, new LinearInterpolator());

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScale = currentScale * scaleFactor;
                if (newScale >= minScale && newScale <= MAX_SCALE) {
                    matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                    currentScale = newScale;
                    constrainMatrix();
                    setImageMatrix(matrix);
                }
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale > minScale) {
                    resetZoom();
                } else {
                    float targetScale = MAX_SCALE / 2f;
                    float factor = targetScale / currentScale;
                    matrix.postScale(factor, factor, e.getX(), e.getY());
                    currentScale = targetScale;
                    constrainMatrix();
                    setImageMatrix(matrix);
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (currentScale > minScale) {
                    matrix.postTranslate(-distanceX, -distanceY);
                    constrainMatrix();
                    setImageMatrix(matrix);
                }
                return true;
            }
        });

        setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            if (!scaleDetector.isInProgress()) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        lastTouch.set(event.getX(), event.getY());
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mode = NONE;
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG && !scaleDetector.isInProgress()) {
                            float dx = event.getX() - lastTouch.x;
                            float dy = event.getY() - lastTouch.y;
                            matrix.set(savedMatrix);
                            matrix.postTranslate(dx, dy);
                            constrainMatrix();
                            setImageMatrix(matrix);
                        }
                        break;
                }
            }
            return true;
        });
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

        float scaledWidth = drawableWidth * minScale;
        float scaledHeight = drawableHeight * minScale;

        float dx = (viewWidth - scaledWidth) / 2f;
        float dy = (viewHeight - scaledHeight) / 2f;

        matrix.reset();
        matrix.setScale(minScale, minScale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
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

        matrix.postTranslate(dx, dy);
    }

    public float getCurrentScale() {
        return currentScale;
    }
}
