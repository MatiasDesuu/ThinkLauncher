package org.matiasdesu.thinklauncherv2.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

public final class DialogEffectHelper {

    private DialogEffectHelper() {
    }

    public static int setup(Dialog dialog, int theme) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            applyDialogBlur(window, dialog.getContext());
        }
        return getSurfaceColor(dialog.getContext(), theme);
    }

    public static void applySurface(View view, int theme, Context context, int surfaceColor) {
        if (view == null) {
            return;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(surfaceColor);
        drawable.setStroke((int) (2 * context.getResources().getDisplayMetrics().density),
                ThemeUtils.getTextColor(theme, context));
        view.setBackground(drawable);
    }

    public static void applyButtonTheme(TextView button, int theme, Context context, int surfaceColor) {
        if (button == null) {
            return;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(surfaceColor);
        drawable.setStroke((int) (2 * context.getResources().getDisplayMetrics().density),
                ThemeUtils.getTextColor(theme, context));
        int padding = (int) (4 * context.getResources().getDisplayMetrics().density);
        button.setBackground(drawable);
        button.setPadding(padding, padding, padding, padding);
        button.setTextColor(ThemeUtils.getTextColor(theme, context));
    }

    public static void applyEditTextTheme(EditText editText, int theme, Context context, int surfaceColor) {
        if (editText == null) {
            return;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(surfaceColor);
        drawable.setStroke((int) (2 * context.getResources().getDisplayMetrics().density),
                ThemeUtils.getTextColor(theme, context));
        editText.setBackground(drawable);
        editText.setTextColor(ThemeUtils.getTextColor(theme, context));
        editText.setHintTextColor(ThemeUtils.getTextColor(theme, context));
    }

    private static int getSurfaceColor(Context context, int theme) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int color = ThemeUtils.getBgColor(theme, context);
        if (prefs.getInt("app_launcher_bg_opacity_enabled", 0) != 1) {
            return color;
        }
        int opacityPercent = prefs.getInt("app_launcher_bg_opacity", 100);
        if (opacityPercent < 0) {
            opacityPercent = 0;
        }
        if (opacityPercent > 100) {
            opacityPercent = 100;
        }
        int alpha = Math.round(255f * (opacityPercent / 100f));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static void applyDialogBlur(Window window, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (prefs.getInt("app_launcher_bg_opacity_enabled", 0) != 1
                || prefs.getInt("app_launcher_bg_blur_enabled", 0) != 1
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        int strength = prefs.getInt("app_launcher_bg_blur_strength", 3);
        window.setBackgroundBlurRadius(WallpaperHelper.getBlurRadiusForStrength(strength));
    }
}
