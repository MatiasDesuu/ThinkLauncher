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
import org.matiasdesu.thinklauncherv2.utils.AppNamePositionHelper;
import org.matiasdesu.thinklauncherv2.utils.IconShapeHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class IconSettingsActivity extends BaseSettingsActivity {

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Black", "White", "Dynamic Dark", "Dynamic White" };

    private boolean showIcons;
    private boolean showAppNames;
    private int appNamePosition;
    private boolean monochromeIcons;
    private boolean dynamicIcons;
    private boolean forceMonochromeFallback;
    private boolean dynamicColors;
    private boolean iconBackground;
    private int iconShape;
    private int iconSize;
    private int iconEffect;
    private int iconEffectColor;

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
    protected int getLayoutResId() {
        return R.layout.activity_icon_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        showIcons = prefs.getBoolean("show_icons", false);
        showAppNames = prefs.getBoolean("show_app_names", true);
        if (!showIcons)
            showAppNames = true;
        appNamePosition = prefs.getInt("app_name_position", AppNamePositionHelper.POSITION_RIGHT);
        monochromeIcons = prefs.getBoolean("monochrome_icons", false);
        dynamicIcons = prefs.getBoolean("dynamic_icons", false);
        forceMonochromeFallback = prefs.getBoolean("force_monochrome_fallback", false);
        dynamicColors = prefs.getBoolean("dynamic_colors", false);
        iconBackground = prefs.getBoolean("icon_background", true);
        iconShape = prefs.getInt("icon_shape", IconShapeHelper.SHAPE_SYSTEM);
        iconSize = prefs.getInt("icon_size", 32);
        iconEffect = prefs.getInt("icon_effect", 0);
        iconEffectColor = prefs.getInt("icon_effect_color", 0);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;

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

        LinearLayout forceMonochromeFallbackLayout = findViewById(R.id.force_monochrome_fallback_layout);
        View forceMonochromeFallbackContainer = findViewById(R.id.force_monochrome_fallback_container);
        TextView forceMonochromeFallbackValueTv = forceMonochromeFallbackContainer.findViewById(R.id.value_text);
        forceMonochromeFallbackValueTv.setText(forceMonochromeFallback ? "ON" : "OFF");
        forceMonochromeFallbackValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(forceMonochromeFallbackValueTv, new String[] { "ON", "OFF" }));
        forceMonochromeFallbackLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout dynamicColorsLayout = findViewById(R.id.dynamic_colors_layout);
        View dynamicColorsContainer = findViewById(R.id.dynamic_colors_container);
        TextView dynamicColorsValueTv = dynamicColorsContainer.findViewById(R.id.value_text);
        dynamicColorsValueTv.setText(dynamicColors ? "ON" : "OFF");
        dynamicColorsValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(dynamicColorsValueTv, new String[] { "ON", "OFF" }));
        dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout iconBackgroundLayout = findViewById(R.id.icon_background_layout);
        View iconBackgroundContainer = findViewById(R.id.icon_background_container);
        TextView iconBackgroundValueTv = iconBackgroundContainer.findViewById(R.id.value_text);
        iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
        iconBackgroundValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconBackgroundValueTv, new String[] { "ON", "OFF" }));
        iconBackgroundLayout.setVisibility(View.VISIBLE);

        LinearLayout iconShapeLayout = findViewById(R.id.icon_shape_layout);
        View iconShapeContainer = findViewById(R.id.icon_shape_container);
        TextView iconShapeValueTv = iconShapeContainer.findViewById(R.id.value_text);
        iconShapeValueTv.setText(IconShapeHelper.getShapeName(iconShape));
        iconShapeValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconShapeValueTv, IconShapeHelper.SHAPE_NAMES));
        iconShapeLayout.setVisibility(iconBackground ? View.VISIBLE : View.GONE);

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
        TextView minusForceMonochromeFallbackBtn = forceMonochromeFallbackContainer.findViewById(R.id.btn_minus);
        TextView plusForceMonochromeFallbackBtn = forceMonochromeFallbackContainer.findViewById(R.id.btn_plus);
        TextView minusDynamicColorsBtn = dynamicColorsContainer.findViewById(R.id.btn_minus);
        TextView plusDynamicColorsBtn = dynamicColorsContainer.findViewById(R.id.btn_plus);
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
            refreshPagination();
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
            refreshPagination();
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
            refreshPagination();
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
            refreshPagination();
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
            forceMonochromeFallbackLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            // iconBackgroundLayout is always visible now
            // iconShapeLayout visibility depends only on iconBackground
            prefs.edit().putBoolean("dynamic_icons", dynamicIcons).apply();
            refreshPagination();
        });

        plusDynamicBtn.setOnClickListener(v -> {
            dynamicIcons = !dynamicIcons;
            dynamicIconsValueTv.setText(dynamicIcons ? "ON" : "OFF");
            forceMonochromeFallbackLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
            dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);
             // iconBackgroundLayout is always visible now
            // iconShapeLayout visibility depends only on iconBackground
            prefs.edit().putBoolean("dynamic_icons", dynamicIcons).apply();
            refreshPagination();
        });

        minusForceMonochromeFallbackBtn.setOnClickListener(v -> {
            forceMonochromeFallback = !forceMonochromeFallback;
            forceMonochromeFallbackValueTv.setText(forceMonochromeFallback ? "ON" : "OFF");
            prefs.edit().putBoolean("force_monochrome_fallback", forceMonochromeFallback).apply();
        });

        plusForceMonochromeFallbackBtn.setOnClickListener(v -> {
            forceMonochromeFallback = !forceMonochromeFallback;
            forceMonochromeFallbackValueTv.setText(forceMonochromeFallback ? "ON" : "OFF");
            prefs.edit().putBoolean("force_monochrome_fallback", forceMonochromeFallback).apply();
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
            refreshPagination();
        });

        plusIconEffectBtn.setOnClickListener(v -> {
            iconEffect = (iconEffect + 1) % EFFECT_NAMES.length;
            iconEffectValueTv.setText(EFFECT_NAMES[iconEffect]);
            prefs.edit().putInt("icon_effect", iconEffect).apply();
            refreshPagination();
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

        initPagination(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout iconOptionsLayout = findViewById(R.id.icon_options_layout);
        iconOptionsLayout.setVisibility(showIcons ? View.VISIBLE : View.GONE);

        LinearLayout appNamePositionLayout = findViewById(R.id.app_name_position_layout);
        appNamePositionLayout.setVisibility(showAppNames ? View.VISIBLE : View.GONE);

        LinearLayout iconEffectColorLayout = findViewById(R.id.icon_effect_color_layout);
        iconEffectColorLayout.setVisibility(iconEffect > 0 ? View.VISIBLE : View.GONE);

        LinearLayout forceMonochromeFallbackLayout = findViewById(R.id.force_monochrome_fallback_layout);
        forceMonochromeFallbackLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout dynamicColorsLayout = findViewById(R.id.dynamic_colors_layout);
        dynamicColorsLayout.setVisibility(dynamicIcons ? View.VISIBLE : View.GONE);

        LinearLayout iconBackgroundLayout = findViewById(R.id.icon_background_layout);
        iconBackgroundLayout.setVisibility(View.VISIBLE);

        LinearLayout iconShapeLayout = findViewById(R.id.icon_shape_layout);
        iconShapeLayout.setVisibility(iconBackground ? View.VISIBLE : View.GONE);
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
