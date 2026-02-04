package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;

import android.os.Build;
import android.widget.FrameLayout;
import android.widget.ScrollView;

public class HomeSettingsActivity extends AppCompatActivity {

    private int maxApps;
    private int homeAlignment;
    private int homeVerticalAlignment;
    private int homeColumns;
    private int homePages;
    private boolean hidePagination;
    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    // Bring MainActivity to front
                    Intent mainIntent = new Intent(HomeSettingsActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_home_settings);

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

        maxApps = prefs.getInt("max_apps", 4);
        homeAlignment = prefs.getInt("home_alignment", 1);
        homeVerticalAlignment = prefs.getInt("home_vertical_alignment", 1);
        homeColumns = prefs.getInt("home_columns", 1);
        homePages = prefs.getInt("home_pages", 1);
        hidePagination = prefs.getBoolean("hide_pagination", false);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View maxAppsContainer = findViewById(R.id.max_apps_container);
        TextView maxAppsValueTv = maxAppsContainer.findViewById(R.id.value_text);
        maxAppsValueTv.setText(String.valueOf(maxApps));

        View columnsContainer = findViewById(R.id.columns_container);
        TextView columnsValueTv = columnsContainer.findViewById(R.id.value_text);
        columnsValueTv.setText(String.valueOf(homeColumns));

        View pagesContainer = findViewById(R.id.pages_container);
        TextView pagesValueTv = pagesContainer.findViewById(R.id.value_text);
        pagesValueTv.setText(String.valueOf(homePages));

