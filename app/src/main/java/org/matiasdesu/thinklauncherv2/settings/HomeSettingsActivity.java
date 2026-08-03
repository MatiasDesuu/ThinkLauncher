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

public class HomeSettingsActivity extends BaseSettingsActivity {

    private static final int HOME_COLUMNS_MIN = 1;
    private static final int HOME_COLUMNS_MAX = 10;

    private int maxApps;
    private int homeAlignment;
    private int homeVerticalAlignment;
    private int homeColumns;
    private int homePages;
    private boolean hidePagination;

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
    protected int getLayoutResId() {
        return R.layout.activity_home_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        maxApps = prefs.getInt("max_apps", 4);
        homeAlignment = prefs.getInt("home_alignment", 1);
        homeVerticalAlignment = prefs.getInt("home_vertical_alignment", 1);
        homeColumns = prefs.getInt("home_columns", HOME_COLUMNS_MIN);
        if (homeColumns < HOME_COLUMNS_MIN) homeColumns = HOME_COLUMNS_MIN;
        if (homeColumns > HOME_COLUMNS_MAX) homeColumns = HOME_COLUMNS_MAX;
        homePages = prefs.getInt("home_pages", 1);
        hidePagination = prefs.getBoolean("hide_pagination", false);

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
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout iconSettingsButton = findViewById(R.id.icon_settings_button);
        iconSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, IconSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout wallpaperSettingsButton = findViewById(R.id.wallpaper_settings_button);
        wallpaperSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WallpaperSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
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
            if (homeColumns > HOME_COLUMNS_MIN) {
                homeColumns--;
                columnsValueTv.setText(String.valueOf(homeColumns));
                prefs.edit().putInt("home_columns", homeColumns).apply();
            }
        }));

        plusColumnsBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homeColumns < HOME_COLUMNS_MAX) {
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
                refreshPagination();
            }
        }));

        plusPagesBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (homePages < 10) { // arbitrary max
                homePages++;
                pagesValueTv.setText(String.valueOf(homePages));
                prefs.edit().putInt("home_pages", homePages).apply();
                refreshPagination();
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

        initPagination(this::refreshVisibility);
    }

    private void refreshVisibility() {
        View hidePaginationContainer = findViewById(R.id.hide_pagination_row);
        hidePaginationContainer.setVisibility(homePages > 1 ? View.VISIBLE : View.GONE);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }
}