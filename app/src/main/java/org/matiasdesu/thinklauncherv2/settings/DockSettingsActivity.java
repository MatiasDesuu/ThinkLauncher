package org.matiasdesu.thinklauncherv2.settings;

import android.content.Context;
import android.content.Intent;
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

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.RepeatListener;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class DockSettingsActivity extends AppCompatActivity {

    private static final int ICON_SIZE_MIN = 16;
    private static final int ICON_SIZE_MAX = 64;
    private static final int NUM_APPS_MIN = 1;
    private static final int NUM_APPS_MAX = 10;

    private static final String PREFIX = "dock";

    private SharedPreferences prefs;
    private int theme;
    private boolean screenAnimations;

    private int enabled;
    private int iconSize;
    private int numApps;
    private int border;
    private int background;

    private SettingsPaginationHelper paginationHelper;

    private String p(String key) {
        return PREFIX + "_" + key;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dock_settings);

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

        LinearLayout rootLayout = findViewById(R.id.root_layout);
        rootLayout.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(rootLayout, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
        });

        enabled = prefs.getInt(p("enabled"), 0);
        iconSize = prefs.getInt(p("icon_size"), 24);
        numApps = prefs.getInt(p("num_apps"), 4);
        border = prefs.getInt(p("border"), 0);
        background = prefs.getInt(p("background"), 0);

        View enabledContainer = findViewById(R.id.enabled_container);
        TextView enabledValueTv = enabledContainer.findViewById(R.id.value_text);
        enabledValueTv.setText(enabled == 1 ? "ON" : "OFF");
        enabledValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(enabledValueTv, new String[] { "ON", "OFF" }));

        View styleButton = findViewById(R.id.style_button);
        styleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppBarStyleActivity.class);
            intent.putExtra(AppBarStyleActivity.EXTRA_PREFIX, PREFIX);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        View iconSizeContainer = findViewById(R.id.icon_size_container);
        TextView iconSizeValueTv = iconSizeContainer.findViewById(R.id.value_text);
        iconSizeValueTv.setText(String.valueOf(iconSize));

        View numAppsContainer = findViewById(R.id.num_apps_container);
        TextView numAppsValueTv = numAppsContainer.findViewById(R.id.value_text);
        numAppsValueTv.setText(String.valueOf(numApps));

        View borderContainer = findViewById(R.id.border_container);
        TextView borderValueTv = borderContainer.findViewById(R.id.value_text);
        borderValueTv.setText(border == 1 ? "ON" : "OFF");
        borderValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(borderValueTv, new String[] { "ON", "OFF" }));

        View backgroundContainer = findViewById(R.id.background_container);
        TextView backgroundValueTv = backgroundContainer.findViewById(R.id.value_text);
        backgroundValueTv.setText(background == 1 ? "ON" : "OFF");
        backgroundValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(backgroundValueTv, new String[] { "ON", "OFF" }));

        TextView minusEnabled = enabledContainer.findViewById(R.id.btn_minus);
        TextView plusEnabled = enabledContainer.findViewById(R.id.btn_plus);
        TextView minusIconSize = iconSizeContainer.findViewById(R.id.btn_minus);
        TextView plusIconSize = iconSizeContainer.findViewById(R.id.btn_plus);
        TextView minusNumApps = numAppsContainer.findViewById(R.id.btn_minus);
        TextView plusNumApps = numAppsContainer.findViewById(R.id.btn_plus);
        TextView minusBorder = borderContainer.findViewById(R.id.btn_minus);
        TextView plusBorder = borderContainer.findViewById(R.id.btn_plus);
        TextView minusBackground = backgroundContainer.findViewById(R.id.btn_minus);
        TextView plusBackground = backgroundContainer.findViewById(R.id.btn_plus);

        minusEnabled.setOnClickListener(v -> {
            enabled = enabled == 1 ? 0 : 1;
            enabledValueTv.setText(enabled == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("enabled"), enabled).apply();
            refreshVisibility();
        });
        plusEnabled.setOnClickListener(v -> {
            enabled = enabled == 1 ? 0 : 1;
            enabledValueTv.setText(enabled == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("enabled"), enabled).apply();
            refreshVisibility();
        });

        minusIconSize.setOnTouchListener(new RepeatListener(v -> {
            if (iconSize > ICON_SIZE_MIN) {
                iconSize--;
                iconSizeValueTv.setText(String.valueOf(iconSize));
                prefs.edit().putInt(p("icon_size"), iconSize).apply();
            }
        }));
        plusIconSize.setOnTouchListener(new RepeatListener(v -> {
            if (iconSize < ICON_SIZE_MAX) {
                iconSize++;
                iconSizeValueTv.setText(String.valueOf(iconSize));
                prefs.edit().putInt(p("icon_size"), iconSize).apply();
            }
        }));

        minusNumApps.setOnTouchListener(new RepeatListener(v -> {
            if (numApps > NUM_APPS_MIN) {
                numApps--;
                numAppsValueTv.setText(String.valueOf(numApps));
                prefs.edit().putInt(p("num_apps"), numApps).apply();
            }
        }));
        plusNumApps.setOnTouchListener(new RepeatListener(v -> {
            if (numApps < NUM_APPS_MAX) {
                numApps++;
                numAppsValueTv.setText(String.valueOf(numApps));
                prefs.edit().putInt(p("num_apps"), numApps).apply();
            }
        }));

        minusBorder.setOnClickListener(v -> {
            border = border == 1 ? 0 : 1;
            borderValueTv.setText(border == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("border"), border).apply();
        });
        plusBorder.setOnClickListener(v -> {
            border = border == 1 ? 0 : 1;
            borderValueTv.setText(border == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("border"), border).apply();
        });

        minusBackground.setOnClickListener(v -> {
            background = background == 1 ? 0 : 1;
            backgroundValueTv.setText(background == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("background"), background).apply();
        });
        plusBackground.setOnClickListener(v -> {
            background = background == 1 ? 0 : 1;
            backgroundValueTv.setText(background == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("background"), background).apply();
        });

        LinearLayout selectAppsButton = findViewById(R.id.select_apps_button);
        selectAppsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppBarAppsActivity.class);
            intent.putExtra(AppBarAppsActivity.EXTRA_PREFIX, PREFIX);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);

        refreshVisibility();
    }

    private void refreshVisibility() {
        View[] views = {
                findViewById(R.id.icon_size_layout),
                findViewById(R.id.num_apps_layout),
                findViewById(R.id.border_layout),
                findViewById(R.id.background_layout),
                findViewById(R.id.style_button),
                findViewById(R.id.select_apps_button)
        };
        int visibility = enabled == 1 ? View.VISIBLE : View.GONE;
        for (View v : views) {
            v.setVisibility(visibility);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
    }
}