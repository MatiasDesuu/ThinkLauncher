package org.matiasdesu.thinklauncherv2.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.AppLauncherActivity;
import org.matiasdesu.thinklauncherv2.ui.CalculatorActivity;
import org.matiasdesu.thinklauncherv2.ui.CalendarActivity;
import org.matiasdesu.thinklauncherv2.ui.ClockActivity;
import org.matiasdesu.thinklauncherv2.ui.GalleryActivity;
import org.matiasdesu.thinklauncherv2.ui.KOReaderHistoryActivity;

public final class SystemAppHelper {

    public static final String LAUNCHER_SETTINGS = "launcher_settings";
    public static final String APP_LAUNCHER = "app_launcher";
    public static final String NOTIFICATION_PANEL = "notification_panel";
    public static final String KOREADER_HISTORY = "koreader_history";
    public static final String CALENDAR = "calendar";
    public static final String GALLERY = "gallery";
    public static final String CLOCK = "clock";
    public static final String CALCULATOR = "calculator";
    public static final String BIGME_CONTROL_PANEL = "bigme_control_panel";
    public static final String BIGME_EINK_SETTINGS = "bigme_eink_settings";
    public static final String NEXT_HOME_PAGE = "next_home_page";
    public static final String PREVIOUS_HOME_PAGE = "previous_home_page";
    public static final String FOLDER = "folder";
    public static final String SYSTEM_DEFAULT = "system_default";
    public static final String BLANK = "blank";
    public static final String WEB_APPS = "web_apps";
    public static final String HIDDEN_APP = "hidden_app";

    private SystemAppHelper() {}

    public static boolean isSystemApp(String pkg) {
        if (pkg == null) return false;
        if (pkg.equals(LAUNCHER_SETTINGS) || pkg.equals(APP_LAUNCHER) || pkg.equals(NOTIFICATION_PANEL)
                || pkg.equals(KOREADER_HISTORY) || pkg.equals(CALENDAR) || pkg.equals(GALLERY)
                || pkg.equals(CLOCK) || pkg.equals(CALCULATOR) || pkg.equals(BIGME_CONTROL_PANEL) || pkg.equals(BIGME_EINK_SETTINGS)
                || pkg.equals(NEXT_HOME_PAGE) || pkg.equals(PREVIOUS_HOME_PAGE) || pkg.equals(FOLDER)
                || pkg.equals(SYSTEM_DEFAULT) || pkg.equals(BLANK) || pkg.equals(WEB_APPS) || pkg.equals(HIDDEN_APP)) return true;
        return pkg.startsWith("folder_") || pkg.startsWith("webapp_") || pkg.startsWith("hidden_app_");
    }

    public static boolean isSpecialForHome(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        if (isSystemApp(pkg)) return true;
        return false;
    }

    public static boolean isSpecialLaunchable(String pkg) {
        if (pkg == null) return false;
        return pkg.equals(LAUNCHER_SETTINGS) || pkg.equals(APP_LAUNCHER) || pkg.equals(NOTIFICATION_PANEL)
                || pkg.equals(KOREADER_HISTORY) || pkg.equals(CALENDAR) || pkg.equals(GALLERY)
                || pkg.equals(CLOCK) || pkg.equals(CALCULATOR) || pkg.equals(BIGME_CONTROL_PANEL) || pkg.equals(BIGME_EINK_SETTINGS)
                || pkg.equals(NEXT_HOME_PAGE) || pkg.equals(PREVIOUS_HOME_PAGE);
    }

    public static int getIconRes(String pkg) {
        if (pkg == null) return 0;
        if (pkg.equals(LAUNCHER_SETTINGS)) return R.drawable.settings;
        if (pkg.equals(APP_LAUNCHER)) return R.drawable.search;
        if (pkg.equals(NOTIFICATION_PANEL)) return R.drawable.notifications;
        if (pkg.equals(KOREADER_HISTORY)) return R.drawable.koreader;
        if (pkg.equals(CALENDAR)) return R.drawable.date;
        if (pkg.equals(GALLERY)) return R.drawable.gallery;
        if (pkg.equals(CLOCK)) return R.drawable.time;
        if (pkg.equals(CALCULATOR)) return R.drawable.calculator;
        if (pkg.equals(BIGME_CONTROL_PANEL)) return R.drawable.generic_app;
        if (pkg.equals(BIGME_EINK_SETTINGS)) return R.drawable.generic_app;
        if (pkg.startsWith("webapp_")) return R.drawable.webapps;
        if (pkg.startsWith("folder_") || pkg.equals(FOLDER)) return R.drawable.folder;
        return 0;
    }

