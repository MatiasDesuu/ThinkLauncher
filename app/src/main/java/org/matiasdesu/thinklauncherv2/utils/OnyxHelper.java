package org.matiasdesu.thinklauncherv2.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.GuardedDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OnyxHelper {

    private OnyxHelper() {}

    public static final String ONYX_NOTES = "onyx_notes";
    public static final String ONYX_LIBRARY = "onyx_library";
    public static final String ONYX_STORAGE = "onyx_storage";
    public static final String ONYX_STORE = "onyx_store";
    public static final String ONYX_SETTINGS = "onyx_settings";

    private static final String ONYX_LAUNCHER_PACKAGE = "com.onyx";

    private static final String PREF_ONYX_FREEZE_DISMISSED = "onyx_freeze_notice_dismissed";

    public static boolean isOnyxDevice() {
        String brand = safeLower(Build.BRAND);
        String manufacturer = safeLower(Build.MANUFACTURER);
        String model = safeLower(Build.MODEL);
        String fingerprint = safeLower(Build.FINGERPRINT);
        String product = safeLower(Build.PRODUCT);
        String device = safeLower(Build.DEVICE);
        String board = safeLower(Build.BOARD);
        if (brand.contains("onyx") || manufacturer.contains("onyx")
                || fingerprint.contains("onyx") || product.contains("onyx") || device.contains("onyx")) {
            return true;
        }
        if (brand.contains("boox") || manufacturer.contains("boox") || model.contains("boox")
                || product.contains("boox") || board.contains("boox")) {
            return true;
        }
        if (fingerprint.contains("boox")) {
            return true;
        }
        return false;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    public static boolean isAppFrozen(Context context, String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        if (isOnyxPseudoPackage(pkg)) return false;
        if (pkg.startsWith("folder_") || pkg.startsWith("webapp_") || pkg.startsWith("hidden_app_")) return false;
        if (SystemAppHelper.isSystemApp(pkg)) return false;
        if (pkg.equals("blank") || pkg.equals("system_default")) return false;
        try {
            PackageManager pm = context.getPackageManager();
            int state = pm.getApplicationEnabledSetting(pkg);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
        } catch (Exception e) {
            return false;
        }
    }

    public static void showFreezeHelpDialog(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        GuardedDialog dialog = new GuardedDialog(context, R.style.NoAnimationDialog);
        View content = createFreezeHelpView(dialog, context, theme, null);
        dialog.setContentView(content);
        DialogEffectHelper.setup(dialog, theme);
        FontHelper.applyToViewTree(context, dialog.findViewById(android.R.id.content));
        dialog.show();
    }

    public static boolean handleFrozenAppIfNeeded(Context context, String pkg, int theme) {
        if (!isOnyxDevice()) return false;
        if (!isAppFrozen(context, pkg)) return false;
        showFrozenAppDialog(context, pkg, theme);
        return true;
    }

    public static void showFrozenAppDialog(Context context, String pkg, int theme) {
        GuardedDialog dialog = new GuardedDialog(context, R.style.NoAnimationDialog);
        View content = createFreezeHelpView(dialog, context, theme, pkg);
        dialog.setContentView(content);
        DialogEffectHelper.setup(dialog, theme);
        FontHelper.applyToViewTree(context, dialog.findViewById(android.R.id.content));
        dialog.show();
    }

    private static View createFreezeHelpView(Dialog dialog, Context context, int theme, String frozenPkg) {
        int textColor = ThemeUtils.getTextColor(theme, context);
        int surfaceColor = ThemeUtils.getBgColor(theme, context);
        SharedPreferences p = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (p.getInt("app_launcher_bg_opacity_enabled", 0) == 1) {
            surfaceColor = WallpaperHelper.applyOpacity(surfaceColor, p.getInt("app_launcher_bg_opacity", 100));
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                (int) (320 * context.getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(containerParams);
        int surfaceColorTmp = ThemeUtils.getBgColor(theme, context);
        SharedPreferences pTmp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (pTmp.getInt("app_launcher_bg_opacity_enabled", 0) == 1) {
            surfaceColorTmp = WallpaperHelper.applyOpacity(surfaceColorTmp, pTmp.getInt("app_launcher_bg_opacity", 100));
        }
        DialogEffectHelper.applySurface(container, theme, context, surfaceColorTmp);

        TextView title = new TextView(context);
        title.setText(frozenPkg != null ? "App is frozen" : "BOOX App Freeze");
        title.setTextColor(textColor);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        try {
            title.setTypeface(null, android.graphics.Typeface.BOLD);
        } catch (Exception ignored) {}
        container.addView(title);

        TextView message = new TextView(context);
        message.setTextColor(textColor);
        message.setTextSize(14);
        message.setLineSpacing(0, 1.1f);
        int msgTop = (int) (12 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgParams.topMargin = msgTop;
        msgParams.bottomMargin = (int) (16 * context.getResources().getDisplayMetrics().density);
        message.setLayoutParams(msgParams);
        if (frozenPkg != null) {
            String appName = frozenPkg;
            try {
                PackageManager pm = context.getPackageManager();
                CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(frozenPkg, 0));
                if (label != null) appName = label.toString();
            } catch (Exception ignored) {}
            message.setText(
                    "\"" + appName + "\" is frozen by Onyx BOOX system and cannot be launched from a third-party launcher.\n\n"
                    + "To unfreeze:\n"
                    + "1. Switch back to the stock Onyx launcher\n"
                    + "2. Open \"Apps\", tap the snowflake ❄️ icon (top right)\n"
                    + "3. Disable \"Auto freeze\" and \"Automatic app freezing after installation\"\n"
                    + "4. Unfreeze all apps in the list, including this one\n\n"
                    + "Tip: You can also temporarily unfreeze an app by opening it once from the stock launcher."
            );
        } else {
            message.setText(
                    "Onyx BOOX devices have an \"App Freeze\" feature that disables apps automatically.\n\n"
                    + "• Frozen apps can't be launched from ThinkLauncher and may not appear in the app list.\n"
                    + "• Newly installed apps are frozen by default.\n\n"
                    + "To fix:\n"
                    + "1. Switch back to the stock Onyx launcher\n"
                    + "2. Open \"Apps\" → tap snowflake ❄️ (top right)\n"
                    + "3. Disable \"Auto freeze\" and \"Automatic app freezing after installation\"\n"
                    + "4. Unfreeze all apps in the list\n\n"
                    + "Tip: Opening an app once from the stock launcher also unfreezes it."
            );
        }
        container.addView(message);

        LinearLayout buttonRow = new LinearLayout(context);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        buttonRow.setBaselineAligned(false);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonRow.setLayoutParams(rowParams);

        TextView dismissBtn = new TextView(context);
        dismissBtn.setText("Got it");
        dismissBtn.setGravity(Gravity.CENTER);
        dismissBtn.setTextSize(16);
        dismissBtn.setPadding(pad/2, pad/2, pad/2, pad/2);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        btnParams.rightMargin = pad/2;
        dismissBtn.setLayoutParams(btnParams);
        DialogEffectHelper.applyButtonTheme(dismissBtn, theme, context, surfaceColor);
        dismissBtn.setOnClickListener(v -> dialog.dismiss());
        buttonRow.addView(dismissBtn);

        TextView dontShowBtn = new TextView(context);
        dontShowBtn.setText("Don't show again");
        dontShowBtn.setGravity(Gravity.CENTER);
        dontShowBtn.setTextSize(16);
        dontShowBtn.setPadding(pad/2, pad/2, pad/2, pad/2);
        LinearLayout.LayoutParams btn2Params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        btn2Params.leftMargin = pad/2;
        dontShowBtn.setLayoutParams(btn2Params);
        DialogEffectHelper.applyButtonTheme(dontShowBtn, theme, context, surfaceColor);
        dontShowBtn.setOnClickListener(v -> {
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean(PREF_ONYX_FREEZE_DISMISSED, true).apply();
            dialog.dismiss();
        });
        buttonRow.addView(dontShowBtn);

        container.addView(buttonRow);

        return container;
    }

    public static void showFreezeNoticeIfNeeded(Context context) {
        if (!isOnyxDevice()) return;
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_ONYX_FREEZE_DISMISSED, false)) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                () -> {
                    try {
                        showFreezeHelpDialog(context);
                    } catch (Exception ignored) {}
                }, 600);
    }

    public static boolean isOnyxPseudoPackage(String pkg) {
        return ONYX_NOTES.equals(pkg) || ONYX_LIBRARY.equals(pkg) || ONYX_STORAGE.equals(pkg)
                || ONYX_STORE.equals(pkg) || ONYX_SETTINGS.equals(pkg);
    }

    public static Intent getOnyxIntent(String pseudo) {
        if (pseudo == null) return null;
        switch (pseudo) {
            case ONYX_NOTES:
                return new Intent("com.onyx.intent.action.MAIN_ACTIVITY").putExtra("json", "{\"action\":\"OPEN_NOTE\"}");
            case ONYX_LIBRARY:
                return new Intent("com.onyx.action.LIBRARY");
            case ONYX_STORAGE:
                return new Intent("com.onyx.action.STORAGE");
            case ONYX_STORE:
                return new Intent("com.onyx.action.SHOP");
            case ONYX_SETTINGS:
                return new Intent("com.onyx.action.SETTING");
            default:
                return null;
        }
    }

    public static String getOnyxLabel(String pseudo) {
        if (pseudo == null) return "";
        switch (pseudo) {
            case ONYX_NOTES: return "BOOX Notes";
            case ONYX_LIBRARY: return "BOOX Library";
            case ONYX_STORAGE: return "BOOX Storage";
            case ONYX_STORE: return "BOOX Store";
            case ONYX_SETTINGS: return "BOOX Settings";
            default: return pseudo;
        }
    }

    public static Drawable getOnyxIcon(Context context, String pseudo) {
        String iconName = null;
        switch (pseudo) {
            case ONYX_NOTES: iconName = "app_note"; break;
            case ONYX_LIBRARY: iconName = "app_library"; break;
            case ONYX_STORAGE: iconName = "app_storage"; break;
            case ONYX_STORE: iconName = "app_shop"; break;
            case ONYX_SETTINGS: iconName = "app_setting"; break;
        }
        if (iconName != null) {
            try {
                PackageManager pm = context.getPackageManager();
                Resources res = pm.getResourcesForApplication(ONYX_LAUNCHER_PACKAGE);
                int resId = res.getIdentifier(iconName, "drawable", ONYX_LAUNCHER_PACKAGE);
                if (resId != 0) {
                    Drawable d = ResourcesCompat.getDrawable(res, resId, null);
                    if (d != null) return d;
                }
            } catch (Exception ignored) {}
        }
        try {
            return context.getDrawable(R.drawable.generic_app);
        } catch (Exception e) {
            return new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT);
        }
    }

    public static boolean isOnyxAppAvailable(Context context, String pseudo) {
        Intent intent = getOnyxIntent(pseudo);
        if (intent == null) return false;
        try {
            ResolveInfo ri = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<AppSearchHelper.AppItem> getAvailableOnyxApps(Context context) {
        List<AppSearchHelper.AppItem> list = new ArrayList<>();
        String[] pseudos = new String[]{ONYX_NOTES, ONYX_LIBRARY, ONYX_STORAGE, ONYX_STORE, ONYX_SETTINGS};
        for (String pseudo : pseudos) {
            if (isOnyxAppAvailable(context, pseudo)) {
                list.add(new AppSearchHelper.AppItem(getOnyxLabel(pseudo), pseudo));
            }
        }
        return list;
    }

    public static boolean launchOnyxApp(Context context, String pseudo) {
        Intent intent = getOnyxIntent(pseudo);
        if (intent == null) return false;
        try {
            PackageManager pm = context.getPackageManager();
            ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri == null) return false;
        } catch (Exception ignored) {
            return false;
        }
        try {
            SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
            boolean animate = prefs.getInt("app_launch_animation", 0) == 1;
            if (!animate) intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            if (context instanceof android.app.Activity) {
                if (animate) ((android.app.Activity) context).overridePendingTransition(R.anim.dialog_fade_in, 0);
                else ((android.app.Activity) context).overridePendingTransition(0, 0);
            }
            return true;
        } catch (Exception e) {
            try {
                Intent fallback = getOnyxIntent(pseudo);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
