package org.matiasdesu.thinklauncherv2.utils;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

public class IconMonochromeHelper {

    public static ColorMatrixColorFilter getMonochromeFilter() {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        return new ColorMatrixColorFilter(colorMatrix);
    }
}