    public static String getDefaultLabel(String pkg) {
        if (pkg == null) return "";
        if (pkg.equals(LAUNCHER_SETTINGS)) return "Launcher Settings";
        if (pkg.equals(APP_LAUNCHER)) return "App Launcher";
        if (pkg.equals(NOTIFICATION_PANEL)) return "Notification Panel";
        if (pkg.equals(KOREADER_HISTORY)) return "KOReader History";
        if (pkg.equals(CALENDAR)) return "Calendar Screen";
        if (pkg.equals(GALLERY)) return "Gallery";
        if (pkg.equals(CLOCK)) return "Clock";
        if (pkg.equals(CALCULATOR)) return "Calculator";
        if (pkg.equals(BIGME_CONTROL_PANEL)) return "Bigme Control Panel";
        if (pkg.equals(BIGME_EINK_SETTINGS)) return "Bigme Eink Settings";
        if (pkg.equals(NEXT_HOME_PAGE)) return "Next Home Page";
        if (pkg.equals(PREVIOUS_HOME_PAGE)) return "Previous Home Page";
        return pkg;
    }

    public static Intent getLaunchIntent(Context context, String pkg) {
        if (pkg == null) return null;
        if (pkg.equals(CLOCK)) return new Intent(context, ClockActivity.class);
        if (pkg.equals(CALCULATOR)) return new Intent(context, CalculatorActivity.class);
        if (pkg.equals(CALENDAR)) return new Intent(context, CalendarActivity.class);
        if (pkg.equals(GALLERY)) return new Intent(context, GalleryActivity.class);
        if (pkg.equals(KOREADER_HISTORY)) return new Intent(context, KOReaderHistoryActivity.class);
        if (pkg.equals(APP_LAUNCHER)) return new Intent(context, AppLauncherActivity.class);
        if (pkg.equals(LAUNCHER_SETTINGS)) {
            try {
                Class<?> clazz = Class.forName("org.matiasdesu.thinklauncherv2.settings.SettingsActivity");
                return new Intent(context, clazz);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        if (pkg.equals(BIGME_CONTROL_PANEL)) {
            Intent intent = new Intent();
            intent.setComponent(new android.content.ComponentName("com.xrz.sys.control", "com.xrz.settings.ControlCenterActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
        if (pkg.equals(NOTIFICATION_PANEL)) return null;
        return null;
    }

    public static boolean launch(Context context, String pkg) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        boolean animate = prefs.getInt("screen_animations", 0) == 1;
        return launch(context, pkg, animate, false);
    }

    public static boolean launch(Activity activity, String pkg) {
        SharedPreferences prefs = activity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        boolean animate = prefs.getInt("screen_animations", 0) == 1;
        boolean result = launch((Context) activity, pkg, animate, true);
        if (result && animate && pkg != null && (pkg.equals(NOTIFICATION_PANEL) || pkg.equals(BIGME_CONTROL_PANEL))) {
        } else if (result && animate) {
            activity.overridePendingTransition(R.anim.dialog_fade_in, 0);
        } else if (result) {
            activity.overridePendingTransition(0, 0);
        }
        return result;
    }

    private static boolean launch(Context context, String pkg, boolean animate, boolean fromActivity) {
        if (pkg == null || pkg.isEmpty()) return false;
        if (pkg.equals(NOTIFICATION_PANEL)) {
            try {
                Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel")
                        .invoke(context.getSystemService("statusbar"));
                return true;
            } catch (Exception e) {
                return true;
            }
        }
        Intent intent = getLaunchIntent(context, pkg);
        if (intent != null) {
            if (!animate) intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (pkg.equals(BIGME_CONTROL_PANEL)) {
                try {
                    context.startActivity(intent);
                    if (context instanceof Activity && !animate) ((Activity) context).overridePendingTransition(0, 0);
                    return true;
                } catch (Exception e) {
                    android.widget.Toast.makeText(context, "Bigme Control Panel not available", android.widget.Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            try {
                if (context instanceof Activity) {
                    context.startActivity(intent);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        if (pkg.equals(NEXT_HOME_PAGE) || pkg.equals(PREVIOUS_HOME_PAGE)) return true;
        if (pkg.startsWith("folder_") || pkg.equals(FOLDER) || pkg.startsWith("webapp_") || pkg.equals(WEB_APPS) || pkg.startsWith("hidden_app_") || pkg.equals(HIDDEN_APP) || pkg.equals(BLANK) || pkg.equals(SYSTEM_DEFAULT)) {
            return false;
        }
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) {
            SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
            boolean appAnimate = prefs.getInt("app_launch_animation", 0) == 1;
            if (!appAnimate) launch.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            try {
                context.startActivity(launch);
            } catch (Exception e) {
                return false;
            }
            if (context instanceof Activity) {
                if (context instanceof org.matiasdesu.thinklauncherv2.ui.AppLauncherActivity) {
                    if (appAnimate) ((Activity) context).overridePendingTransition(R.anim.dialog_fade_in, 0);
                    else ((Activity) context).overridePendingTransition(0, 0);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> ((Activity) context).finish(), 100);
                } else {
                    if (appAnimate) ((Activity) context).overridePendingTransition(R.anim.dialog_fade_in, 0);
                    else ((Activity) context).overridePendingTransition(0, 0);
                }
            }
            return true;
        }
        return false;
    }

    public static void addCoreLauncherApps(java.util.List<String> labels, java.util.List<String> packages) {
        labels.add("Launcher Settings");
        packages.add(LAUNCHER_SETTINGS);
        labels.add("KOReader History");
        packages.add(KOREADER_HISTORY);
        labels.add("Calendar Screen");
        packages.add(CALENDAR);
        labels.add("Gallery");
        packages.add(GALLERY);
        labels.add("Clock");
        packages.add(CLOCK);
        labels.add("Calculator");
        packages.add(CALCULATOR);
    }

    public static int insertCoreAppsForLauncher(java.util.List<String> labels, java.util.List<String> packages, int startIndex) {
        labels.add(startIndex, getDefaultLabel(LAUNCHER_SETTINGS));
        packages.add(startIndex++, LAUNCHER_SETTINGS);
        labels.add(startIndex, getDefaultLabel(KOREADER_HISTORY));
        packages.add(startIndex++, KOREADER_HISTORY);
        labels.add(startIndex, getDefaultLabel(CALENDAR));
        packages.add(startIndex++, CALENDAR);
        labels.add(startIndex, getDefaultLabel(GALLERY));
        packages.add(startIndex++, GALLERY);
        labels.add(startIndex, getDefaultLabel(CLOCK));
        packages.add(startIndex++, CLOCK);
        labels.add(startIndex, getDefaultLabel(CALCULATOR));
        packages.add(startIndex++, CALCULATOR);
        return startIndex;
    }

    public static int insertCoreAppsForSelector(java.util.List<String> labels, java.util.List<String> packages, int startIndex) {
        labels.add(startIndex, getDefaultLabel(LAUNCHER_SETTINGS));
        packages.add(startIndex++, LAUNCHER_SETTINGS);
        labels.add(startIndex, getDefaultLabel(NOTIFICATION_PANEL));
        packages.add(startIndex++, NOTIFICATION_PANEL);
        labels.add(startIndex, getDefaultLabel(APP_LAUNCHER));
        packages.add(startIndex++, APP_LAUNCHER);
        labels.add(startIndex, getDefaultLabel(KOREADER_HISTORY));
        packages.add(startIndex++, KOREADER_HISTORY);
        labels.add(startIndex, getDefaultLabel(CALENDAR));
        packages.add(startIndex++, CALENDAR);
        labels.add(startIndex, getDefaultLabel(GALLERY));
        packages.add(startIndex++, GALLERY);
        labels.add(startIndex, getDefaultLabel(CLOCK));
        packages.add(startIndex++, CLOCK);
        labels.add(startIndex, getDefaultLabel(CALCULATOR));
        packages.add(startIndex++, CALCULATOR);
        return startIndex;
    }

    public static int insertSelectorApps(java.util.List<String> labels, java.util.List<String> packages, int specialIndex, int position, boolean isBigme) {
        labels.add(specialIndex, getDefaultLabel(LAUNCHER_SETTINGS));
        packages.add(specialIndex, LAUNCHER_SETTINGS);
        int next = specialIndex + 1;
        if (position == -2) return next;
        labels.add(next, getDefaultLabel(NOTIFICATION_PANEL));
        packages.add(next++, NOTIFICATION_PANEL);
        labels.add(next, getDefaultLabel(APP_LAUNCHER));
        packages.add(next++, APP_LAUNCHER);
        labels.add(next, getDefaultLabel(KOREADER_HISTORY));
        packages.add(next++, KOREADER_HISTORY);
        labels.add(next, getDefaultLabel(CALENDAR));
        packages.add(next++, CALENDAR);
        labels.add(next, getDefaultLabel(GALLERY));
        packages.add(next++, GALLERY);
        labels.add(next, getDefaultLabel(CLOCK));
        packages.add(next++, CLOCK);
        labels.add(next, getDefaultLabel(CALCULATOR));
        packages.add(next++, CALCULATOR);
        if (isBigme) {
            labels.add(next, "Bigme Control Panel");
            packages.add(next++, "bigme_control_panel");
        }
        if (position == -1) {
            labels.add(next, "Next Home Page");
            packages.add(next++, "next_home_page");
            labels.add(next, "Previous Home Page");
            packages.add(next++, "previous_home_page");
        }
        return next;
    }
}
