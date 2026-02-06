package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.TypedValue;

public class DynamicIconHelper {

    /**
     * Get the app icon, preferring adaptive/dynamic icons if available and enabled
     * 
     * @param context        Application context
     * @param packageName    Package name of the app
     * @param useDynamic     Whether to use dynamic icons (Material You themed
     *                       icons)
     * @param theme          Current theme (0 = light, 1 = dark)
     * @param iconBackground Whether to use background (true) or transparent (false)
     * @return The app icon drawable
     * @throws PackageManager.NameNotFoundException if package not found
     */
    public static Drawable getAppIcon(Context context, String packageName, boolean useDynamic, int theme,
            boolean iconBackground) throws PackageManager.NameNotFoundException {
        return getAppIcon(context, packageName, useDynamic, theme, iconBackground, false, false,
                IconShapeHelper.SHAPE_SYSTEM);
    }

    /**
     * Get the app icon, preferring adaptive/dynamic icons if available and enabled
     * 
     * @param context        Application context
     * @param packageName    Package name of the app
     * @param useDynamic     Whether to use dynamic icons (Material You themed
     *                       icons)
     * @param theme          Current theme (0 = light, 1 = dark)
     * @param iconBackground Whether to use background (true) or transparent (false)
     * @param dynamicColors  Whether to use Android's Material You dynamic colors
     * @return The app icon drawable
     * @throws PackageManager.NameNotFoundException if package not found
     */
    public static Drawable getAppIcon(Context context, String packageName, boolean useDynamic, int theme,
            boolean iconBackground, boolean dynamicColors) throws PackageManager.NameNotFoundException {
        return getAppIcon(context, packageName, useDynamic, theme, iconBackground, dynamicColors, false,
                IconShapeHelper.SHAPE_SYSTEM);
    }

    public static Drawable getAppIcon(Context context, String packageName, boolean useDynamic, int theme,
            boolean iconBackground, boolean dynamicColors, int iconShape) throws PackageManager.NameNotFoundException {
        return getAppIcon(context, packageName, useDynamic, theme, iconBackground, dynamicColors, false, iconShape);
    }

    public static Drawable getAppIcon(Context context, String packageName, boolean useDynamic, int theme,
            boolean iconBackground, boolean dynamicColors, boolean invertIconColors)
            throws PackageManager.NameNotFoundException {
        return getAppIcon(context, packageName, useDynamic, theme, iconBackground, dynamicColors, invertIconColors,
                IconShapeHelper.SHAPE_SYSTEM);
    }

    /**
     * Get the app icon, preferring adaptive/dynamic icons if available and enabled
     * 
     * @param context        Application context
     * @param packageName    Package name of the app
     * @param useDynamic     Whether to use dynamic icons (Material You themed
     *                       icons)
     * @param theme          Current theme (0 = light, 1 = dark)
     * @param iconBackground Whether to use background (true) or transparent (false)
     * @param dynamicColors  Whether to use Android's Material You dynamic colors
     * @param iconShape      The shape to apply to the icon (from IconShapeHelper)
     * @return The app icon drawable
     * @throws PackageManager.NameNotFoundException if package not found
     */
    public static Drawable getAppIcon(Context context, String packageName, boolean useDynamic, int theme,
            boolean iconBackground, boolean dynamicColors, boolean invertIconColors, int iconShape)
            throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();

        Drawable icon = pm.getApplicationIcon(packageName);

