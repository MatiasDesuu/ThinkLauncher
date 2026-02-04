package org.matiasdesu.thinklauncherv2.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class ColorPickerView extends View {

    private Paint huePaint;
    private Paint satValPaint;
    private Paint pointerPaint;
    private Paint borderPaint;

    private float[] hsv = {1f, 1f, 1f};
    private OnColorChangedListener listener;

    private RectF hueRect = new RectF();
    private RectF satValRect = new RectF();

    private float hueHandleRadius = 20f;
    private float satValHandleRadius = 20f;

    private boolean isDraggingHue = false;
    private boolean isDraggingSatVal = false;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        satValPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointerPaint.setStyle(Paint.Style.STROKE);
        pointerPaint.setStrokeWidth(5f);
        pointerPaint.setColor(Color.BLACK);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(getContext().getResources().getDisplayMetrics().density * 2);
        borderPaint.setColor(Color.BLACK); // Será actualizado dinámicamente
    }

    public void setBorderColor(int color) {
        borderPaint.setColor(color);
        invalidate();
    }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 40f;
        float hueHeight = 60f;
        
        hueRect.set(padding, padding, w - padding, padding + hueHeight);
        satValRect.set(padding, hueRect.bottom + padding, w - padding, h - padding);

        int[] colors = new int[361];
        for (int i = 0; i <= 360; i++) {
            colors[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
        }
        huePaint.setShader(new LinearGradient(hueRect.left, 0, hueRect.right, 0, colors, null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw Hue bar and border
        canvas.drawRect(hueRect, huePaint);
        canvas.drawRect(hueRect, borderPaint);

        float hueX = hueRect.left + (hsv[0] / 360f) * hueRect.width();
        pointerPaint.setColor(Color.BLACK);
        canvas.drawCircle(hueX, hueRect.centerY(), hueHandleRadius, pointerPaint);

        // Draw Saturation/Value square and border
        updateSatValShader();
        canvas.drawRect(satValRect, satValPaint);
        canvas.drawRect(satValRect, borderPaint);

        float satX = satValRect.left + hsv[1] * satValRect.width();
        float valY = satValRect.bottom - hsv[2] * satValRect.height();
        
        pointerPaint.setColor(hsv[2] > 0.5f ? Color.BLACK : Color.WHITE);
        canvas.drawCircle(satX, valY, satValHandleRadius, pointerPaint);
    }

    private void updateSatValShader() {
        int color = Color.HSVToColor(new float[]{hsv[0], 1f, 1f});
        
        // Shader para la saturación (Blanco a Color puro)
        Shader satShader = new LinearGradient(satValRect.left, 0, satValRect.right, 0, Color.WHITE, color, Shader.TileMode.CLAMP);
        // Shader para el valor/brillo (Transparente a Negro, vertical)
        Shader valShader = new LinearGradient(0, satValRect.top, 0, satValRect.bottom, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
        
        // Combinamos ambos para obtener el cuadrado completo de Saturation/Value
        satValPaint.setShader(new android.graphics.ComposeShader(satShader, valShader, android.graphics.PorterDuff.Mode.SRC_OVER));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (hueRect.contains(x, y)) {
                    isDraggingHue = true;
                    updateHue(x);
                } else if (satValRect.contains(x, y)) {
                    isDraggingSatVal = true;
                    updateSatVal(x, y);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDraggingHue) {
                    updateHue(x);
                } else if (isDraggingSatVal) {
                    updateSatVal(x, y);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDraggingHue = false;
                isDraggingSatVal = false;
                break;
        }

        invalidate();
        if (listener != null) {
            listener.onColorChanged(getColor());
        }
        return true;
    }

    private void updateHue(float x) {
        hsv[0] = Math.max(0, Math.min(360, ((x - hueRect.left) / hueRect.width()) * 360f));
    }

    private void updateSatVal(float x, float y) {
        hsv[1] = Math.max(0, Math.min(1f, (x - satValRect.left) / satValRect.width()));
        hsv[2] = Math.max(0, Math.min(1f, 1f - (y - satValRect.top) / satValRect.height()));
    }
}
