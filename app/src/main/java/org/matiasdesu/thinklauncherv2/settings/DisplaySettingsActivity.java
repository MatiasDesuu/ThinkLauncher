package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;

import android.os.Build;

public class DisplaySettingsActivity extends AppCompatActivity {

    private int scrollAppList;
    private boolean autoFocusSearch;
    private int einkRefreshEnabled;
    private int einkRefreshDelay;
    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;

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
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_settings);

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

        scrollAppList = prefs.getInt("scroll_app_list", 0);
        autoFocusSearch = prefs.getBoolean("auto_focus_search", true);
        einkRefreshEnabled = prefs.getInt("eink_refresh_enabled", 0);
        einkRefreshDelay = prefs.getInt("eink_refresh_delay", 100);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

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

        TextView minusScrollAppListBtn = scrollAppListContainer.findViewById(R.id.btn_minus);
        TextView plusScrollAppListBtn = scrollAppListContainer.findViewById(R.id.btn_plus);

        TextView minusAutoFocusBtn = autoFocusContainer.findViewById(R.id.btn_minus);
        TextView plusAutoFocusBtn = autoFocusContainer.findViewById(R.id.btn_plus);

        TextView minusEinkRefreshEnabledBtn = einkRefreshEnabledContainer.findViewById(R.id.btn_minus);
        TextView plusEinkRefreshEnabledBtn = einkRefreshEnabledContainer.findViewById(R.id.btn_plus);

        TextView minusEinkRefreshDelayBtn = einkRefreshDelayContainer.findViewById(R.id.btn_minus);
        TextView plusEinkRefreshDelayBtn = einkRefreshDelayContainer.findViewById(R.id.btn_plus);

        minusScrollAppListBtn.setOnClickListener(v -> {
            scrollAppList = (scrollAppList - 1 + 2) % 2;
            scrollAppListValueTv.setText(getOnOffText(scrollAppList));
            prefs.edit().putInt("scroll_app_list", scrollAppList).apply();
            if (paginationHelper != null) {
                paginationHelper.initialize(this::refreshVisibility);
            }
        });

        plusScrollAppListBtn.setOnClickListener(v -> {
            scrollAppList = (scrollAppList + 1) % 2;
            scrollAppListValueTv.setText(getOnOffText(scrollAppList));
            prefs.edit().putInt("scroll_app_list", scrollAppList).apply();
            if (paginationHelper != null) {
                paginationHelper.initialize(this::refreshVisibility);
            }
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

        minusEinkRefreshEnabledBtn.setOnClickListener(v -> {
            einkRefreshEnabled = (einkRefreshEnabled - 1 + 2) % 2;
            einkRefreshEnabledValueTv.setText(getOnOffText(einkRefreshEnabled));
            prefs.edit().putInt("eink_refresh_enabled", einkRefreshEnabled).apply();
            updateEinkRefreshDelayVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusEinkRefreshEnabledBtn.setOnClickListener(v -> {
            einkRefreshEnabled = (einkRefreshEnabled + 1) % 2;
            einkRefreshEnabledValueTv.setText(getOnOffText(einkRefreshEnabled));
            prefs.edit().putInt("eink_refresh_enabled", einkRefreshEnabled).apply();
            updateEinkRefreshDelayVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
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
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);

        findViewById(R.id.time_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, TimeSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.date_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, DateSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.settings_button_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsButtonSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.search_button_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchButtonSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void refreshVisibility() {
        updateEinkRefreshDelayVisibility();
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