        View hidePaginationContainer = findViewById(R.id.hide_pagination_row);
        hidePaginationContainer.setVisibility(homePages > 1 ? View.VISIBLE : View.GONE);
        TextView hidePaginationValueTv = hidePaginationContainer.findViewById(R.id.value_text);
        hidePaginationValueTv.setText(hidePagination ? "ON" : "OFF");
        hidePaginationValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(hidePaginationValueTv, new String[] { "ON", "OFF" }));

        LinearLayout textSettingsButton = findViewById(R.id.text_settings_button);
        textSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TextSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout iconSettingsButton = findViewById(R.id.icon_settings_button);
        iconSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, IconSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout wallpaperSettingsButton = findViewById(R.id.wallpaper_settings_button);
        wallpaperSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WallpaperSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        View homeAlignmentContainer = findViewById(R.id.home_alignment_container);
        TextView homeAlignmentValueTv = homeAlignmentContainer.findViewById(R.id.value_text);
        homeAlignmentValueTv.setText(getAlignmentText(homeAlignment));
        homeAlignmentValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(homeAlignmentValueTv, new String[] { "Left", "Center", "Right" }));

        View homeVerticalAlignmentContainer = findViewById(R.id.home_vertical_alignment_container);
        TextView homeVerticalAlignmentValueTv = homeVerticalAlignmentContainer.findViewById(R.id.value_text);
        homeVerticalAlignmentValueTv.setText(getVerticalAlignmentText(homeVerticalAlignment));
        homeVerticalAlignmentValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(homeVerticalAlignmentValueTv,
                new String[] { "Top", "Center", "Bottom" }));

        TextView minusMaxAppsBtn = maxAppsContainer.findViewById(R.id.btn_minus);
        TextView plusMaxAppsBtn = maxAppsContainer.findViewById(R.id.btn_plus);
        TextView minusColumnsBtn = columnsContainer.findViewById(R.id.btn_minus);
        TextView plusColumnsBtn = columnsContainer.findViewById(R.id.btn_plus);
        TextView minusPagesBtn = pagesContainer.findViewById(R.id.btn_minus);
        TextView plusPagesBtn = pagesContainer.findViewById(R.id.btn_plus);
        TextView minusHidePaginationBtn = hidePaginationContainer.findViewById(R.id.btn_minus);
        TextView plusHidePaginationBtn = hidePaginationContainer.findViewById(R.id.btn_plus);
        TextView minusHomeAlignmentBtn = homeAlignmentContainer.findViewById(R.id.btn_minus);
        TextView plusHomeAlignmentBtn = homeAlignmentContainer.findViewById(R.id.btn_plus);
        TextView minusHomeVerticalBtn = homeVerticalAlignmentContainer.findViewById(R.id.btn_minus);
        TextView plusHomeVerticalBtn = homeVerticalAlignmentContainer.findViewById(R.id.btn_plus);

        minusMaxAppsBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (maxApps > 1) {
                maxApps--;
                maxAppsValueTv.setText(String.valueOf(maxApps));
                prefs.edit().putInt("max_apps", maxApps).apply();
            }
        }));

        plusMaxAppsBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            maxApps++;
            maxAppsValueTv.setText(String.valueOf(maxApps));
            prefs.edit().putInt("max_apps", maxApps).apply();
        }));

        minusColumnsBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homeColumns > 1) {
                homeColumns--;
                columnsValueTv.setText(String.valueOf(homeColumns));
                prefs.edit().putInt("home_columns", homeColumns).apply();
            }
        }));

        plusColumnsBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homeColumns < 4) {
                homeColumns++;
                columnsValueTv.setText(String.valueOf(homeColumns));
                prefs.edit().putInt("home_columns", homeColumns).apply();
            }
        }));

        minusPagesBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homePages > 1) {
                homePages--;
                pagesValueTv.setText(String.valueOf(homePages));
                prefs.edit().putInt("home_pages", homePages).apply();
                if (paginationHelper != null) {
                    paginationHelper.updateVisibleItemsList();
                }
            }
        }));

        plusPagesBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homePages < 10) { // arbitrary max
                homePages++;
                pagesValueTv.setText(String.valueOf(homePages));
                prefs.edit().putInt("home_pages", homePages).apply();
                if (paginationHelper != null) {
                    paginationHelper.updateVisibleItemsList();
                }
            }
        }));

        minusHomeAlignmentBtn.setOnClickListener(v -> {
            homeAlignment = (homeAlignment - 1 + 3) % 3;
            homeAlignmentValueTv.setText(getAlignmentText(homeAlignment));
            prefs.edit().putInt("home_alignment", homeAlignment).apply();
        });

        plusHomeAlignmentBtn.setOnClickListener(v -> {
            homeAlignment = (homeAlignment + 1) % 3;
            homeAlignmentValueTv.setText(getAlignmentText(homeAlignment));
            prefs.edit().putInt("home_alignment", homeAlignment).apply();
        });

        minusHomeVerticalBtn.setOnClickListener(v -> {
            homeVerticalAlignment = (homeVerticalAlignment - 1 + 3) % 3;
            homeVerticalAlignmentValueTv.setText(getVerticalAlignmentText(homeVerticalAlignment));
            prefs.edit().putInt("home_vertical_alignment", homeVerticalAlignment).apply();
        });

        plusHomeVerticalBtn.setOnClickListener(v -> {
            homeVerticalAlignment = (homeVerticalAlignment + 1) % 3;
            homeVerticalAlignmentValueTv.setText(getVerticalAlignmentText(homeVerticalAlignment));
            prefs.edit().putInt("home_vertical_alignment", homeVerticalAlignment).apply();
        });

        minusHidePaginationBtn.setOnClickListener(v -> {
            hidePagination = !hidePagination;
            hidePaginationValueTv.setText(hidePagination ? "ON" : "OFF");
            prefs.edit().putBoolean("hide_pagination", hidePagination).apply();
        });

        plusHidePaginationBtn.setOnClickListener(v -> {
            hidePagination = !hidePagination;
            hidePaginationValueTv.setText(hidePagination ? "ON" : "OFF");
            prefs.edit().putBoolean("hide_pagination", hidePagination).apply();
        });

        // Initialize pagination
        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme,
                settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(() -> {
            hidePaginationContainer.setVisibility(homePages > 1 ? View.VISIBLE : View.GONE);
        });
    }

    private String getAlignmentText(int alignment) {
        switch (alignment) {
            case 0:
                return "Left";
            case 1:
                return "Center";
            case 2:
                return "Right";
            default:
                return "Center";
        }
    }

    private String getVerticalAlignmentText(int alignment) {
        switch (alignment) {
            case 0:
                return "Top";
            case 1:
                return "Center";
            case 2:
                return "Bottom";
            default:
                return "Center";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        // Check if scroll_app_list setting has changed
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
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