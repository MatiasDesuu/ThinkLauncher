package org.matiasdesu.thinklauncherv2.utils;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

public class EinkRefreshHelper {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final String OVERLAY_TAG = "tl_eink_overlay";

    private EinkRefreshHelper() {
    }

    private static void flashOverlay(android.view.Window window, SharedPreferences prefs, int delayMs) {
        final int theme = prefs.getInt("theme", 0);
        boolean isDark = ThemeUtils.isDarkTheme(theme);
        final int overlayColor = isDark ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
        final ViewGroup parent = (ViewGroup) window.getDecorView();
        if (parent == null) {
            return;
        }

        // Remove any overlay still pending from a previous refresh so rapid
        // refreshes (e.g. focus + preference change) never stack full-screen
        // views on top of each other.
        removeExistingOverlays(parent);

        View overlay = new View(window.getContext());
        overlay.setTag(OVERLAY_TAG);
        overlay.setBackgroundColor(overlayColor);
        overlay.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        parent.addView(overlay);
        overlay.setElevation(Float.MAX_VALUE);
        overlay.bringToFront();
        parent.invalidate();
        MAIN_HANDLER.postDelayed(() -> {
            removeExistingOverlays(parent);
        }, Math.max(delayMs, 1));
    }

    private static void removeExistingOverlays(ViewGroup parent) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            if (child != null && OVERLAY_TAG.equals(child.getTag())) {
                parent.removeView(child);
            }
        }
    }

    /**
     * Triggers an E-Ink refresh by flashing an overlay on the given Window's decorView.
     * @param window The Window to add the overlay to (covers entire screen including system bars)
     * @param prefs SharedPreferences instance for theme and refresh settings
     * @param delayMs How long the overlay should be visible (ms)
     */
    public static void refreshEink(android.view.Window window, SharedPreferences prefs, int delayMs) {
        if (prefs.getInt("eink_refresh_enabled", 0) == 0) return;
        // Ensure UI operations are performed on the main thread
        MAIN_HANDLER.post(() -> flashOverlay(window, prefs, delayMs));
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
        MAIN_HANDLER.post(() -> flashOverlay(window, prefs, delayMs));
    }
}
