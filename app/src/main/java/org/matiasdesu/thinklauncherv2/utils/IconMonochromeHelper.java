package org.matiasdesu.thinklauncherv2.utils;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

public class IconMonochromeHelper {

    private static ColorMatrixColorFilter cachedFilter = null;

    public static ColorMatrixColorFilter getMonochromeFilter() {
        if (cachedFilter == null) {
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            cachedFilter = new ColorMatrixColorFilter(colorMatrix);
        }
        return cachedFilter;
    }
}
