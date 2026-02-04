package org.matiasdesu.thinklauncherv2.utils;

import android.graphics.Paint;
import android.widget.TextView;

public class TextWidthHelper {

    public static int getMaxTextWidthPx(TextView tv, String[] texts) {
        Paint paint = new Paint();
        paint.setTextSize(tv.getTextSize());
        float maxWidth = 0;
        for (String text : texts) {
            float w = paint.measureText(text);
            if (w > maxWidth) maxWidth = w;
        }
        return (int) Math.ceil(maxWidth);
    }
}