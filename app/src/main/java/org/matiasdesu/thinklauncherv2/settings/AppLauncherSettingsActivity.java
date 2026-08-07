package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.HideAppsActivity;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class AppLauncherSettingsActivity extends BaseSettingsActivity {

    private int appIndexSidebar;
    private boolean autoFocusSearch;
    private int webappPwaMode;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(AppLauncherSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_app_launcher_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        autoFocusSearch = prefs.getBoolean("auto_focus_search", true);
        webappPwaMode = prefs.getInt("webapp_pwa_mode", 0);

        findViewById(R.id.hide_apps_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, HideAppsActivity.class);
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
        });

        plusAppIndexSidebarBtn.setOnClickListener(v -> {
            appIndexSidebar = (appIndexSidebar + 1) % 2;
            appIndexSidebarValueTv.setText(getOnOffText(appIndexSidebar));
            prefs.edit().putInt("app_index_sidebar", appIndexSidebar).apply();
        });

        View autoFocusContainer = findViewById(R.id.autofocus_container);
        TextView autoFocusValueTv = autoFocusContainer.findViewById(R.id.value_text);
        autoFocusValueTv.setText(autoFocusSearch ? "ON" : "OFF");
        autoFocusValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(autoFocusValueTv, new String[] { "OFF", "ON" }));

        ImageButton minusAutoFocusBtn = autoFocusContainer.findViewById(R.id.btn_minus);
        ImageButton plusAutoFocusBtn = autoFocusContainer.findViewById(R.id.btn_plus);

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

        initPagination(null);
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
