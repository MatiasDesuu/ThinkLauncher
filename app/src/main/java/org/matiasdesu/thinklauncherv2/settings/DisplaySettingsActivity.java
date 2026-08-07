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
    private int einkRefreshEnabled;
    private int einkRefreshDelay;
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
        einkRefreshEnabled = prefs.getInt("eink_refresh_enabled", 0);
        einkRefreshDelay = prefs.getInt("eink_refresh_delay", 100);
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

        initPagination(this::refreshVisibility);
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
