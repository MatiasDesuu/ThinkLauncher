package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
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

        findViewById(R.id.home_screen_settings_button).setOnClickListener(v -> open(HomeScreenSettingsActivity.class));
        findViewById(R.id.appearance_settings_button).setOnClickListener(v -> open(AppearanceSettingsActivity.class));
        findViewById(R.id.docks_settings_button).setOnClickListener(v -> open(DocksSettingsActivity.class));
        findViewById(R.id.controls_settings_button).setOnClickListener(v -> open(ControlsSettingsActivity.class));
        findViewById(R.id.app_launcher_settings_button).setOnClickListener(v -> open(AppLauncherSettingsActivity.class));
        findViewById(R.id.system_settings_button).setOnClickListener(v -> open(SystemSettingsActivity.class));

        initPagination(null);
    }

    private void open(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        if (!screenAnimations) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
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
