package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.util.DisplayMetrics;

/**
 * Helper class to calculate items per page for Settings screens.
 * Settings items have different spacing and font sizes compared to app lists.
 */
public class SettingsListSizeHelper {

    /**
     * Calculate how many setting items fit in one page.
     * Settings items use 18sp text, 24dp icons, and 16dp margins.
     * 
     * @param context The application context
     * @return Number of setting items that fit in one page
     */
    public static int calculateItemsPerPage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float density = dm.density;
        float scaledDensity = dm.scaledDensity;
        float screenHeightDp = dm.heightPixels / density;

        int navBarHeightPx = 0;
        try {
            navBarHeightPx = context.getResources().getDimensionPixelSize(
                context.getResources().getIdentifier("navigation_bar_height", "dimen", "android")
            );
        } catch (Exception e) {
        }
        float navBarHeightDp = navBarHeightPx / density;
        screenHeightDp -= navBarHeightDp;

        float topHeightDp = 48; 
        float dividerDp = 4;
        float bottomHeightDp = 48;
        float containerHeightDp = screenHeightDp - topHeightDp - dividerDp - bottomHeightDp;

        
        float itemHeightDp = 74;

        int itemsPerPage = (int) Math.floor(containerHeightDp / itemHeightDp);
        if (itemsPerPage < 1) itemsPerPage = 1;

        return itemsPerPage;
    }
}
