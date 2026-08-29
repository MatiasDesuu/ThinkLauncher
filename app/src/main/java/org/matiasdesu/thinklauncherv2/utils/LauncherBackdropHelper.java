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

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.core.view.WindowCompat;

import org.matiasdesu.thinklauncherv2.R;

public final class LauncherBackdropHelper {

    private static final ExecutorService WALLPAPER_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "TL-WallpaperLoader");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    });

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
            surfaceColor = WallpaperHelper.applyOpacity(surfaceColor,
                    prefs.getInt("app_launcher_bg_opacity", 100));
        }
        activity.getWindow().setBackgroundDrawable(new ColorDrawable(surfaceColor));

        boolean showWallpaperBackdrop = opacityEnabled && WallpaperHelper.hasWallpaper(activity);
        final AtomicBoolean showWallpaperBackdropRef = new AtomicBoolean(showWallpaperBackdrop);
        View backdropRoot = activity.findViewById(R.id.root_layout);
        View contentLayout = activity.findViewById(R.id.content_layout);
        ImageView wallpaperView = activity.findViewById(R.id.wallpaper_view);

        if (backdropRoot != null) {
            backdropRoot.setBackgroundColor(surfaceColor);
        }
        if (contentLayout != null) {
            contentLayout.setBackgroundColor(surfaceColor);
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
            final View finalContentLayout = contentLayout;
            backdropRoot.setOnApplyWindowInsetsListener((v, insets) -> {
                if (finalContentLayout == null) {
                    return insets;
                }
                if (showWallpaperBackdropRef.get()) {
                    int leftPad = finalContentLayout.getPaddingLeft();
                    int rightPad = finalContentLayout.getPaddingRight();
                    int topInset;
                    int bottomInset;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
                        bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    } else {
                        topInset = insets.getSystemWindowInsetTop();
                        bottomInset = insets.getSystemWindowInsetBottom();
                    }
                    finalContentLayout.setPadding(leftPad, topInset, rightPad, bottomInset);
                } else {
                    finalContentLayout.setPadding(0, 0, 0, 0);
                }
                return insets;
            });
            backdropRoot.requestApplyInsets();
        }

        View root = activity.findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(surfaceColor);
        }

        if (showWallpaperBackdrop && wallpaperView != null) {
            wallpaperView.setVisibility(View.GONE);
            wallpaperView.setScaleType(ImageView.ScaleType.FIT_XY);
            final WeakReference<Activity> activityRef = new WeakReference<>(activity);
            final WeakReference<ImageView> wallpaperViewRef = new WeakReference<>(wallpaperView);
            final int finalSurfaceColor = surfaceColor;
            final boolean finalBlurEnabled = blurEnabled;
            final int finalBlurStrength = blurStrength;
            final View finalBackdropRoot = backdropRoot;
            final View finalRoot = root;

            Runnable applyWallpaperMode = () -> {
                Activity a = activityRef.get();
                if (a == null) return;
                if (a.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && a.isDestroyed())) return;
                a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowCompat.setDecorFitsSystemWindows(a.getWindow(), false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    a.getWindow().setStatusBarColor(Color.TRANSPARENT);
                    a.getWindow().setNavigationBarColor(Color.TRANSPARENT);
                }
                View br = a.findViewById(R.id.root_layout);
                View rt = a.findViewById(android.R.id.content);
                applySurfaceBackground(br, true, finalSurfaceColor);
                applySurfaceBackground(rt, true, finalSurfaceColor);
                ImageView iv = wallpaperViewRef.get();
                if (iv != null) iv.setVisibility(View.VISIBLE);
                if (br != null) br.requestApplyInsets();
            };

            Runnable loadWithCurrentDimensions = () -> {
                Activity act = activityRef.get();
                ImageView iv = wallpaperViewRef.get();
                if (act == null || iv == null) return;
                int w = 0;
                int h = 0;
                View br = act.findViewById(R.id.root_layout);
                if (br != null && br.getWidth() > 0 && br.getHeight() > 0) {
                    w = br.getWidth();
                    h = br.getHeight();
                } else if (finalBackdropRoot != null && finalBackdropRoot.getWidth() > 0 && finalBackdropRoot.getHeight() > 0) {
                    w = finalBackdropRoot.getWidth();
                    h = finalBackdropRoot.getHeight();
                } else {
                    int[] dims = WallpaperHelper.getScreenDimensions(act);
                    w = dims[0];
                    h = dims[1];
                }
                final int fw = w;
                final int fh = h;
                Bitmap cached = WallpaperHelper.getWallpaperForScreenCached(act, fw, fh, finalBlurEnabled, finalBlurStrength);
                if (cached != null) {
                    act.runOnUiThread(() -> {
                        ImageView iv2 = wallpaperViewRef.get();
                        if (iv2 == null) return;
                        if (act.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && act.isDestroyed())) return;
                        iv2.setImageBitmap(cached);
                        applyWallpaperMode.run();
                    });
                } else {
                    WALLPAPER_EXECUTOR.execute(() -> {
                        Activity act2 = activityRef.get();
                        if (act2 == null) return;
                        Bitmap wallpaper = WallpaperHelper.getWallpaperForScreenCached(act2, fw, fh, finalBlurEnabled, finalBlurStrength);
                        act2.runOnUiThread(() -> {
                            Activity a = activityRef.get();
                            ImageView iv3 = wallpaperViewRef.get();
                            if (a == null || iv3 == null) return;
                            if (a.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && a.isDestroyed())) return;
                            if (wallpaper != null) {
                                iv3.setImageBitmap(wallpaper);
                                applyWallpaperMode.run();
                            } else {
                                showWallpaperBackdropRef.set(false);
                                View br2 = a.findViewById(R.id.root_layout);
                                if (br2 != null) {
                                    br2.setBackgroundColor(finalSurfaceColor);
                                    br2.requestApplyInsets();
                                }
                                View rt2 = a.findViewById(android.R.id.content);
                                if (rt2 != null) rt2.setBackgroundColor(finalSurfaceColor);
                            }
                        });
                    });
                }
            };

            if (backdropRoot != null && backdropRoot.getWidth() > 0 && backdropRoot.getHeight() > 0) {
                loadWithCurrentDimensions.run();
            } else if (backdropRoot != null) {
                backdropRoot.post(loadWithCurrentDimensions);
            } else {
                loadWithCurrentDimensions.run();
            }
        }

        return new Result(surfaceColor, showWallpaperBackdrop);
    }
}
