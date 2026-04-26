package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class OpacityBlurEffectsActivity extends AppCompatActivity {

    private int appLauncherBgOpacityEnabled;
    private int appLauncherBgOpacity;
    private int appLauncherBgBlurEnabled;
    private int appLauncherBgBlurStrength;
    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;

    private final BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(OpacityBlurEffectsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        int bgColor = ThemeUtils.getBgColor(theme, this);

        appLauncherBgOpacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0);
        appLauncherBgOpacity = prefs.getInt("app_launcher_bg_opacity", 100);
        appLauncherBgBlurEnabled = prefs.getInt("app_launcher_bg_blur_enabled", 0);
        appLauncherBgBlurStrength = prefs.getInt("app_launcher_bg_blur_strength", 3);

        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opacity_blur_effects);

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

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View enabledContainer = findViewById(R.id.app_launcher_bg_opacity_enabled_container);
        TextView enabledValueTv = enabledContainer.findViewById(R.id.value_text);
        enabledValueTv.setText(getOnOffText(appLauncherBgOpacityEnabled));
        enabledValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(enabledValueTv, new String[] { "OFF", "ON" }));

        View opacityContainer = findViewById(R.id.app_launcher_bg_opacity_container);
        TextView opacityValueTv = opacityContainer.findViewById(R.id.value_text);
        opacityValueTv.setText(String.valueOf(appLauncherBgOpacity));
        opacityValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(opacityValueTv, new String[] { "0", "100" }));

        View blurContainer = findViewById(R.id.app_launcher_bg_blur_container);
        TextView blurValueTv = blurContainer.findViewById(R.id.value_text);
        blurValueTv.setText(getOnOffText(appLauncherBgBlurEnabled));
        blurValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(blurValueTv, new String[] { "OFF", "ON" }));

        View blurStrengthContainer = findViewById(R.id.app_launcher_bg_blur_strength_container);
        TextView blurStrengthValueTv = blurStrengthContainer.findViewById(R.id.value_text);
        blurStrengthValueTv.setText(String.valueOf(appLauncherBgBlurStrength));
        blurStrengthValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(blurStrengthValueTv,
                new String[] { "1", "5" }));

        TextView minusEnabledBtn = enabledContainer.findViewById(R.id.btn_minus);
        TextView plusEnabledBtn = enabledContainer.findViewById(R.id.btn_plus);
        TextView minusOpacityBtn = opacityContainer.findViewById(R.id.btn_minus);
        TextView plusOpacityBtn = opacityContainer.findViewById(R.id.btn_plus);
        TextView minusBlurBtn = blurContainer.findViewById(R.id.btn_minus);
        TextView plusBlurBtn = blurContainer.findViewById(R.id.btn_plus);
        TextView minusBlurStrengthBtn = blurStrengthContainer.findViewById(R.id.btn_minus);
        TextView plusBlurStrengthBtn = blurStrengthContainer.findViewById(R.id.btn_plus);

        minusEnabledBtn.setOnClickListener(v -> {
            appLauncherBgOpacityEnabled = (appLauncherBgOpacityEnabled - 1 + 2) % 2;
            enabledValueTv.setText(getOnOffText(appLauncherBgOpacityEnabled));
            prefs.edit().putInt("app_launcher_bg_opacity_enabled", appLauncherBgOpacityEnabled).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusEnabledBtn.setOnClickListener(v -> {
            appLauncherBgOpacityEnabled = (appLauncherBgOpacityEnabled + 1) % 2;
            enabledValueTv.setText(getOnOffText(appLauncherBgOpacityEnabled));
            prefs.edit().putInt("app_launcher_bg_opacity_enabled", appLauncherBgOpacityEnabled).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusOpacityBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (appLauncherBgOpacity > 0) {
                appLauncherBgOpacity--;
                opacityValueTv.setText(String.valueOf(appLauncherBgOpacity));
                prefs.edit().putInt("app_launcher_bg_opacity", appLauncherBgOpacity).apply();
            }
        }));

        plusOpacityBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (appLauncherBgOpacity < 100) {
                appLauncherBgOpacity++;
                opacityValueTv.setText(String.valueOf(appLauncherBgOpacity));
                prefs.edit().putInt("app_launcher_bg_opacity", appLauncherBgOpacity).apply();
            }
        }));

        minusBlurBtn.setOnClickListener(v -> {
            appLauncherBgBlurEnabled = (appLauncherBgBlurEnabled - 1 + 2) % 2;
            blurValueTv.setText(getOnOffText(appLauncherBgBlurEnabled));
            prefs.edit().putInt("app_launcher_bg_blur_enabled", appLauncherBgBlurEnabled).apply();
        });

        plusBlurBtn.setOnClickListener(v -> {
            appLauncherBgBlurEnabled = (appLauncherBgBlurEnabled + 1) % 2;
            blurValueTv.setText(getOnOffText(appLauncherBgBlurEnabled));
            prefs.edit().putInt("app_launcher_bg_blur_enabled", appLauncherBgBlurEnabled).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusBlurStrengthBtn.setOnClickListener(v -> {
            if (appLauncherBgBlurStrength > 1) {
                appLauncherBgBlurStrength--;
                blurStrengthValueTv.setText(String.valueOf(appLauncherBgBlurStrength));
                prefs.edit().putInt("app_launcher_bg_blur_strength", appLauncherBgBlurStrength).apply();
            }
        });

        plusBlurStrengthBtn.setOnClickListener(v -> {
            if (appLauncherBgBlurStrength < 5) {
                appLauncherBgBlurStrength++;
                blurStrengthValueTv.setText(String.valueOf(appLauncherBgBlurStrength));
                prefs.edit().putInt("app_launcher_bg_blur_strength", appLauncherBgBlurStrength).apply();
            }
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);

        refreshVisibility();
    }

    private void refreshVisibility() {
        LinearLayout opacityLayout = findViewById(R.id.app_launcher_bg_opacity_layout);
        LinearLayout blurLayout = findViewById(R.id.app_launcher_bg_blur_layout);
        LinearLayout blurStrengthLayout = findViewById(R.id.app_launcher_bg_blur_strength_layout);
        opacityLayout.setVisibility(appLauncherBgOpacityEnabled == 1 ? View.VISIBLE : View.GONE);
        blurLayout.setVisibility(appLauncherBgOpacityEnabled == 1 ? View.VISIBLE : View.GONE);
        blurStrengthLayout.setVisibility(appLauncherBgOpacityEnabled == 1 && appLauncherBgBlurEnabled == 1
                ? View.VISIBLE
                : View.GONE);
    }

    private String getOnOffText(int pos) {
        return pos == 1 ? "ON" : "OFF";
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
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

