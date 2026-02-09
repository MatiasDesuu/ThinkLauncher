package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;

public class ThemeUtils {

    // Theme constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_DYNAMIC_LIGHT = 2;
    public static final int THEME_DYNAMIC_DARK = 3;
    public static final int THEME_CUSTOM = 4;

    // Theme names for UI
    public static final String[] THEME_NAMES = {"Light", "Dark", "Dynamic Light", "Dynamic Dark", "Custom"};

    public static void applyDialogBackground(View view, int theme, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getBgColor(theme, context));
        view.setBackground(drawable);
    }

    public static void applyEditTextTheme(EditText editText, int theme, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getBgColor(theme, context));
        editText.setBackground(drawable);
        editText.setTextColor(getTextColor(theme, context));
        editText.setHintTextColor(getTextColor(theme, context));
    }

    public static void applyButtonTheme(TextView button, int theme, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getBgColor(theme, context));
        drawable.setStroke((int) (2 * context.getResources().getDisplayMetrics().density), getTextColor(theme, context));
        int padding = (int) (4 * context.getResources().getDisplayMetrics().density);
        button.setBackground(drawable);
        button.setPadding(padding, padding, padding, padding);
        button.setTextColor(getTextColor(theme, context));
    }

    public static void applyTextColor(TextView textView, int theme) {
        textView.setTextColor(isDarkTheme(theme) ? Color.WHITE : Color.BLACK);
    }

    public static void applyTextColor(TextView textView, int theme, Context context) {
        textView.setTextColor(getTextColor(theme, context));
    }

    public static void applyBackgroundColor(View view, int theme) {
        view.setBackgroundColor(isDarkTheme(theme) ? Color.BLACK : Color.WHITE);
    }

    public static void applyBackgroundColor(View view, int theme, Context context) {
        view.setBackgroundColor(getBgColor(theme, context));
    }

    /**
     * Check if the theme is a dark variant (Dark or Dynamic Dark)
     */
    public static boolean isDarkTheme(int theme) {
        return theme == THEME_DARK || theme == THEME_DYNAMIC_DARK;
    }

    /**
     * Check if the theme is dark, considering custom colors if enabled
     */
    public static boolean isDarkTheme(int theme, Context context) {
        if (theme == THEME_CUSTOM && context != null) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
            int bgColor = prefs.getInt("custom_bg_color", Color.WHITE);
            return isColorDark(bgColor);
        }
        return isDarkTheme(theme);
    }

    /**
     * Check if a color is dark
     */
    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    /**
     * Check if the theme is a dynamic variant (Dynamic Light or Dynamic Dark)
     */
    public static boolean isDynamicTheme(int theme) {
        return theme == THEME_DYNAMIC_LIGHT || theme == THEME_DYNAMIC_DARK;
    }

    /**
     * Get background color for the given theme (legacy method without context)
     */
    public static int getBgColor(int theme) {
        return isDarkTheme(theme) ? Color.BLACK : Color.WHITE;
    }

    /**
     * Get text color for the given theme (legacy method without context)
     */
    public static int getTextColor(int theme) {
        return isDarkTheme(theme) ? Color.WHITE : Color.BLACK;
    }

    /**
     * Get background color for the given theme with dynamic color support
     */
    public static int getBgColor(int theme, Context context) {
        if (theme == THEME_CUSTOM && context != null) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
            return prefs.getInt("custom_bg_color", Color.WHITE);
        }
        if (isDynamicTheme(theme) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            try {
                if (theme == THEME_DYNAMIC_LIGHT) {
                    // Light dynamic: use a very light tint from Material You
                    return context.getResources().getColor(android.R.color.system_accent2_50, context.getTheme());
                } else {
                    // Dark dynamic: use a very dark tint from Material You
                    return context.getResources().getColor(android.R.color.system_accent2_900, context.getTheme());
                }
            } catch (Exception e) {
                // Fallback to standard colors
            }
        }
        return isDarkTheme(theme) ? Color.BLACK : Color.WHITE;
    }

    /**
     * Get text color for the given theme with dynamic color support
     */
    public static int getTextColor(int theme, Context context) {
        if (theme == THEME_CUSTOM && context != null) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
            return prefs.getInt("custom_accent_color", Color.BLACK);
        }
        if (isDynamicTheme(theme) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            try {
                if (theme == THEME_DYNAMIC_LIGHT) {
                    // Light dynamic: use a dark vibrant color for text
                    return context.getResources().getColor(android.R.color.system_accent1_700, context.getTheme());
                } else {
                    // Dark dynamic: use a light vibrant color for text
                    return context.getResources().getColor(android.R.color.system_accent1_100, context.getTheme());
                }
            } catch (Exception e) {
                // Fallback to standard colors
            }
        }
        return isDarkTheme(theme) ? Color.WHITE : Color.BLACK;
    }

    /**
     * Get the next theme in the cycle
     */
    public static int getNextTheme(int currentTheme) {
        return (currentTheme + 1) % THEME_NAMES.length;
    }

    /**
     * Get the previous theme in the cycle
     */
    public static int getPreviousTheme(int currentTheme) {
        return (currentTheme - 1 + THEME_NAMES.length) % THEME_NAMES.length;
    }

    /**
     * Get the theme name for display
     */
    public static String getThemeName(int theme) {
        if (theme >= 0 && theme < THEME_NAMES.length) {
            return THEME_NAMES[theme];
        }
        return THEME_NAMES[0];
    }

    /**
     * Apply theme colors to all TextViews and ImageViews in a ViewGroup recursively
     * Also applies button theme to plus/minus buttons and gesture buttons
     */
    public static void applyThemeToViewGroup(ViewGroup viewGroup, int theme, Context context) {
        int textColor = getTextColor(theme, context);
        int bgColor = getBgColor(theme, context);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                tv.setTextColor(textColor);
                // Check if it's a button (plus/minus or gesture button)
                int id = tv.getId();
                if (id == R.id.btn_minus || id == R.id.btn_plus ||
                    id == R.id.swipe_left_app || id == R.id.swipe_right_app ||
                    id == R.id.swipe_down_app || id == R.id.swipe_up_app ||
                    id == R.id.clock_app || id == R.id.date_app ||
                    id == R.id.select_button_text || id == R.id.remove_button_text ||
                    id == R.id.custom_bg_color_text || id == R.id.custom_accent_color_text) {
                    applyButtonTheme(tv, theme, context);
                }
            } else if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(textColor);
            }
            // Also apply to dividers (View with height 2dp used as separator)
            if (child.getClass() == View.class) {
                child.setBackgroundColor(textColor);
            }
            if (child instanceof ViewGroup) {
                applyThemeToViewGroup((ViewGroup) child, theme, context);
            }
        }
    }
}