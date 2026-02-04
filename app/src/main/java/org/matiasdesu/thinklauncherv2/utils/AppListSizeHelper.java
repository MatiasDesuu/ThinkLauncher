package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.util.DisplayMetrics;

public class AppListSizeHelper {

    public static int calculateItemsPerPage(Context context, int textSize) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);

        // Get display metrics
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float density = dm.density;
        float scaledDensity = dm.scaledDensity;
        float screenHeightDp = dm.heightPixels / density;

        // Subtract navigation bar height if present
        int navBarHeightPx = 0;
        try {
            navBarHeightPx = context.getResources().getDimensionPixelSize(
                context.getResources().getIdentifier("navigation_bar_height", "dimen", "android")
            );
        } catch (Exception e) {
            // Navigation bar height not found, assume 0
        }
        float navBarHeightDp = navBarHeightPx / density;
        screenHeightDp -= navBarHeightDp;

        // Fixed heights in dp
        float topHeightDp = 48; // top layout
        float dividerDp = 4; // divider + bottom divider
        float bottomHeightDp = 48; // bottom bar
        float recyclerHeightDp = screenHeightDp - topHeightDp - dividerDp - bottomHeightDp;

        // Calculate text height accurately
        Paint paint = new Paint();
        paint.setTextSize(textSize * scaledDensity);
        float textHeightPx = paint.getFontMetrics().bottom - paint.getFontMetrics().top;
        float textHeightDp = textHeightPx / density;

        // Item height: text height + margins (20dp) + buffer (10dp for safety)
        float itemHeightDp = textHeightDp + 20;

        int itemsPerPage = (int) (recyclerHeightDp / itemHeightDp);
        if (itemsPerPage < 1) itemsPerPage = 1;

        // Save device density for future calculations
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("device_density", density);
        editor.putFloat("device_scaled_density", scaledDensity);
        editor.apply();

        return itemsPerPage;
    }
}