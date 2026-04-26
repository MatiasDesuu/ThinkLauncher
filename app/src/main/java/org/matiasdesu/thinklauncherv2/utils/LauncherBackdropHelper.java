package org.matiasdesu.thinklauncherv2.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;

import androidx.core.view.WindowCompat;

import org.matiasdesu.thinklauncherv2.R;

public final class LauncherBackdropHelper {

    public static final class Result {
        public final int surfaceColor;
        public final boolean showWallpaperBackdrop;

        public Result(int surfaceColor, boolean showWallpaperBackdrop) {
            this.surfaceColor = surfaceColor;
            this.showWallpaperBackdrop = showWallpaperBackdrop;
        }
    }

    private LauncherBackdropHelper() {
    }

    public static void applySurfaceBackground(View view, boolean showWallpaperBackdrop, int surfaceColor) {
        if (view == null) {
            return;
        }
        view.setBackgroundColor(showWallpaperBackdrop ? Color.TRANSPARENT : surfaceColor);
    }

    public static void applySurfaceBackgrounds(boolean showWallpaperBackdrop, int surfaceColor, View... views) {
        if (views == null) {
            return;
        }
        for (View view : views) {
            applySurfaceBackground(view, showWallpaperBackdrop, surfaceColor);
        }
    }

    public static int resolveThemeResId(Context context, int theme, boolean opacityEnabled) {
        boolean darkTheme = ThemeUtils.isDarkTheme(theme, context);
        if (darkTheme) {
            return opacityEnabled ? R.style.AppTheme_Translucent_Dark : R.style.AppTheme_Dark;
        }
        return opacityEnabled ? R.style.AppTheme_Translucent : R.style.AppTheme;
    }

    public static Result setup(Activity activity, int theme, boolean opacityEnabled) {
        SharedPreferences prefs = activity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        boolean blurEnabled = prefs.getInt("app_launcher_bg_blur_enabled", 0) == 1;
        int blurStrength = prefs.getInt("app_launcher_bg_blur_strength", 3);
        int surfaceColor = ThemeUtils.getBgColor(theme, activity);
        if (opacityEnabled) {
            int opacityPercent = prefs.getInt("app_launcher_bg_opacity", 100);
            if (opacityPercent < 0) {
                opacityPercent = 0;
            }
            if (opacityPercent > 100) {
                opacityPercent = 100;
            }
            int alpha = Math.round(255f * (opacityPercent / 100f));
            surfaceColor = Color.argb(alpha, Color.red(surfaceColor), Color.green(surfaceColor),
                    Color.blue(surfaceColor));
            activity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        boolean showWallpaperBackdrop = opacityEnabled && WallpaperHelper.hasWallpaper(activity);
        View backdropRoot = activity.findViewById(R.id.root_layout);
        View contentLayout = activity.findViewById(R.id.content_layout);
        ImageView wallpaperView = activity.findViewById(R.id.wallpaper_view);

        applySurfaceBackground(backdropRoot, showWallpaperBackdrop, surfaceColor);
        if (contentLayout != null) {
            contentLayout.setBackgroundColor(surfaceColor);
        }

        if (showWallpaperBackdrop && wallpaperView != null) {
            int[] screenDimensions = WallpaperHelper.getScreenDimensions(activity);
            Bitmap wallpaper = WallpaperHelper.getWallpaperForScreen(activity, screenDimensions[0], screenDimensions[1],
                    blurEnabled, blurStrength);
            if (wallpaper != null) {
                wallpaperView.setImageBitmap(wallpaper);
                wallpaperView.setVisibility(View.VISIBLE);
            } else {
                showWallpaperBackdrop = false;
                applySurfaceBackground(backdropRoot, false, surfaceColor);
            }
        }

        if (showWallpaperBackdrop) {
            WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
                activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setStatusBarColor(surfaceColor);
                activity.getWindow().setNavigationBarColor(surfaceColor);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!ThemeUtils.isDarkTheme(theme, activity)) {
                activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                activity.getWindow().getDecorView().setSystemUiVisibility(0);
            }
        }

        if (backdropRoot != null) {
            final boolean finalShowWallpaperBackdrop = showWallpaperBackdrop;
            final View finalContentLayout = contentLayout;
            backdropRoot.setOnApplyWindowInsetsListener((v, insets) -> {
                if (finalContentLayout == null) {
                    return insets;
                }
                if (finalShowWallpaperBackdrop) {
                    int topInset;
                    int bottomInset;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
                        bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    } else {
                        topInset = insets.getSystemWindowInsetTop();
                        bottomInset = insets.getSystemWindowInsetBottom();
                    }
                    finalContentLayout.setPadding(0, topInset, 0, bottomInset);
                } else {
                    finalContentLayout.setPadding(0, 0, 0, 0);
                }
                return insets;
            });
            backdropRoot.requestApplyInsets();
        }

        View root = activity.findViewById(android.R.id.content);
        applySurfaceBackground(root, showWallpaperBackdrop, surfaceColor);

        return new Result(surfaceColor, showWallpaperBackdrop);
    }
}
