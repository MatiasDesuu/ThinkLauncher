package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;

import android.os.Build;

public class ThemeSettingsActivity extends AppCompatActivity {

    private int theme;
    private int customBgColor;
    private int customAccentColor;
    private boolean invertIconColors;
    private boolean invertHomeColors;
    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason) || "recentapps".equals(reason)) {
                    // Bring MainActivity to front
                    Intent mainIntent = new Intent(ThemeSettingsActivity.this, MainActivity.class);
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
        customBgColor = prefs.getInt("custom_bg_color", Color.WHITE);
        customAccentColor = prefs.getInt("custom_accent_color", Color.BLACK);
        invertIconColors = prefs.getBoolean("invert_icon_colors", false);
        invertHomeColors = prefs.getBoolean("invert_home_colors", false);
        
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_settings);

        int bgColor = ThemeUtils.getBgColor(theme, this);

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

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View themeContainer = findViewById(R.id.theme_container);
        TextView themeValueTv = themeContainer.findViewById(R.id.value_text);
        themeValueTv.setText(ThemeUtils.getThemeName(theme));
        themeValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(themeValueTv, ThemeUtils.THEME_NAMES));

        TextView minusThemeBtn = themeContainer.findViewById(R.id.btn_minus);
        TextView plusThemeBtn = themeContainer.findViewById(R.id.btn_plus);

        minusThemeBtn.setOnClickListener(v -> {
            theme = ThemeUtils.getPreviousTheme(theme);
            themeValueTv.setText(ThemeUtils.getThemeName(theme));
            prefs.edit().putInt("theme", theme).apply();
            restartActivity();
        });

        plusThemeBtn.setOnClickListener(v -> {
            theme = ThemeUtils.getNextTheme(theme);
            themeValueTv.setText(ThemeUtils.getThemeName(theme));
            prefs.edit().putInt("theme", theme).apply();
            restartActivity();
        });

        LinearLayout customBgColorLayout = findViewById(R.id.custom_bg_color_layout);
        TextView customBgColorText = findViewById(R.id.custom_bg_color_text);
        customBgColorText.setText(String.format("#%06X", (0xFFFFFF & customBgColor)));
        customBgColorText.setOnClickListener(v -> showColorPickerDialog("custom_bg_color", customBgColor));

        LinearLayout customAccentColorLayout = findViewById(R.id.custom_accent_color_layout);
        TextView customAccentColorText = findViewById(R.id.custom_accent_color_text);
        customAccentColorText.setText(String.format("#%06X", (0xFFFFFF & customAccentColor)));
        customAccentColorText.setOnClickListener(v -> showColorPickerDialog("custom_accent_color", customAccentColor));

        View invertIconColorsContainer = findViewById(R.id.invert_icon_colors_container);
        TextView invertIconColorsValueTv = invertIconColorsContainer.findViewById(R.id.value_text);
        invertIconColorsValueTv.setText(invertIconColors ? "ON" : "OFF");
        invertIconColorsValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(invertIconColorsValueTv, new String[] { "ON", "OFF" }));

        TextView minusInvertIconColorsBtn = invertIconColorsContainer.findViewById(R.id.btn_minus);
        TextView plusInvertIconColorsBtn = invertIconColorsContainer.findViewById(R.id.btn_plus);

        minusInvertIconColorsBtn.setOnClickListener(v -> {
            invertIconColors = !invertIconColors;
            invertIconColorsValueTv.setText(invertIconColors ? "ON" : "OFF");
            prefs.edit().putBoolean("invert_icon_colors", invertIconColors).apply();
        });

        plusInvertIconColorsBtn.setOnClickListener(v -> {
            invertIconColors = !invertIconColors;
            invertIconColorsValueTv.setText(invertIconColors ? "ON" : "OFF");
            prefs.edit().putBoolean("invert_icon_colors", invertIconColors).apply();
        });

        View invertHomeColorsContainer = findViewById(R.id.invert_home_colors_container);
        TextView invertHomeColorsValueTv = invertHomeColorsContainer.findViewById(R.id.value_text);
        invertHomeColorsValueTv.setText(invertHomeColors ? "ON" : "OFF");
        invertHomeColorsValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(invertHomeColorsValueTv, new String[] { "ON", "OFF" }));

        TextView minusInvertHomeColorsBtn = invertHomeColorsContainer.findViewById(R.id.btn_minus);
        TextView plusInvertHomeColorsBtn = invertHomeColorsContainer.findViewById(R.id.btn_plus);

        minusInvertHomeColorsBtn.setOnClickListener(v -> {
            invertHomeColors = !invertHomeColors;
            invertHomeColorsValueTv.setText(invertHomeColors ? "ON" : "OFF");
            prefs.edit().putBoolean("invert_home_colors", invertHomeColors).apply();
        });

        plusInvertHomeColorsBtn.setOnClickListener(v -> {
            invertHomeColors = !invertHomeColors;
            invertHomeColorsValueTv.setText(invertHomeColors ? "ON" : "OFF");
            prefs.edit().putBoolean("invert_home_colors", invertHomeColors).apply();
        });

        updateCustomColorVisibility();

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void refreshVisibility() {
        updateCustomColorVisibility();
    }

    private void updateCustomColorVisibility() {
        LinearLayout customBgColorLayout = findViewById(R.id.custom_bg_color_layout);
        LinearLayout customAccentColorLayout = findViewById(R.id.custom_accent_color_layout);
        LinearLayout invertIconColorsLayout = findViewById(R.id.invert_icon_colors_layout);
        LinearLayout invertHomeColorsLayout = findViewById(R.id.invert_home_colors_layout);
        
        if (theme != ThemeUtils.THEME_CUSTOM) {
            customBgColorLayout.setVisibility(View.GONE);
            customAccentColorLayout.setVisibility(View.GONE);
        } else {
            customBgColorLayout.setVisibility(View.VISIBLE);
            customAccentColorLayout.setVisibility(View.VISIBLE);
        }
        
        // These are useful for all themes
        invertIconColorsLayout.setVisibility(View.VISIBLE);
        invertHomeColorsLayout.setVisibility(View.VISIBLE);
    }

    private void showColorPickerDialog(String key, int currentColor) {
        org.matiasdesu.thinklauncherv2.ui.ColorPickerDialog dialog = new org.matiasdesu.thinklauncherv2.ui.ColorPickerDialog(
            this, 
            currentColor, 
            color -> {
                getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt(key, color).apply();
                restartActivity();
            }
        );
        dialog.show();
    }

    private void restartActivity() {
        Intent intent = new Intent(this, ThemeSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
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
