package org.matiasdesu.thinklauncherv2.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.RepeatListener;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class AppBarSettingsActivity extends AppCompatActivity {

    private static final int ICON_SIZE_MIN = 16;
    private static final int ICON_SIZE_MAX = 64;
    private static final int NUM_APPS_MIN = 1;
    private static final int NUM_APPS_MAX = 10;

    private SharedPreferences prefs;
    private int theme;
    private boolean screenAnimations;

    private int enabled;
    private int orientation;
    private int iconSize;
    private int numApps;

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
        setContentView(R.layout.activity_app_bar_settings);

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

        enabled = prefs.getInt("app_bar_enabled", 0);
        orientation = prefs.getInt("app_bar_orientation", 0);
        iconSize = prefs.getInt("app_bar_icon_size", 24);
        numApps = prefs.getInt("app_bar_num_apps", 4);

        View enabledContainer = findViewById(R.id.enabled_container);
        TextView enabledValueTv = enabledContainer.findViewById(R.id.value_text);
        enabledValueTv.setText(enabled == 1 ? "ON" : "OFF");
        enabledValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(enabledValueTv, new String[] { "ON", "OFF" }));

        View positionButton = findViewById(R.id.position_button);
        positionButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppBarPositionActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        View orientationContainer = findViewById(R.id.orientation_container);
        TextView orientationValueTv = orientationContainer.findViewById(R.id.value_text);
        orientationValueTv.setText(orientation == 1 ? "Vertical" : "Horizontal");
        orientationValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(orientationValueTv, new String[] { "Horizontal", "Vertical" }));

        View iconSizeContainer = findViewById(R.id.icon_size_container);
        TextView iconSizeValueTv = iconSizeContainer.findViewById(R.id.value_text);
        iconSizeValueTv.setText(String.valueOf(iconSize));

        View numAppsContainer = findViewById(R.id.num_apps_container);
        TextView numAppsValueTv = numAppsContainer.findViewById(R.id.value_text);
        numAppsValueTv.setText(String.valueOf(numApps));

        TextView minusEnabled = enabledContainer.findViewById(R.id.btn_minus);
        TextView plusEnabled = enabledContainer.findViewById(R.id.btn_plus);
        TextView minusOrientation = orientationContainer.findViewById(R.id.btn_minus);
        TextView plusOrientation = orientationContainer.findViewById(R.id.btn_plus);
        TextView minusIconSize = iconSizeContainer.findViewById(R.id.btn_minus);
        TextView plusIconSize = iconSizeContainer.findViewById(R.id.btn_plus);
        TextView minusNumApps = numAppsContainer.findViewById(R.id.btn_minus);
        TextView plusNumApps = numAppsContainer.findViewById(R.id.btn_plus);

        minusEnabled.setOnClickListener(v -> {
            enabled = enabled == 1 ? 0 : 1;
            enabledValueTv.setText(enabled == 1 ? "ON" : "OFF");
            prefs.edit().putInt("app_bar_enabled", enabled).apply();
        });
        plusEnabled.setOnClickListener(v -> {
            enabled = enabled == 1 ? 0 : 1;
            enabledValueTv.setText(enabled == 1 ? "ON" : "OFF");
            prefs.edit().putInt("app_bar_enabled", enabled).apply();
        });

        // Position is selected via the visual picker (AppBarPositionActivity)
        minusOrientation.setOnClickListener(v -> {
            orientation = orientation == 1 ? 0 : 1;
            orientationValueTv.setText(orientation == 1 ? "Vertical" : "Horizontal");
            prefs.edit().putInt("app_bar_orientation", orientation).apply();
        });
        plusOrientation.setOnClickListener(v -> {
            orientation = orientation == 1 ? 0 : 1;
            orientationValueTv.setText(orientation == 1 ? "Vertical" : "Horizontal");
            prefs.edit().putInt("app_bar_orientation", orientation).apply();
        });

        minusIconSize.setOnTouchListener(new RepeatListener(v -> {
            if (iconSize > ICON_SIZE_MIN) {
                iconSize--;
                iconSizeValueTv.setText(String.valueOf(iconSize));
                prefs.edit().putInt("app_bar_icon_size", iconSize).apply();
            }
        }));
        plusIconSize.setOnTouchListener(new RepeatListener(v -> {
            if (iconSize < ICON_SIZE_MAX) {
                iconSize++;
                iconSizeValueTv.setText(String.valueOf(iconSize));
                prefs.edit().putInt("app_bar_icon_size", iconSize).apply();
            }
        }));

        minusNumApps.setOnTouchListener(new RepeatListener(v -> {
            if (numApps > NUM_APPS_MIN) {
                numApps--;
                numAppsValueTv.setText(String.valueOf(numApps));
                prefs.edit().putInt("app_bar_num_apps", numApps).apply();
            }
        }));
        plusNumApps.setOnTouchListener(new RepeatListener(v -> {
            if (numApps < NUM_APPS_MAX) {
                numApps++;
                numAppsValueTv.setText(String.valueOf(numApps));
                prefs.edit().putInt("app_bar_num_apps", numApps).apply();
            }
        }));

        LinearLayout selectAppsButton = findViewById(R.id.select_apps_button);
        selectAppsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppBarAppsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });
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