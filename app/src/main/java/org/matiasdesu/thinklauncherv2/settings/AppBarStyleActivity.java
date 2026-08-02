package org.matiasdesu.thinklauncherv2.settings;

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
import org.matiasdesu.thinklauncherv2.utils.IconShapeHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class AppBarStyleActivity extends AppCompatActivity {

    public static final String EXTRA_PREFIX = "prefix";

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Black", "White", "Dynamic Black", "Dynamic White" };
    private static final String[] COLOR_SOURCE_NAMES = { "Follow Theme", "Dark", "White", "Dynamic Dark", "Dynamic Light" };

    private SharedPreferences prefs;
    private int theme;
    private boolean screenAnimations;
    private String prefix = "app_bar";

    private String p(String key) {
        return prefix + "_" + key;
    }

    private boolean monochrome;
    private boolean dynamic;
    private boolean forceFallback;
    private boolean dynamicColors;
    private boolean iconBackground;
    private int iconShape;
    private int iconEffect;
    private int iconEffectColor;
    private int borderColor;
    private int backgroundColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;
        prefix = getIntent().getStringExtra(EXTRA_PREFIX);
        if (prefix == null || prefix.isEmpty()) {
            prefix = "app_bar";
        }
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_bar_style);

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

        monochrome = prefs.getBoolean(p("monochrome_icons"), false);
        dynamic = prefs.getBoolean(p("dynamic_icons"), false);
        forceFallback = prefs.getBoolean(p("force_monochrome_fallback"), false);
        dynamicColors = prefs.getBoolean(p("dynamic_colors"), false);
        iconBackground = prefs.getBoolean(p("icon_background"), true);
        iconShape = prefs.getInt(p("icon_shape"), IconShapeHelper.SHAPE_SYSTEM);
        iconEffect = prefs.getInt(p("icon_effect"), 0);
        iconEffectColor = prefs.getInt(p("icon_effect_color"), 0);
        borderColor = prefs.getInt(p("border_color"), 0);
        backgroundColor = prefs.getInt(p("background_color"), 0);

        View monochromeContainer = findViewById(R.id.monochrome_icons_container);
        TextView monochromeValueTv = monochromeContainer.findViewById(R.id.value_text);
        monochromeValueTv.setText(monochrome ? "ON" : "OFF");
        monochromeValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(monochromeValueTv, new String[] { "ON", "OFF" }));

        View dynamicContainer = findViewById(R.id.dynamic_icons_container);
        TextView dynamicValueTv = dynamicContainer.findViewById(R.id.value_text);
        dynamicValueTv.setText(dynamic ? "ON" : "OFF");
        dynamicValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dynamicValueTv, new String[] { "ON", "OFF" }));

        LinearLayout forceFallbackLayout = findViewById(R.id.force_monochrome_fallback_layout);
        View forceFallbackContainer = findViewById(R.id.force_monochrome_fallback_container);
        TextView forceFallbackValueTv = forceFallbackContainer.findViewById(R.id.value_text);
        forceFallbackValueTv.setText(forceFallback ? "ON" : "OFF");
        forceFallbackValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(forceFallbackValueTv, new String[] { "ON", "OFF" }));
        forceFallbackLayout.setVisibility(dynamic ? View.VISIBLE : View.GONE);

        LinearLayout dynamicColorsLayout = findViewById(R.id.dynamic_colors_layout);
        View dynamicColorsContainer = findViewById(R.id.dynamic_colors_container);
        TextView dynamicColorsValueTv = dynamicColorsContainer.findViewById(R.id.value_text);
        dynamicColorsValueTv.setText(dynamicColors ? "ON" : "OFF");
        dynamicColorsValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(dynamicColorsValueTv, new String[] { "ON", "OFF" }));
        dynamicColorsLayout.setVisibility(dynamic ? View.VISIBLE : View.GONE);

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

        TextView minusMonochrome = monochromeContainer.findViewById(R.id.btn_minus);
        TextView plusMonochrome = monochromeContainer.findViewById(R.id.btn_plus);
        TextView minusDynamic = dynamicContainer.findViewById(R.id.btn_minus);
        TextView plusDynamic = dynamicContainer.findViewById(R.id.btn_plus);
        TextView minusForceFallback = forceFallbackContainer.findViewById(R.id.btn_minus);
        TextView plusForceFallback = forceFallbackContainer.findViewById(R.id.btn_plus);
        TextView minusDynamicColors = dynamicColorsContainer.findViewById(R.id.btn_minus);
        TextView plusDynamicColors = dynamicColorsContainer.findViewById(R.id.btn_plus);
        TextView minusIconBackground = iconBackgroundContainer.findViewById(R.id.btn_minus);
        TextView plusIconBackground = iconBackgroundContainer.findViewById(R.id.btn_plus);
        TextView minusIconShape = iconShapeContainer.findViewById(R.id.btn_minus);
        TextView plusIconShape = iconShapeContainer.findViewById(R.id.btn_plus);
        TextView minusIconEffect = iconEffectContainer.findViewById(R.id.btn_minus);
        TextView plusIconEffect = iconEffectContainer.findViewById(R.id.btn_plus);
        TextView minusIconEffectColor = iconEffectColorContainer.findViewById(R.id.btn_minus);
        TextView plusIconEffectColor = iconEffectColorContainer.findViewById(R.id.btn_plus);

        minusMonochrome.setOnClickListener(v -> {
            monochrome = !monochrome;
            monochromeValueTv.setText(monochrome ? "ON" : "OFF");
            prefs.edit().putBoolean(p("monochrome_icons"), monochrome).apply();
        });
        plusMonochrome.setOnClickListener(v -> {
            monochrome = !monochrome;
            monochromeValueTv.setText(monochrome ? "ON" : "OFF");
            prefs.edit().putBoolean(p("monochrome_icons"), monochrome).apply();
        });

        minusDynamic.setOnClickListener(v -> {
            dynamic = !dynamic;
            dynamicValueTv.setText(dynamic ? "ON" : "OFF");
            forceFallbackLayout.setVisibility(dynamic ? View.VISIBLE : View.GONE);
            dynamicColorsLayout.setVisibility(dynamic ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean(p("dynamic_icons"), dynamic).apply();
        });
        plusDynamic.setOnClickListener(v -> {
            dynamic = !dynamic;
            dynamicValueTv.setText(dynamic ? "ON" : "OFF");
            forceFallbackLayout.setVisibility(dynamic ? View.VISIBLE : View.GONE);
            dynamicColorsLayout.setVisibility(dynamic ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean(p("dynamic_icons"), dynamic).apply();
        });

        minusForceFallback.setOnClickListener(v -> {
            forceFallback = !forceFallback;
            forceFallbackValueTv.setText(forceFallback ? "ON" : "OFF");
            prefs.edit().putBoolean(p("force_monochrome_fallback"), forceFallback).apply();
        });
        plusForceFallback.setOnClickListener(v -> {
            forceFallback = !forceFallback;
            forceFallbackValueTv.setText(forceFallback ? "ON" : "OFF");
            prefs.edit().putBoolean(p("force_monochrome_fallback"), forceFallback).apply();
        });

        minusDynamicColors.setOnClickListener(v -> {
            dynamicColors = !dynamicColors;
            dynamicColorsValueTv.setText(dynamicColors ? "ON" : "OFF");
            prefs.edit().putBoolean(p("dynamic_colors"), dynamicColors).apply();
        });
        plusDynamicColors.setOnClickListener(v -> {
            dynamicColors = !dynamicColors;
            dynamicColorsValueTv.setText(dynamicColors ? "ON" : "OFF");
            prefs.edit().putBoolean(p("dynamic_colors"), dynamicColors).apply();
        });

        minusIconBackground.setOnClickListener(v -> {
            iconBackground = !iconBackground;
            iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
            iconShapeLayout.setVisibility(iconBackground ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean(p("icon_background"), iconBackground).apply();
        });
        plusIconBackground.setOnClickListener(v -> {
            iconBackground = !iconBackground;
            iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
            iconShapeLayout.setVisibility(iconBackground ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean(p("icon_background"), iconBackground).apply();
        });

        minusIconShape.setOnClickListener(v -> {
            iconShape = IconShapeHelper.getPreviousShape(iconShape);
            iconShapeValueTv.setText(IconShapeHelper.getShapeName(iconShape));
            prefs.edit().putInt(p("icon_shape"), iconShape).apply();
        });
        plusIconShape.setOnClickListener(v -> {
            iconShape = IconShapeHelper.getNextShape(iconShape);
            iconShapeValueTv.setText(IconShapeHelper.getShapeName(iconShape));
            prefs.edit().putInt(p("icon_shape"), iconShape).apply();
        });

        minusIconEffect.setOnClickListener(v -> {
            iconEffect = (iconEffect - 1 + EFFECT_NAMES.length) % EFFECT_NAMES.length;
            iconEffectValueTv.setText(EFFECT_NAMES[iconEffect]);
            iconEffectColorLayout.setVisibility(iconEffect > 0 ? View.VISIBLE : View.GONE);
            prefs.edit().putInt(p("icon_effect"), iconEffect).apply();
        });
        plusIconEffect.setOnClickListener(v -> {
            iconEffect = (iconEffect + 1) % EFFECT_NAMES.length;
            iconEffectValueTv.setText(EFFECT_NAMES[iconEffect]);
            iconEffectColorLayout.setVisibility(iconEffect > 0 ? View.VISIBLE : View.GONE);
            prefs.edit().putInt(p("icon_effect"), iconEffect).apply();
        });

        minusIconEffectColor.setOnClickListener(v -> {
            iconEffectColor = (iconEffectColor - 1 + EFFECT_COLOR_NAMES.length) % EFFECT_COLOR_NAMES.length;
            iconEffectColorValueTv.setText(EFFECT_COLOR_NAMES[iconEffectColor]);
            prefs.edit().putInt(p("icon_effect_color"), iconEffectColor).apply();
        });
        plusIconEffectColor.setOnClickListener(v -> {
            iconEffectColor = (iconEffectColor + 1) % EFFECT_COLOR_NAMES.length;
            iconEffectColorValueTv.setText(EFFECT_COLOR_NAMES[iconEffectColor]);
            prefs.edit().putInt(p("icon_effect_color"), iconEffectColor).apply();
        });

        View borderColorContainer = findViewById(R.id.border_color_container);
        TextView borderColorValueTv = borderColorContainer.findViewById(R.id.value_text);
        borderColorValueTv.setText(COLOR_SOURCE_NAMES[borderColor]);
        borderColorValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(borderColorValueTv, COLOR_SOURCE_NAMES));
        TextView minusBorderColor = borderColorContainer.findViewById(R.id.btn_minus);
        TextView plusBorderColor = borderColorContainer.findViewById(R.id.btn_plus);
        minusBorderColor.setOnClickListener(v -> {
            borderColor = (borderColor - 1 + COLOR_SOURCE_NAMES.length) % COLOR_SOURCE_NAMES.length;
            borderColorValueTv.setText(COLOR_SOURCE_NAMES[borderColor]);
            prefs.edit().putInt(p("border_color"), borderColor).apply();
        });
        plusBorderColor.setOnClickListener(v -> {
            borderColor = (borderColor + 1) % COLOR_SOURCE_NAMES.length;
            borderColorValueTv.setText(COLOR_SOURCE_NAMES[borderColor]);
            prefs.edit().putInt(p("border_color"), borderColor).apply();
        });

        View backgroundColorContainer = findViewById(R.id.background_color_container);
        TextView backgroundColorValueTv = backgroundColorContainer.findViewById(R.id.value_text);
        backgroundColorValueTv.setText(COLOR_SOURCE_NAMES[backgroundColor]);
        backgroundColorValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(backgroundColorValueTv, COLOR_SOURCE_NAMES));
        TextView minusBackgroundColor = backgroundColorContainer.findViewById(R.id.btn_minus);
        TextView plusBackgroundColor = backgroundColorContainer.findViewById(R.id.btn_plus);
        minusBackgroundColor.setOnClickListener(v -> {
            backgroundColor = (backgroundColor - 1 + COLOR_SOURCE_NAMES.length) % COLOR_SOURCE_NAMES.length;
            backgroundColorValueTv.setText(COLOR_SOURCE_NAMES[backgroundColor]);
            prefs.edit().putInt(p("background_color"), backgroundColor).apply();
        });
        plusBackgroundColor.setOnClickListener(v -> {
            backgroundColor = (backgroundColor + 1) % COLOR_SOURCE_NAMES.length;
            backgroundColorValueTv.setText(COLOR_SOURCE_NAMES[backgroundColor]);
            prefs.edit().putInt(p("background_color"), backgroundColor).apply();
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