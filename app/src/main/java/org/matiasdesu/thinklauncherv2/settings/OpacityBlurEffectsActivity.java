package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class OpacityBlurEffectsActivity extends BaseSettingsActivity {

    private int appLauncherBgOpacityEnabled;
    private int appLauncherBgOpacity;
    private int appLauncherBgBlurEnabled;
    private int appLauncherBgBlurStrength;

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
    protected int getLayoutResId() {
        return R.layout.activity_opacity_blur_effects;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);

        appLauncherBgOpacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0);
        appLauncherBgOpacity = prefs.getInt("app_launcher_bg_opacity", 100);
        appLauncherBgBlurEnabled = prefs.getInt("app_launcher_bg_blur_enabled", 0);
        appLauncherBgBlurStrength = prefs.getInt("app_launcher_bg_blur_strength", 3);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;

        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

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
                new String[] { "1", "10" }));

        ImageButton minusEnabledBtn = enabledContainer.findViewById(R.id.btn_minus);
        ImageButton plusEnabledBtn = enabledContainer.findViewById(R.id.btn_plus);
        ImageButton minusOpacityBtn = opacityContainer.findViewById(R.id.btn_minus);
        ImageButton plusOpacityBtn = opacityContainer.findViewById(R.id.btn_plus);
        ImageButton minusBlurBtn = blurContainer.findViewById(R.id.btn_minus);
        ImageButton plusBlurBtn = blurContainer.findViewById(R.id.btn_plus);
        ImageButton minusBlurStrengthBtn = blurStrengthContainer.findViewById(R.id.btn_minus);
        ImageButton plusBlurStrengthBtn = blurStrengthContainer.findViewById(R.id.btn_plus);

        minusEnabledBtn.setOnClickListener(v -> {
            appLauncherBgOpacityEnabled = (appLauncherBgOpacityEnabled - 1 + 2) % 2;
            enabledValueTv.setText(getOnOffText(appLauncherBgOpacityEnabled));
            prefs.edit().putInt("app_launcher_bg_opacity_enabled", appLauncherBgOpacityEnabled).apply();
            refreshVisibility();
            refreshPagination();
        });

        plusEnabledBtn.setOnClickListener(v -> {
            appLauncherBgOpacityEnabled = (appLauncherBgOpacityEnabled + 1) % 2;
            enabledValueTv.setText(getOnOffText(appLauncherBgOpacityEnabled));
            prefs.edit().putInt("app_launcher_bg_opacity_enabled", appLauncherBgOpacityEnabled).apply();
            refreshVisibility();
            refreshPagination();
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
            refreshPagination();
        });

        minusBlurStrengthBtn.setOnClickListener(v -> {
            if (appLauncherBgBlurStrength > 1) {
                appLauncherBgBlurStrength--;
                blurStrengthValueTv.setText(String.valueOf(appLauncherBgBlurStrength));
                prefs.edit().putInt("app_launcher_bg_blur_strength", appLauncherBgBlurStrength).apply();
            }
        });

        plusBlurStrengthBtn.setOnClickListener(v -> {
            if (appLauncherBgBlurStrength < 10) {
                appLauncherBgBlurStrength++;
                blurStrengthValueTv.setText(String.valueOf(appLauncherBgBlurStrength));
                prefs.edit().putInt("app_launcher_bg_blur_strength", appLauncherBgBlurStrength).apply();
            }
        });

        initPagination(this::refreshVisibility);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }
}

