package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

public class EinkRefreshHelper {

    /**
     * Triggers an E-Ink refresh by flashing an overlay on the given Window's decorView.
     * @param window The Window to add the overlay to (covers entire screen including system bars)
     * @param prefs SharedPreferences instance for theme and refresh settings
     * @param delayMs How long the overlay should be visible (ms)
     */
    public static void refreshEink(android.view.Window window, SharedPreferences prefs, int delayMs) {
        if (prefs.getInt("eink_refresh_enabled", 0) == 0) return;

        // Ensure UI operations are performed on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            int theme = prefs.getInt("theme", 0); // 0 light, 1 dark
            boolean isDark = (theme == 1);
            int overlayColor = isDark ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
            View overlay = new View(window.getContext());
            overlay.setBackgroundColor(overlayColor);
            overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            ViewGroup parent = (ViewGroup) window.getDecorView();
            if (parent != null) {
                parent.addView(overlay);
                overlay.setElevation(Float.MAX_VALUE);
                overlay.bringToFront();
                parent.invalidate();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    parent.removeView(overlay);
                }, delayMs);
            }
        });
    }

    /**
     * Forces an E-Ink refresh by flashing an overlay, bypassing the einkRefreshEnabled preference.
     * This is useful for gesture-triggered refreshes that should work independently of the global setting.
     * @param window The Window to add the overlay to (covers entire screen including system bars)
     * @param prefs SharedPreferences instance for theme resolution
     * @param delayMs How long the overlay should be visible (ms)
     */
    public static void refreshEinkForced(android.view.Window window, SharedPreferences prefs, int delayMs) {
        // Ensure UI operations are performed on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            int theme = prefs.getInt("theme", 0); // 0 light, 1 dark
            boolean isDark = (theme == 1);
            int overlayColor = isDark ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
            View overlay = new View(window.getContext());
            overlay.setBackgroundColor(overlayColor);
            overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            ViewGroup parent = (ViewGroup) window.getDecorView();
            if (parent != null) {
                parent.addView(overlay);
                overlay.setElevation(Float.MAX_VALUE);
                overlay.bringToFront();
                parent.invalidate();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    parent.removeView(overlay);
                }, delayMs);
            }
        });
    }
}