        if (useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (icon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Drawable monochromeDrawable = adaptiveIcon.getMonochrome();
                    if (monochromeDrawable != null) {
                        int[] colors = getDynamicColors(context, theme, iconBackground, invertIconColors, dynamicColors);
                        int iconColor = colors[0];
                        int backgroundColor = colors[1];

                        Drawable tintedMonochrome;
                        if (monochromeDrawable.getConstantState() != null) {
                            tintedMonochrome = monochromeDrawable.getConstantState().newDrawable(context.getResources())
                                    .mutate();
                        } else {
                            tintedMonochrome = monochromeDrawable.mutate();
                        }

                        if (tintedMonochrome.getIntrinsicWidth() > 0 && tintedMonochrome.getIntrinsicHeight() > 0) {
                            tintedMonochrome.setBounds(0, 0, tintedMonochrome.getIntrinsicWidth(),
                                    tintedMonochrome.getIntrinsicHeight());
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            tintedMonochrome.setColorFilter(new BlendModeColorFilter(iconColor, BlendMode.SRC_IN));
                        } else {
                            tintedMonochrome.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        }

                        tintedMonochrome.setFilterBitmap(true);

                        if (iconShape != IconShapeHelper.SHAPE_SYSTEM && iconBackground) {
                            int size = 108;
                            Drawable shapedIcon = IconShapeHelper.createShapedIcon(context, tintedMonochrome,
                                    backgroundColor, iconShape, size, false);
                            if (shapedIcon != null) {
                                return shapedIcon;
                            }
                        }

                        if (!iconBackground) {
                            float expandFraction = -0.35f;
                            InsetDrawable expandedIcon = new InsetDrawable(tintedMonochrome, expandFraction);
                            return expandedIcon;
                        }

                        ColorDrawable themedBackground = new ColorDrawable(backgroundColor);

                        AdaptiveIconDrawable monochromeIcon = new AdaptiveIconDrawable(
                                themedBackground,
                                tintedMonochrome);

                        if (icon.getIntrinsicWidth() > 0 && icon.getIntrinsicHeight() > 0) {
                            monochromeIcon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                        }

                        return monochromeIcon;
                    }
                }
            }
        }

        return applyShapeIfNeeded(context, icon, iconShape, iconBackground);
    }

    private static Drawable applyShapeIfNeeded(Context context, Drawable drawable, int iconShape,
            boolean iconBackground) {
        if (iconShape != IconShapeHelper.SHAPE_SYSTEM && iconBackground) {
            
            // Use 108 to match the size used for Dynamic Icons
            int size = 108;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) drawable;
                Drawable bg = adaptiveIcon.getBackground();
                Drawable fg = adaptiveIcon.getForeground();
                
                if (bg != null && fg != null) {
                    // Try to use the layers directly to avoid system mask
                    try {
                        Drawable shaped = IconShapeHelper.createShapedIcon(context, fg.mutate(), bg.mutate(), iconShape, size);
                        if (shaped != null) return shaped;
                    } catch (Exception e) {
                        // Fallback to standard masking if something goes wrong
                    }
                }
            }

            return IconShapeHelper.applyShapeMask(context, drawable, iconShape, size);
        }
        return drawable;
    }

    /**
     * Get Material You dynamic colors for icon and background
     * 
     * @param context        Application context
     * @param theme          Current theme (0 = light, 1 = dark, 2 = dynamic light,
     *                       3 = dynamic dark)
     * @param iconBackground Whether to use background
     * @return int array where [0] is icon color and [1] is background color
     */
    public static int[] getDynamicColors(Context context, int theme, boolean iconBackground) {
        return getDynamicColors(context, theme, iconBackground, false);
    }

    /**
     * Get Material You dynamic colors for icon and background
     * 
     * @param context          Application context
     * @param theme            Current theme (0 = light, 1 = dark, 2 = dynamic
     *                         light, 3 = dynamic dark)
     * @param iconBackground   Whether to use background
     * @param invertIconColors Whether to invert icon and background colors
     * @return int array where [0] is icon color and [1] is background color
     */
    public static int[] getDynamicColors(Context context, int theme, boolean iconBackground, boolean invertIconColors, boolean useMaterialYou) {
        int iconColor;
        int backgroundColor;
        boolean isDark = ThemeUtils.isDarkTheme(theme, context);

        if (context != null && theme == ThemeUtils.THEME_CUSTOM) {
            int customBg = ThemeUtils.getBgColor(theme, context);
            int customTx = ThemeUtils.getTextColor(theme, context);
            
            if (iconBackground) {
                backgroundColor = customTx; // Accent
                iconColor = customBg; // Bg
            } else {
                backgroundColor = customBg; 
                iconColor = customTx;
            }

            if (invertIconColors) {
                int temp = iconColor;
                iconColor = backgroundColor;
                backgroundColor = temp;
            }
            return new int[] { iconColor, backgroundColor };
        }

        if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (!isDark) {
                    if (iconBackground) {
                        iconColor = context.getResources().getColor(android.R.color.system_accent2_50,
                                context.getTheme());
                        backgroundColor = context.getResources().getColor(android.R.color.system_accent1_700,
                                context.getTheme());
                    } else {
                        iconColor = context.getResources().getColor(android.R.color.system_accent1_700,
                                context.getTheme());
                        backgroundColor = ThemeUtils.getBgColor(theme, context);
                    }
                } else {
                    if (iconBackground) {
                        iconColor = context.getResources().getColor(android.R.color.system_accent2_900,
                                context.getTheme());
                        backgroundColor = context.getResources().getColor(android.R.color.system_accent1_100,
                                context.getTheme());
                    } else {
                        iconColor = context.getResources().getColor(android.R.color.system_accent1_100,
                                context.getTheme());
                        backgroundColor = ThemeUtils.getBgColor(theme, context);
                    }
                }

                if (invertIconColors) {
                    int temp = iconColor;
                    iconColor = backgroundColor;
                    backgroundColor = temp;
                }

                return new int[] { iconColor, backgroundColor };
            } catch (Exception e) {
                // Fallback to default colors if dynamic colors are not available
            }
        }

        if (iconBackground) {
            iconColor = isDark ? Color.BLACK : Color.WHITE;
            backgroundColor = isDark ? Color.WHITE : Color.BLACK;
        } else {
            iconColor = isDark ? Color.WHITE : Color.BLACK;
            backgroundColor = ThemeUtils.getBgColor(theme, context);
        }

        if (invertIconColors) {
            int temp = iconColor;
            iconColor = backgroundColor;
            backgroundColor = temp;
        }

        return new int[] { iconColor, backgroundColor };
    }

    public static int[] getDynamicColors(Context context, int theme, boolean iconBackground, boolean invertIconColors) {
        return getDynamicColors(context, theme, iconBackground, invertIconColors, true);
    }

    private static int getThemedIconColor(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
                return typedValue.data;
            } catch (Exception e) {
                // Fallback
            }
        }
        return Color.parseColor("#1976D2"); // Material Blue
    }

    private static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Check if an app has an adaptive icon with monochrome layer (supports dynamic
     * theming)
     * 
     * @param context     Application context
     * @param packageName Package name of the app
     * @return true if the app has a monochrome layer for dynamic theming
     */
    public static boolean hasMonochromeIcon(Context context, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                PackageManager pm = context.getPackageManager();
                Drawable icon = pm.getApplicationIcon(packageName);

                if (icon instanceof AdaptiveIconDrawable) {
                    AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;
                    Drawable monochromeLayer = adaptiveIcon.getMonochrome();
                    return monochromeLayer != null;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Create a special icon (settings, search, notifications) with adaptive icon
     * background
     * This follows the same style as other dynamic icons with backgrounds
     * 
     * @param context        Application context
     * @param drawableResId  The drawable resource ID for the icon
     * @param theme          Current theme
     * @param iconBackground Whether to show background
     * @param dynamicColors  Whether to use dynamic colors
     * @return Drawable with appropriate styling
     */
    public static Drawable createSpecialIcon(Context context, int drawableResId, int theme, boolean iconBackground,
            boolean dynamicColors) {
        return createSpecialIcon(context, drawableResId, theme, iconBackground, dynamicColors, false,
                IconShapeHelper.SHAPE_SYSTEM);
    }

    public static Drawable createSpecialIcon(Context context, int drawableResId, int theme, boolean iconBackground,
            boolean dynamicColors, boolean invertIconColors) {
        return createSpecialIcon(context, drawableResId, theme, iconBackground, dynamicColors, invertIconColors,
                IconShapeHelper.SHAPE_SYSTEM);
    }

    /**
     * Create a special icon (settings, search, notifications) with adaptive icon
     * background
     * This follows the same style as other dynamic icons with backgrounds
     * 
     * @param context        Application context
     * @param drawableResId  The drawable resource ID for the icon
     * @param theme          Current theme
     * @param iconBackground Whether to show background
     * @param dynamicColors  Whether to use dynamic colors
     * @param iconShape      The shape to apply to the icon (from IconShapeHelper)
     * @return Drawable with appropriate styling
     */
    public static Drawable createSpecialIcon(Context context, int drawableResId, int theme, boolean iconBackground,
            boolean dynamicColors, boolean invertIconColors, int iconShape) {
        Drawable iconDrawable = context.getResources().getDrawable(drawableResId, context.getTheme()).mutate();

        int[] colors = getDynamicColors(context, theme, iconBackground, invertIconColors, dynamicColors);
        int iconColor = colors[0];
        int backgroundColor = colors[1];

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            iconDrawable.setColorFilter(new BlendModeColorFilter(iconColor, BlendMode.SRC_IN));
        } else {
            iconDrawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        }

        if (iconBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (iconShape != IconShapeHelper.SHAPE_SYSTEM) {
                int size = 108;
                Drawable shapedIcon = IconShapeHelper.createShapedIcon(context, iconDrawable, backgroundColor,
                        iconShape, size);
                if (shapedIcon != null) {
                    return shapedIcon;
                }
            }

            ColorDrawable bgDrawable = new ColorDrawable(backgroundColor);

            float insetFraction = 0.30f;
            InsetDrawable insetIcon = new InsetDrawable(iconDrawable, insetFraction);

            AdaptiveIconDrawable adaptiveIcon = new AdaptiveIconDrawable(bgDrawable, insetIcon);

            return adaptiveIcon;
        }

        float expandFraction = 0.15f;
        InsetDrawable expandedIcon = new InsetDrawable(iconDrawable, expandFraction);
        return expandedIcon;
    }
}
