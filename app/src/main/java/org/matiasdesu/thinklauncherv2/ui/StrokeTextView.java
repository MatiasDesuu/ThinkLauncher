package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class StrokeTextView extends AppCompatTextView {
    private int strokeColor = 0;
    private float strokeWidth = 0;

    public StrokeTextView(Context context) {
        super(context);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setStroke(int color, float width) {
        if (this.strokeColor != color || this.strokeWidth != width) {
            this.strokeColor = color;
            this.strokeWidth = width;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (strokeWidth > 0) {
            // Add extra space for the stroke to avoid clipping on the right/left
            int extra = (int) Math.ceil(strokeWidth * 2);
            setMeasuredDimension(getMeasuredWidth() + extra, getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (strokeWidth > 0) {
            canvas.save();
            // Translate the canvas to center the text a bit more and make room for the left
            // stroke
            canvas.translate(strokeWidth / 2f, 0);

            TextPaint paint = getPaint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            int restoreColor = getCurrentTextColor();
            setTextColor(strokeColor);
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.FILL);
            setTextColor(restoreColor);
            super.onDraw(canvas);
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }
}
