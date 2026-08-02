package org.matiasdesu.thinklauncherv2.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class AppBarPositionView extends View {

    public interface OnPositionSelectedListener {
        void onPositionSelected(int position, String label);
    }

    // Cell -> position id mapping
    private static final int[][] CELL_TO_POSITION = {
            { 0, 6, 1 },
            { 4, 8, 5 },
            { 2, 7, 3 }
    };

    private static final String[] POSITION_LABELS = {
            "Top Left", "Top Right", "Bottom Left", "Bottom Right",
            "Center Left", "Center Right", "Top Center", "Bottom Center", "Center"
    };

    private int theme;
    private int selectedPosition = 0;
    private Paint borderPaint;
    private Paint fillPaint;
    private Paint detailPaint;
    private OnPositionSelectedListener listener;
    private RectF tempRect = new RectF();
    private RectF gridRect = new RectF();

    public AppBarPositionView(Context context) {
        super(context);
        init(context);
    }

    public AppBarPositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AppBarPositionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        theme = context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("theme", 0);
        int textColor = ThemeUtils.getTextColor(theme, context);
        int bgColor = ThemeUtils.getBgColor(theme, context);

        setClickable(true);
        setFocusable(true);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        borderPaint.setColor(textColor);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(textColor);

        detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        detailPaint.setStyle(Paint.Style.STROKE);
        detailPaint.setStrokeWidth(3);
        detailPaint.setColor(textColor);
    }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= 9) return;
        this.selectedPosition = position;
        invalidate();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setOnPositionSelectedListener(OnPositionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float side = Math.min(w, h);
        float left = (w - side) / 2;
        float top = (h - side) / 2;
        float right = left + side;
        float bottom = top + side;

        gridRect.set(left, top, right, bottom);

        float cellW = side / 3f;
        float cellH = side / 3f;

        // Outer border
        canvas.drawRect(gridRect, borderPaint);

        // Draw dividers
        for (int i = 1; i < 3; i++) {
            canvas.drawLine(left + i * cellW, top, left + i * cellW, bottom, detailPaint);
            canvas.drawLine(left, top + i * cellH, right, top + i * cellH, detailPaint);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int position = CELL_TO_POSITION[row][col];
                float cx = left + col * cellW + cellW / 2f;
                float cy = top + row * cellH + cellH / 2f;
                boolean selected = (position == selectedPosition);
                float size = Math.min(cellW, cellH) * 0.5f;
                float leftPos = cx - size / 2f;
                float topPos = cy - size / 2f;
                RectF r = new RectF(leftPos, topPos, leftPos + size, topPos + size);

                if (selected) {
                    canvas.drawRect(r, fillPaint);
                } else {
                    canvas.drawRect(r, detailPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            if (gridRect.contains(x, y)) {
                float w = gridRect.width();
                float cellW = w / 3f;
                float cellH = gridRect.height() / 3f;
                int col = (int) ((x - gridRect.left) / cellW);
                int row = (int) ((y - gridRect.top) / cellH);
                if (col < 0) col = 0;
                if (col > 2) col = 2;
                if (row < 0) row = 0;
                if (row > 2) row = 2;

                int position = CELL_TO_POSITION[row][col];
                selectedPosition = position;
                invalidate();
                if (listener != null) {
                    listener.onPositionSelected(position, POSITION_LABELS[position]);
                }
            }
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}