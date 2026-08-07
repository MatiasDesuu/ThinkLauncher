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
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class DisplaySettingsActivity extends BaseSettingsActivity {

    private int scrollAppList;
    private int appIndexSidebar;
    private int appIndexAnimation;
    private boolean autoFocusSearch;
    private int einkRefreshEnabled;
    private int einkRefreshDelay;
    private int webappPwaMode;
    private int modalCornerRadius;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {

                    Intent mainIntent = new Intent(DisplaySettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_display_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        scrollAppList = prefs.getInt("scroll_app_list", 0);
        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        appIndexAnimation = prefs.getInt("app_index_animation", 0);
        autoFocusSearch = prefs.getBoolean("auto_focus_search", true);
        einkRefreshEnabled = prefs.getInt("eink_refresh_enabled", 0);
        einkRefreshDelay = prefs.getInt("eink_refresh_delay", 100);
        webappPwaMode = prefs.getInt("webapp_pwa_mode", 0);
        modalCornerRadius = prefs.getInt("modal_corner_radius", 0);

        View modalCornerRadiusContainer = findViewById(R.id.modal_corner_radius_container);
        TextView modalCornerRadiusValueTv = modalCornerRadiusContainer.findViewById(R.id.value_text);
        modalCornerRadiusValueTv.setText(String.valueOf(modalCornerRadius));

        ImageButton minusModalCornerRadiusBtn = modalCornerRadiusContainer.findViewById(R.id.btn_minus);
        ImageButton plusModalCornerRadiusBtn = modalCornerRadiusContainer.findViewById(R.id.btn_plus);

        minusModalCornerRadiusBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (modalCornerRadius >= 2) {
                modalCornerRadius -= 2;
                modalCornerRadiusValueTv.setText(String.valueOf(modalCornerRadius));
                prefs.edit().putInt("modal_corner_radius", modalCornerRadius).apply();
                updateCornerRadius();
            }
        }));

        plusModalCornerRadiusBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (modalCornerRadius <= 30) {
                modalCornerRadius += 2;
                modalCornerRadiusValueTv.setText(String.valueOf(modalCornerRadius));
                prefs.edit().putInt("modal_corner_radius", modalCornerRadius).apply();
                updateCornerRadius();
            }
        }));

        View scrollAppListContainer = findViewById(R.id.scroll_app_list_container);
        TextView scrollAppListValueTv = scrollAppListContainer.findViewById(R.id.value_text);
        scrollAppListValueTv.setText(getOnOffText(scrollAppList));
        scrollAppListValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(scrollAppListValueTv, new String[] { "OFF", "ON" }));

        View autoFocusContainer = findViewById(R.id.autofocus_container);
        TextView autoFocusValueTv = autoFocusContainer.findViewById(R.id.value_text);
        autoFocusValueTv.setText(autoFocusSearch ? "ON" : "OFF");
        autoFocusValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(autoFocusValueTv, new String[] { "OFF", "ON" }));

        View einkRefreshEnabledContainer = findViewById(R.id.eink_refresh_enabled_container);
        TextView einkRefreshEnabledValueTv = einkRefreshEnabledContainer.findViewById(R.id.value_text);
        einkRefreshEnabledValueTv.setText(getOnOffText(einkRefreshEnabled));
        einkRefreshEnabledValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(einkRefreshEnabledValueTv, new String[] { "OFF", "ON" }));

        View einkRefreshDelayContainer = findViewById(R.id.eink_refresh_delay_container);
        TextView einkRefreshDelayValueTv = einkRefreshDelayContainer.findViewById(R.id.value_text);
        einkRefreshDelayValueTv.setText(String.valueOf(einkRefreshDelay));

        ImageButton minusScrollAppListBtn = scrollAppListContainer.findViewById(R.id.btn_minus);
        ImageButton plusScrollAppListBtn = scrollAppListContainer.findViewById(R.id.btn_plus);

        ImageButton minusAutoFocusBtn = autoFocusContainer.findViewById(R.id.btn_minus);
        ImageButton plusAutoFocusBtn = autoFocusContainer.findViewById(R.id.btn_plus);

        ImageButton minusEinkRefreshEnabledBtn = einkRefreshEnabledContainer.findViewById(R.id.btn_minus);
        ImageButton plusEinkRefreshEnabledBtn = einkRefreshEnabledContainer.findViewById(R.id.btn_plus);

        ImageButton minusEinkRefreshDelayBtn = einkRefreshDelayContainer.findViewById(R.id.btn_minus);
        ImageButton plusEinkRefreshDelayBtn = einkRefreshDelayContainer.findViewById(R.id.btn_plus);

        minusScrollAppListBtn.setOnClickListener(v -> {
            scrollAppList = (scrollAppList - 1 + 2) % 2;
            scrollAppListValueTv.setText(getOnOffText(scrollAppList));
            prefs.edit().putInt("scroll_app_list", scrollAppList).apply();
            refreshPagination();
        });

        plusScrollAppListBtn.setOnClickListener(v -> {
            scrollAppList = (scrollAppList + 1) % 2;
            scrollAppListValueTv.setText(getOnOffText(scrollAppList));
            prefs.edit().putInt("scroll_app_list", scrollAppList).apply();
            refreshPagination();
        });

        minusAutoFocusBtn.setOnClickListener(v -> {
            autoFocusSearch = !autoFocusSearch;
            autoFocusValueTv.setText(autoFocusSearch ? "ON" : "OFF");
            prefs.edit().putBoolean("auto_focus_search", autoFocusSearch).apply();
        });

        plusAutoFocusBtn.setOnClickListener(v -> {
            autoFocusSearch = !autoFocusSearch;
            autoFocusValueTv.setText(autoFocusSearch ? "ON" : "OFF");
            prefs.edit().putBoolean("auto_focus_search", autoFocusSearch).apply();
        });

        View appIndexSidebarContainer = findViewById(R.id.app_index_sidebar_container);
        TextView appIndexSidebarValueTv = appIndexSidebarContainer.findViewById(R.id.value_text);
        appIndexSidebarValueTv.setText(getOnOffText(appIndexSidebar));
        appIndexSidebarValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(appIndexSidebarValueTv, new String[]{"OFF", "ON"}));

        ImageButton minusAppIndexSidebarBtn = appIndexSidebarContainer.findViewById(R.id.btn_minus);
        ImageButton plusAppIndexSidebarBtn = appIndexSidebarContainer.findViewById(R.id.btn_plus);

        minusAppIndexSidebarBtn.setOnClickListener(v -> {
            appIndexSidebar = (appIndexSidebar - 1 + 2) % 2;
            appIndexSidebarValueTv.setText(getOnOffText(appIndexSidebar));
            prefs.edit().putInt("app_index_sidebar", appIndexSidebar).apply();
            refreshPagination();
        });

        plusAppIndexSidebarBtn.setOnClickListener(v -> {
            appIndexSidebar = (appIndexSidebar + 1) % 2;
            appIndexSidebarValueTv.setText(getOnOffText(appIndexSidebar));
            prefs.edit().putInt("app_index_sidebar", appIndexSidebar).apply();
            refreshPagination();
        });

        View webappPwaContainer = findViewById(R.id.webapp_pwa_container);
        TextView webappPwaValueTv = webappPwaContainer.findViewById(R.id.value_text);
        webappPwaValueTv.setText(getOnOffText(webappPwaMode));
        webappPwaValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(webappPwaValueTv, new String[]{"OFF", "ON"}));

        ImageButton minusWebappPwaBtn = webappPwaContainer.findViewById(R.id.btn_minus);
        ImageButton plusWebappPwaBtn = webappPwaContainer.findViewById(R.id.btn_plus);

        minusWebappPwaBtn.setOnClickListener(v -> {
            webappPwaMode = (webappPwaMode - 1 + 2) % 2;
            webappPwaValueTv.setText(getOnOffText(webappPwaMode));
            prefs.edit().putInt("webapp_pwa_mode", webappPwaMode).apply();
        });

        plusWebappPwaBtn.setOnClickListener(v -> {
            webappPwaMode = (webappPwaMode + 1) % 2;
            webappPwaValueTv.setText(getOnOffText(webappPwaMode));
            prefs.edit().putInt("webapp_pwa_mode", webappPwaMode).apply();
        });

        LinearLayout animationSettingsButton = findViewById(R.id.animation_settings_button);
        animationSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimationSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        minusEinkRefreshEnabledBtn.setOnClickListener(v -> {
            einkRefreshEnabled = (einkRefreshEnabled - 1 + 2) % 2;
            einkRefreshEnabledValueTv.setText(getOnOffText(einkRefreshEnabled));
            prefs.edit().putInt("eink_refresh_enabled", einkRefreshEnabled).apply();
            updateEinkRefreshDelayVisibility();
            refreshPagination();
        });

        plusEinkRefreshEnabledBtn.setOnClickListener(v -> {
            einkRefreshEnabled = (einkRefreshEnabled + 1) % 2;
            einkRefreshEnabledValueTv.setText(getOnOffText(einkRefreshEnabled));
            prefs.edit().putInt("eink_refresh_enabled", einkRefreshEnabled).apply();
            updateEinkRefreshDelayVisibility();
            refreshPagination();
        });

        minusEinkRefreshDelayBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (einkRefreshDelay > 100) {
                einkRefreshDelay -= 100;
                einkRefreshDelayValueTv.setText(String.valueOf(einkRefreshDelay));
                prefs.edit().putInt("eink_refresh_delay", einkRefreshDelay).apply();
            }
        }));

        plusEinkRefreshDelayBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (einkRefreshDelay < 1000) {
                einkRefreshDelay += 100;
                einkRefreshDelayValueTv.setText(String.valueOf(einkRefreshDelay));
                prefs.edit().putInt("eink_refresh_delay", einkRefreshDelay).apply();
            }
        }));

        updateEinkRefreshDelayVisibility();

        LinearLayout homePaddingButton = findViewById(R.id.home_screen_padding_button);
        homePaddingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeScreenPaddingActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        initPagination(this::refreshVisibility);

        findViewById(R.id.time_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, TimeSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.date_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, DateSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.settings_button_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsButtonSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.search_button_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchButtonSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.opacity_blur_effects_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, OpacityBlurEffectsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.font_sizes_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, FontSizesSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.custom_font_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, FontSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });
    }

    private void refreshVisibility() {
        updateEinkRefreshDelayVisibility();
    }

    private void updateCornerRadius() {
        DialogEffectHelper.applyCornerRadiusToTree(findViewById(R.id.root_layout), this);
    }

    private String getOnOffText(int pos) {
        switch (pos) {
            case 0:
                return "OFF";
            case 1:
                return "ON";
            default:
                return "OFF";
        }
    }

    private void updateEinkRefreshDelayVisibility() {
        LinearLayout einkRefreshDelayLayout = findViewById(R.id.eink_refresh_delay_layout);
        if (einkRefreshEnabled == 0) {
            einkRefreshDelayLayout.setVisibility(View.GONE);
        } else {
            einkRefreshDelayLayout.setVisibility(View.VISIBLE);
        }
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
