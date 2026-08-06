package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.HideAppsActivity;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class SettingsActivity extends BaseSettingsActivity {

    private int customBgColor;
    private int customAccentColor;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    // Bring MainActivity to front
                    Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        customAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;
        if (!prefs.contains("theme")) {
            prefs.edit().putInt("theme", 0).apply();
        }

        int bgColor = ThemeUtils.getBgColor(theme, this);
        int textColor = ThemeUtils.getTextColor(theme, this);

        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        theme = prefs.getInt("theme", 0);

        // Set divider colors
        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(textColor);
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(textColor);

        LinearLayout themeSettingsButton = findViewById(R.id.theme_settings_button);
        themeSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemeSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout gestureSettingsButton = findViewById(R.id.gesture_settings_button);
        gestureSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, GestureSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout hardwareKeysButton = findViewById(R.id.hardware_keys_button);
        hardwareKeysButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HardwareKeysSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout hideAppsButton = findViewById(R.id.hide_apps_button);
        hideAppsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HideAppsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout homeSettingsButton = findViewById(R.id.home_settings_button);
        homeSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout displaySettingsButton = findViewById(R.id.display_settings_button);
        displaySettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DisplaySettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout docksSettingsButton = findViewById(R.id.docks_settings_button);
        docksSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DocksSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout defaultLauncherButton = findViewById(R.id.default_launcher_button);
        defaultLauncherButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout accessibilitySettingsButton = findViewById(R.id.accessibility_settings_button);
        accessibilitySettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout deviceAdminSettingsButton = findViewById(R.id.device_admin_settings_button);
        deviceAdminSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.DeviceAdminSettings");
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout appSettingsButton = findViewById(R.id.app_settings_button);
        appSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout githubRepoButton = findViewById(R.id.github_repo_button);
        githubRepoButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/MatiasDesuu/ThinkLauncher"));
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        initPagination(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        int newTheme = prefs.getInt("theme", 0);
        int newCustomBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        int newCustomAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);

        if (newTheme != theme || (newTheme == ThemeUtils.THEME_CUSTOM &&
                (newCustomBgColor != customBgColor || newCustomAccentColor != customAccentColor))) {
            restartActivity();
        }
    }

    private void restartActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        if (!screenAnimations) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, screenAnimations ? R.anim.dialog_fade_out : 0);
    }
}
