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
import org.matiasdesu.thinklauncherv2.utils.AppNamePositionHelper;
import org.matiasdesu.thinklauncherv2.utils.IconShapeHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;

import android.os.Build;

public class IconSettingsActivity extends AppCompatActivity {

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Black", "White", "Dynamic Dark", "Dynamic White" };

    private boolean showIcons;
    private boolean showAppNames;
    private int appNamePosition;
    private boolean monochromeIcons;
    private boolean dynamicIcons;
    private boolean dynamicColors;
    private boolean invertIconColors;
    private boolean iconBackground;
    private int iconShape;
    private int iconSize;
    private int iconEffect;
    private int iconEffectColor;
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
                    Intent mainIntent = new Intent(IconSettingsActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_icon_settings);

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

        showIcons = prefs.getBoolean("show_icons", false);
        showAppNames = prefs.getBoolean("show_app_names", true);
        if (!showIcons)
            showAppNames = true;
        appNamePosition = prefs.getInt("app_name_position", AppNamePositionHelper.POSITION_RIGHT);
        monochromeIcons = prefs.getBoolean("monochrome_icons", false);
        dynamicIcons = prefs.getBoolean("dynamic_icons", false);
        dynamicColors = prefs.getBoolean("dynamic_colors", false);
        invertIconColors = prefs.getBoolean("invert_icon_colors", false);
        iconBackground = prefs.getBoolean("icon_background", true);
        iconShape = prefs.getInt("icon_shape", IconShapeHelper.SHAPE_SYSTEM);
        iconSize = prefs.getInt("icon_size", 32);
        iconEffect = prefs.getInt("icon_effect", 0);
        iconEffectColor = prefs.getInt("icon_effect_color", 0);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View showIconsContainer = findViewById(R.id.show_icons_container);
        TextView showIconsValueTv = showIconsContainer.findViewById(R.id.value_text);
        showIconsValueTv.setText(showIcons ? "ON" : "OFF");
        showIconsValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(showIconsValueTv, new String[] { "ON", "OFF" }));

        LinearLayout iconOptionsLayout = findViewById(R.id.icon_options_layout);
        iconOptionsLayout.setVisibility(showIcons ? View.VISIBLE : View.GONE);

        View showAppNamesContainer = findViewById(R.id.show_app_names_container);
        TextView showAppNamesValueTv = showAppNamesContainer.findViewById(R.id.value_text);
        showAppNamesValueTv.setText(showAppNames ? "ON" : "OFF");
        showAppNamesValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(showAppNamesValueTv, new String[] { "ON", "OFF" }));

        LinearLayout appNamePositionLayout = findViewById(R.id.app_name_position_layout);
        View appNamePositionContainer = findViewById(R.id.app_name_position_container);
        TextView appNamePositionValueTv = appNamePositionContainer.findViewById(R.id.value_text);
        appNamePositionValueTv.setText(AppNamePositionHelper.getPositionName(appNamePosition));
        appNamePositionValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(appNamePositionValueTv, AppNamePositionHelper.POSITION_NAMES));
        appNamePositionLayout.setVisibility(showAppNames ? View.VISIBLE : View.GONE);

        View monochromeIconsContainer = findViewById(R.id.monochrome_icons_container);
        TextView monochromeIconsValueTv = monochromeIconsContainer.findViewById(R.id.value_text);
        monochromeIconsValueTv.setText(monochromeIcons ? "ON" : "OFF");
        monochromeIconsValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(monochromeIconsValueTv, new String[] { "ON", "OFF" }));

        View dynamicIconsContainer = findViewById(R.id.dynamic_icons_container);
        TextView dynamicIconsValueTv = dynamicIconsContainer.findViewById(R.id.value_text);
        dynamicIconsValueTv.setText(dynamicIcons ? "ON" : "OFF");
        dynamicIconsValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(dynamicIconsValueTv, new String[] { "ON", "OFF" }));

        LinearLayout dynamicColorsLayout = findViewById(R.id.dynamic_colors_layout);
        View dynamicColorsContainer = findViewById(R.id.dynamic_colors_container);
        TextView dynamicColorsValueTv = dynamicColorsContainer.findViewById(R.id.value_text);
        dynamicColorsValueTv.setText(dynamicColors ? "ON" : "OFF");
        dynamicColorsValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(dynamicColorsValueTv, new String[] { "ON", "OFF" }));
        dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
        LinearLayout invertIconColorsLayout = findViewById(R.id.invert_icon_colors_layout);
        View invertIconColorsContainer = findViewById(R.id.invert_icon_colors_container);
        TextView invertIconColorsValueTv = invertIconColorsContainer.findViewById(R.id.value_text);
        invertIconColorsValueTv.setText(invertIconColors ? "ON" : "OFF");
        invertIconColorsValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(invertIconColorsValueTv, new String[] { "ON", "OFF" }));
        invertIconColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
        LinearLayout iconBackgroundLayout = findViewById(R.id.icon_background_layout);
        View iconBackgroundContainer = findViewById(R.id.icon_background_container);
        TextView iconBackgroundValueTv = iconBackgroundContainer.findViewById(R.id.value_text);
        iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
        iconBackgroundValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconBackgroundValueTv, new String[] { "ON", "OFF" }));
        iconBackgroundLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout iconShapeLayout = findViewById(R.id.icon_shape_layout);
        View iconShapeContainer = findViewById(R.id.icon_shape_container);
        TextView iconShapeValueTv = iconShapeContainer.findViewById(R.id.value_text);
        iconShapeValueTv.setText(IconShapeHelper.getShapeName(iconShape));
        iconShapeValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconShapeValueTv, IconShapeHelper.SHAPE_NAMES));
        iconShapeLayout.setVisibility((dynamicIcons && iconBackground) ? View.VISIBLE : View.GONE);

        View iconEffectContainer = findViewById(R.id.icon_effect_container);
        TextView iconEffectValueTv = iconEffectContainer.findViewById(R.id.value_text);
        iconEffectValueTv.setText(EFFECT_NAMES[iconEffect]);
        iconEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconEffectValueTv, EFFECT_NAMES));

        LinearLayout iconEffectColorLayout = findViewById(R.id.icon_effect_color_layout);
        View iconEffectColorContainer = findViewById(R.id.icon_effect_color_container);
        TextView iconEffectColorValueTv = iconEffectColorContainer.findViewById(R.id.value_text);
        iconEffectColorValueTv.setText(EFFECT_COLOR_NAMES[iconEffectColor]);
        iconEffectColorValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconEffectColorValueTv, EFFECT_COLOR_NAMES));
        iconEffectColorLayout.setVisibility(iconEffect > 0 ? View.VISIBLE : View.GONE);

        View iconSizeContainer = findViewById(R.id.icon_size_container);
        TextView iconSizeValueTv = iconSizeContainer.findViewById(R.id.value_text);
        iconSizeValueTv.setText(String.valueOf(iconSize));

        TextView minusShowIconsBtn = showIconsContainer.findViewById(R.id.btn_minus);
        TextView plusShowIconsBtn = showIconsContainer.findViewById(R.id.btn_plus);
        TextView minusShowAppNamesBtn = showAppNamesContainer.findViewById(R.id.btn_minus);
        TextView plusShowAppNamesBtn = showAppNamesContainer.findViewById(R.id.btn_plus);
        TextView minusAppNamePositionBtn = appNamePositionContainer.findViewById(R.id.btn_minus);
        TextView plusAppNamePositionBtn = appNamePositionContainer.findViewById(R.id.btn_plus);
        TextView minusMonochromeBtn = monochromeIconsContainer.findViewById(R.id.btn_minus);
        TextView plusMonochromeBtn = monochromeIconsContainer.findViewById(R.id.btn_plus);
        TextView minusDynamicBtn = dynamicIconsContainer.findViewById(R.id.btn_minus);
        TextView plusDynamicBtn = dynamicIconsContainer.findViewById(R.id.btn_plus);
        TextView minusDynamicColorsBtn = dynamicColorsContainer.findViewById(R.id.btn_minus);
        TextView plusDynamicColorsBtn = dynamicColorsContainer.findViewById(R.id.btn_plus);
        TextView minusInvertIconColorsBtn = invertIconColorsContainer.findViewById(R.id.btn_minus);
        TextView plusInvertIconColorsBtn = invertIconColorsContainer.findViewById(R.id.btn_plus);
        TextView minusIconBackgroundBtn = iconBackgroundContainer.findViewById(R.id.btn_minus);
        TextView plusIconBackgroundBtn = iconBackgroundContainer.findViewById(R.id.btn_plus);
        TextView minusIconShapeBtn = iconShapeContainer.findViewById(R.id.btn_minus);
        TextView plusIconShapeBtn = iconShapeContainer.findViewById(R.id.btn_plus);
        TextView minusIconEffectBtn = iconEffectContainer.findViewById(R.id.btn_minus);
        TextView plusIconEffectBtn = iconEffectContainer.findViewById(R.id.btn_plus);
        TextView minusIconEffectColorBtn = iconEffectColorContainer.findViewById(R.id.btn_minus);
        TextView plusIconEffectColorBtn = iconEffectColorContainer.findViewById(R.id.btn_plus);
        TextView minusIconSizeBtn = iconSizeContainer.findViewById(R.id.btn_minus);
        TextView plusIconSizeBtn = iconSizeContainer.findViewById(R.id.btn_plus);

        minusShowIconsBtn.setOnClickListener(v -> {
            showIcons = !showIcons;
            showIconsValueTv.setText(showIcons ? "ON" : "OFF");
            if (!showIcons) {
                showAppNames = true;
                showAppNamesValueTv.setText("ON");
                prefs.edit().putBoolean("show_app_names", true).apply();
            }
            iconOptionsLayout.setVisibility(showIcons ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("show_icons", showIcons).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        plusShowIconsBtn.setOnClickListener(v -> {
            showIcons = !showIcons;
            showIconsValueTv.setText(showIcons ? "ON" : "OFF");
            if (!showIcons) {
                showAppNames = true;
                showAppNamesValueTv.setText("ON");
                prefs.edit().putBoolean("show_app_names", true).apply();
            }
            iconOptionsLayout.setVisibility(showIcons ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("show_icons", showIcons).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        minusShowAppNamesBtn.setOnClickListener(v -> {
            showAppNames = !showAppNames;
            showAppNamesValueTv.setText(showAppNames ? "ON" : "OFF");
            appNamePositionLayout.setVisibility(showAppNames ? View.VISIBLE : View.GONE);
            if (!showAppNames) {
                appNamePosition = AppNamePositionHelper.POSITION_RIGHT;
                appNamePositionValueTv.setText(AppNamePositionHelper.getPositionName(appNamePosition));
                prefs.edit().putInt("app_name_position", appNamePosition).apply();
            }
            prefs.edit().putBoolean("show_app_names", showAppNames).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        plusShowAppNamesBtn.setOnClickListener(v -> {
            showAppNames = !showAppNames;
            showAppNamesValueTv.setText(showAppNames ? "ON" : "OFF");
            appNamePositionLayout.setVisibility(showAppNames ? View.VISIBLE : View.GONE);
            if (!showAppNames) {
                appNamePosition = AppNamePositionHelper.POSITION_RIGHT;
                appNamePositionValueTv.setText(AppNamePositionHelper.getPositionName(appNamePosition));
                prefs.edit().putInt("app_name_position", appNamePosition).apply();
            }
            prefs.edit().putBoolean("show_app_names", showAppNames).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        minusAppNamePositionBtn.setOnClickListener(v -> {
            appNamePosition = AppNamePositionHelper.getPreviousPosition(appNamePosition);
            appNamePositionValueTv.setText(AppNamePositionHelper.getPositionName(appNamePosition));
            prefs.edit().putInt("app_name_position", appNamePosition).apply();
        });

        plusAppNamePositionBtn.setOnClickListener(v -> {
            appNamePosition = AppNamePositionHelper.getNextPosition(appNamePosition);
            appNamePositionValueTv.setText(AppNamePositionHelper.getPositionName(appNamePosition));
            prefs.edit().putInt("app_name_position", appNamePosition).apply();
        });

        minusMonochromeBtn.setOnClickListener(v -> {
            monochromeIcons = !monochromeIcons;
            monochromeIconsValueTv.setText(monochromeIcons ? "ON" : "OFF");
            prefs.edit().putBoolean("monochrome_icons", monochromeIcons).apply();
        });

        plusMonochromeBtn.setOnClickListener(v -> {
            monochromeIcons = !monochromeIcons;
            monochromeIconsValueTv.setText(monochromeIcons ? "ON" : "OFF");
            prefs.edit().putBoolean("monochrome_icons", monochromeIcons).apply();
        });

        minusDynamicBtn.setOnClickListener(v -> {
            dynamicIcons = !dynamicIcons;
            dynamicIconsValueTv.setText(dynamicIcons ? "ON" : "OFF");
            dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            invertIconColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            iconBackgroundLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            iconShapeLayout.setVisibility((dynamicIcons && iconBackground) ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("dynamic_icons", dynamicIcons).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        plusDynamicBtn.setOnClickListener(v -> {
            dynamicIcons = !dynamicIcons;
            dynamicIconsValueTv.setText(dynamicIcons ? "ON" : "OFF");
            dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            invertIconColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            iconBackgroundLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            iconShapeLayout.setVisibility((dynamicIcons && iconBackground) ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("dynamic_icons", dynamicIcons).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        minusDynamicColorsBtn.setOnClickListener(v -> {
            dynamicColors = !dynamicColors;
            dynamicColorsValueTv.setText(dynamicColors ? "ON" : "OFF");
            prefs.edit().putBoolean("dynamic_colors", dynamicColors).apply();
        });

        plusDynamicColorsBtn.setOnClickListener(v -> {
            dynamicColors = !dynamicColors;
            dynamicColorsValueTv.setText(dynamicColors ? "ON" : "OFF");
            prefs.edit().putBoolean("dynamic_colors", dynamicColors).apply();
        });

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

        minusIconBackgroundBtn.setOnClickListener(v -> {
            iconBackground = !iconBackground;
            iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
            iconShapeLayout.setVisibility(iconBackground ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("icon_background", iconBackground).apply();
        });

        plusIconBackgroundBtn.setOnClickListener(v -> {
            iconBackground = !iconBackground;
            iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
            iconShapeLayout.setVisibility(iconBackground ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("icon_background", iconBackground).apply();
        });

        minusIconEffectBtn.setOnClickListener(v -> {
            iconEffect = (iconEffect - 1 + EFFECT_NAMES.length) % EFFECT_NAMES.length;
            iconEffectValueTv.setText(EFFECT_NAMES[iconEffect]);
            prefs.edit().putInt("icon_effect", iconEffect).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        plusIconEffectBtn.setOnClickListener(v -> {
            iconEffect = (iconEffect + 1) % EFFECT_NAMES.length;
            iconEffectValueTv.setText(EFFECT_NAMES[iconEffect]);
            prefs.edit().putInt("icon_effect", iconEffect).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        minusIconEffectColorBtn.setOnClickListener(v -> {
            iconEffectColor = (iconEffectColor - 1 + EFFECT_COLOR_NAMES.length) % EFFECT_COLOR_NAMES.length;
            iconEffectColorValueTv.setText(EFFECT_COLOR_NAMES[iconEffectColor]);
            prefs.edit().putInt("icon_effect_color", iconEffectColor).apply();
        });

        plusIconEffectColorBtn.setOnClickListener(v -> {
            iconEffectColor = (iconEffectColor + 1) % EFFECT_COLOR_NAMES.length;
            iconEffectColorValueTv.setText(EFFECT_COLOR_NAMES[iconEffectColor]);
            prefs.edit().putInt("icon_effect_color", iconEffectColor).apply();
        });

        minusIconShapeBtn.setOnClickListener(v -> {
            iconShape = IconShapeHelper.getPreviousShape(iconShape);
            iconShapeValueTv.setText(IconShapeHelper.getShapeName(iconShape));
            prefs.edit().putInt("icon_shape", iconShape).apply();
        });

        plusIconShapeBtn.setOnClickListener(v -> {
            iconShape = IconShapeHelper.getNextShape(iconShape);
            iconShapeValueTv.setText(IconShapeHelper.getShapeName(iconShape));
            prefs.edit().putInt("icon_shape", iconShape).apply();
        });

        minusIconSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (iconSize > 10) {
                iconSize--;
                iconSizeValueTv.setText(String.valueOf(iconSize));
                prefs.edit().putInt("icon_size", iconSize).apply();
            }
        }));

        plusIconSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (iconSize < 100) {
                iconSize++;
                iconSizeValueTv.setText(String.valueOf(iconSize));
                prefs.edit().putInt("icon_size", iconSize).apply();
            }
        }));

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout iconOptionsLayout = findViewById(R.id.icon_options_layout);
        iconOptionsLayout.setVisibility(showIcons ? View.VISIBLE : View.GONE);

        LinearLayout appNamePositionLayout = findViewById(R.id.app_name_position_layout);
        appNamePositionLayout.setVisibility(showAppNames ? View.VISIBLE : View.GONE);

        LinearLayout iconEffectColorLayout = findViewById(R.id.icon_effect_color_layout);
        iconEffectColorLayout.setVisibility(iconEffect > 0 ? View.VISIBLE : View.GONE);

        LinearLayout dynamicColorsLayout = findViewById(R.id.dynamic_colors_layout);
        dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout invertIconColorsLayout = findViewById(R.id.invert_icon_colors_layout);
        invertIconColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout iconBackgroundLayout = findViewById(R.id.icon_background_layout);
        iconBackgroundLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout iconShapeLayout = findViewById(R.id.icon_shape_layout);
        iconShapeLayout.setVisibility((dynamicIcons && iconBackground) ? View.VISIBLE : View.GONE);
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
