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
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import android.os.Build;

public class FontSizesSettingsActivity extends AppCompatActivity {

    private int homeFontSize;
    private int appLauncherFontSize;
    private int calendarFontSize;
    private int koreaderHistoryFontSize;
    private int folderFontSize;
    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;
    private boolean screenAnimations;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(FontSizesSettingsActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_font_sizes_settings);

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

        homeFontSize = prefs.getInt("text_size", 32);
        appLauncherFontSize = prefs.getInt("app_launcher_font_size", 32);
        calendarFontSize = prefs.getInt("calendar_font_size", 32);
        koreaderHistoryFontSize = prefs.getInt("koreader_history_font_size", 32);
        folderFontSize = prefs.getInt("folder_font_size", 32);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
        });

        View homeContainer = findViewById(R.id.home_font_size_container);
        TextView homeValueTv = homeContainer.findViewById(R.id.value_text);
        homeValueTv.setText(String.valueOf(homeFontSize));

        View appLauncherContainer = findViewById(R.id.app_launcher_font_size_container);
        TextView appLauncherValueTv = appLauncherContainer.findViewById(R.id.value_text);
        appLauncherValueTv.setText(String.valueOf(appLauncherFontSize));

        View calendarContainer = findViewById(R.id.calendar_font_size_container);
        TextView calendarValueTv = calendarContainer.findViewById(R.id.value_text);
        calendarValueTv.setText(String.valueOf(calendarFontSize));

        View koreaderContainer = findViewById(R.id.koreader_history_font_size_container);
        TextView koreaderValueTv = koreaderContainer.findViewById(R.id.value_text);
        koreaderValueTv.setText(String.valueOf(koreaderHistoryFontSize));

        View folderContainer = findViewById(R.id.folder_font_size_container);
        TextView folderValueTv = folderContainer.findViewById(R.id.value_text);
        folderValueTv.setText(String.valueOf(folderFontSize));

        TextView minusHomeBtn = homeContainer.findViewById(R.id.btn_minus);
        TextView plusHomeBtn = homeContainer.findViewById(R.id.btn_plus);
        TextView minusAppLauncherBtn = appLauncherContainer.findViewById(R.id.btn_minus);
        TextView plusAppLauncherBtn = appLauncherContainer.findViewById(R.id.btn_plus);
        TextView minusCalendarBtn = calendarContainer.findViewById(R.id.btn_minus);
        TextView plusCalendarBtn = calendarContainer.findViewById(R.id.btn_plus);
        TextView minusKoreaderBtn = koreaderContainer.findViewById(R.id.btn_minus);
        TextView plusKoreaderBtn = koreaderContainer.findViewById(R.id.btn_plus);
        TextView minusFolderBtn = folderContainer.findViewById(R.id.btn_minus);
        TextView plusFolderBtn = folderContainer.findViewById(R.id.btn_plus);

        minusHomeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homeFontSize > 10) {
                homeFontSize--;
                homeValueTv.setText(String.valueOf(homeFontSize));
                prefs.edit().putInt("text_size", homeFontSize).apply();
            }
        }));

        plusHomeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homeFontSize < 50) {
                homeFontSize++;
                homeValueTv.setText(String.valueOf(homeFontSize));
                prefs.edit().putInt("text_size", homeFontSize).apply();
            }
        }));

        minusAppLauncherBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (appLauncherFontSize > 10) {
                appLauncherFontSize--;
                appLauncherValueTv.setText(String.valueOf(appLauncherFontSize));
                prefs.edit().putInt("app_launcher_font_size", appLauncherFontSize).apply();
            }
        }));

        plusAppLauncherBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (appLauncherFontSize < 50) {
                appLauncherFontSize++;
                appLauncherValueTv.setText(String.valueOf(appLauncherFontSize));
                prefs.edit().putInt("app_launcher_font_size", appLauncherFontSize).apply();
            }
        }));

        minusCalendarBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (calendarFontSize > 10) {
                calendarFontSize--;
                calendarValueTv.setText(String.valueOf(calendarFontSize));
                prefs.edit().putInt("calendar_font_size", calendarFontSize).apply();
            }
        }));

        plusCalendarBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (calendarFontSize < 50) {
                calendarFontSize++;
                calendarValueTv.setText(String.valueOf(calendarFontSize));
                prefs.edit().putInt("calendar_font_size", calendarFontSize).apply();
            }
        }));

        minusKoreaderBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (koreaderHistoryFontSize > 10) {
                koreaderHistoryFontSize--;
                koreaderValueTv.setText(String.valueOf(koreaderHistoryFontSize));
                prefs.edit().putInt("koreader_history_font_size", koreaderHistoryFontSize).apply();
            }
        }));

        plusKoreaderBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (koreaderHistoryFontSize < 50) {
                koreaderHistoryFontSize++;
                koreaderValueTv.setText(String.valueOf(koreaderHistoryFontSize));
                prefs.edit().putInt("koreader_history_font_size", koreaderHistoryFontSize).apply();
            }
        }));

        minusFolderBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (folderFontSize > 10) {
                folderFontSize--;
                folderValueTv.setText(String.valueOf(folderFontSize));
                prefs.edit().putInt("folder_font_size", folderFontSize).apply();
            }
        }));

        plusFolderBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (folderFontSize < 50) {
                folderFontSize++;
                folderValueTv.setText(String.valueOf(folderFontSize));
                prefs.edit().putInt("folder_font_size", folderFontSize).apply();
            }
        }));

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void refreshVisibility() {
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
        finish();
        overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
    }
}
