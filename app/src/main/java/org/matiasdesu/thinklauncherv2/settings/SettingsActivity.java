package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.HideAppsActivity;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;

import android.os.Build;

public class SettingsActivity extends AppCompatActivity {

    private int theme;
    private int customBgColor;
    private int customAccentColor;
    private LinearLayout rootLayout;
    private SharedPreferences prefs;
    private SettingsPaginationHelper paginationHelper;

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
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        customBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        customAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);
        if (!prefs.contains("theme")) {
            prefs.edit().putInt("theme", 0).apply();
        }
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        int textColor = ThemeUtils.getTextColor(theme, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!ThemeUtils.isDarkTheme(theme, this)) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(0);
            }
        }

        rootLayout = findViewById(R.id.root_layout);
        rootLayout.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(rootLayout, theme, this);

        theme = prefs.getInt("theme", 0);

        // Set divider colors
        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(textColor);
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(textColor);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        LinearLayout themeSettingsButton = findViewById(R.id.theme_settings_button);
        themeSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemeSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout gestureSettingsButton = findViewById(R.id.gesture_settings_button);
        gestureSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, GestureSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout hideAppsButton = findViewById(R.id.hide_apps_button);
        hideAppsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HideAppsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout homeSettingsButton = findViewById(R.id.home_settings_button);
        homeSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout displaySettingsButton = findViewById(R.id.display_settings_button);
        displaySettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DisplaySettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout defaultLauncherButton = findViewById(R.id.default_launcher_button);
        defaultLauncherButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout accessibilitySettingsButton = findViewById(R.id.accessibility_settings_button);
        accessibilitySettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout deviceAdminSettingsButton = findViewById(R.id.device_admin_settings_button);
        deviceAdminSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.DeviceAdminSettings");
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout appSettingsButton = findViewById(R.id.app_settings_button);
        appSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Initialize pagination
        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);
        
        paginationHelper = new SettingsPaginationHelper(this, theme, 
            settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), Context.RECEIVER_NOT_EXPORTED);
        
        int newTheme = prefs.getInt("theme", 0);
        int newCustomBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        int newCustomAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);

        if (newTheme != theme || (newTheme == ThemeUtils.THEME_CUSTOM && 
            (newCustomBgColor != customBgColor || newCustomAccentColor != customAccentColor))) {
            restartActivity();
            return;
        }

        // Check if scroll_app_list setting has changed
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
    }

    private void restartActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }
}
