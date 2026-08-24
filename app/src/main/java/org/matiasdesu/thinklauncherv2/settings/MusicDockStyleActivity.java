package org.matiasdesu.thinklauncherv2.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.IconShapeHelper;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class MusicDockStyleActivity extends BaseSettingsActivity {

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Black", "White", "Dynamic Black", "Dynamic White" };
    private static final String[] COLOR_SOURCE_NAMES = { "Follow Theme", "Dark", "White", "Dynamic Dark", "Dynamic Light" };

    private static final String PREFIX = "music_dock";

    private String p(String key) {
        return PREFIX + "_" + key;
    }

    private boolean iconBackground;
    private int iconShape;
    private int iconEffect;
    private int iconEffectColor;
    private int borderColor;
    private int backgroundColor;
    private int textColor;
    private int textEffect;
    private int textEffectColor;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_music_dock_style;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        iconBackground = prefs.getBoolean(p("icon_background"), true);
        iconShape = prefs.getInt(p("icon_shape"), IconShapeHelper.SHAPE_SYSTEM);
        iconEffect = prefs.getInt(p("icon_effect"), 0);
        iconEffectColor = prefs.getInt(p("icon_effect_color"), 0);
        borderColor = prefs.getInt(p("border_color"), 0);
        backgroundColor = prefs.getInt(p("background_color"), 0);
        textColor = prefs.getInt(p("text_color"), 0);
        textEffect = prefs.getInt(p("text_effect"), 0);
        textEffectColor = prefs.getInt(p("text_effect_color"), 0);

        LinearLayout iconBackgroundLayout = findViewById(R.id.icon_background_layout);
        View iconBackgroundContainer = findViewById(R.id.icon_background_container);
        TextView iconBackgroundValueTv = iconBackgroundContainer.findViewById(R.id.value_text);
        iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
        iconBackgroundValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconBackgroundValueTv, new String[] { "ON", "OFF" }));

        LinearLayout iconShapeLayout = findViewById(R.id.icon_shape_layout);
        View iconShapeContainer = findViewById(R.id.icon_shape_container);
        TextView iconShapeValueTv = iconShapeContainer.findViewById(R.id.value_text);
        iconShapeValueTv.setText(IconShapeHelper.getShapeName(iconShape));
        iconShapeValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(iconShapeValueTv, IconShapeHelper.SHAPE_NAMES));

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

        View borderColorContainer = findViewById(R.id.border_color_container);
        TextView borderColorValueTv = borderColorContainer.findViewById(R.id.value_text);
        borderColorValueTv.setText(COLOR_SOURCE_NAMES[borderColor]);
        borderColorValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(borderColorValueTv, COLOR_SOURCE_NAMES));

        View backgroundColorContainer = findViewById(R.id.background_color_container);
        TextView backgroundColorValueTv = backgroundColorContainer.findViewById(R.id.value_text);
        backgroundColorValueTv.setText(COLOR_SOURCE_NAMES[backgroundColor]);
        backgroundColorValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(backgroundColorValueTv, COLOR_SOURCE_NAMES));

        View textColorContainer = findViewById(R.id.text_color_container);
        TextView textColorValueTv = textColorContainer.findViewById(R.id.value_text);
        textColorValueTv.setText(COLOR_SOURCE_NAMES[textColor]);
        textColorValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(textColorValueTv, COLOR_SOURCE_NAMES));

        View textEffectContainer = findViewById(R.id.text_effect_container);
        TextView textEffectValueTv = textEffectContainer.findViewById(R.id.value_text);
        textEffectValueTv.setText(EFFECT_NAMES[textEffect]);
        textEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(textEffectValueTv, EFFECT_NAMES));

        LinearLayout textEffectColorLayout = findViewById(R.id.text_effect_color_layout);
        View textEffectColorContainer = findViewById(R.id.text_effect_color_container);
        TextView textEffectColorValueTv = textEffectColorContainer.findViewById(R.id.value_text);
        textEffectColorValueTv.setText(EFFECT_COLOR_NAMES[textEffectColor]);
        textEffectColorValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(textEffectColorValueTv, EFFECT_COLOR_NAMES));

        ImageButton minusIconBackground = iconBackgroundContainer.findViewById(R.id.btn_minus);
        ImageButton plusIconBackground = iconBackgroundContainer.findViewById(R.id.btn_plus);
        ImageButton minusIconShape = iconShapeContainer.findViewById(R.id.btn_minus);
        ImageButton plusIconShape = iconShapeContainer.findViewById(R.id.btn_plus);
        ImageButton minusIconEffect = iconEffectContainer.findViewById(R.id.btn_minus);
        ImageButton plusIconEffect = iconEffectContainer.findViewById(R.id.btn_plus);
        ImageButton minusIconEffectColor = iconEffectColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusIconEffectColor = iconEffectColorContainer.findViewById(R.id.btn_plus);
        ImageButton minusBorderColor = borderColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusBorderColor = borderColorContainer.findViewById(R.id.btn_plus);
        ImageButton minusBackgroundColor = backgroundColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusBackgroundColor = backgroundColorContainer.findViewById(R.id.btn_plus);
        ImageButton minusTextColor = textColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusTextColor = textColorContainer.findViewById(R.id.btn_plus);
        ImageButton minusTextEffect = textEffectContainer.findViewById(R.id.btn_minus);
        ImageButton plusTextEffect = textEffectContainer.findViewById(R.id.btn_plus);
        ImageButton minusTextEffectColor = textEffectColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusTextEffectColor = textEffectColorContainer.findViewById(R.id.btn_plus);

        minusIconBackground.setOnClickListener(v -> {
            iconBackground = !iconBackground;
            iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
            prefs.edit().putBoolean(p("icon_background"), iconBackground).apply();
            refreshVisibility();
        });
        plusIconBackground.setOnClickListener(v -> {
            iconBackground = !iconBackground;
            iconBackgroundValueTv.setText(iconBackground ? "ON" : "OFF");
            prefs.edit().putBoolean(p("icon_background"), iconBackground).apply();
            refreshVisibility();
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
            prefs.edit().putInt(p("icon_effect"), iconEffect).apply();
            refreshVisibility();
        });
        plusIconEffect.setOnClickListener(v -> {
            iconEffect = (iconEffect + 1) % EFFECT_NAMES.length;
            iconEffectValueTv.setText(EFFECT_NAMES[iconEffect]);
            prefs.edit().putInt(p("icon_effect"), iconEffect).apply();
            refreshVisibility();
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

        minusTextColor.setOnClickListener(v -> {
            textColor = (textColor - 1 + COLOR_SOURCE_NAMES.length) % COLOR_SOURCE_NAMES.length;
            textColorValueTv.setText(COLOR_SOURCE_NAMES[textColor]);
            prefs.edit().putInt(p("text_color"), textColor).apply();
        });
        plusTextColor.setOnClickListener(v -> {
            textColor = (textColor + 1) % COLOR_SOURCE_NAMES.length;
            textColorValueTv.setText(COLOR_SOURCE_NAMES[textColor]);
            prefs.edit().putInt(p("text_color"), textColor).apply();
        });

        minusTextEffect.setOnClickListener(v -> {
            textEffect = (textEffect - 1 + EFFECT_NAMES.length) % EFFECT_NAMES.length;
            textEffectValueTv.setText(EFFECT_NAMES[textEffect]);
            prefs.edit().putInt(p("text_effect"), textEffect).apply();
            refreshVisibility();
        });
        plusTextEffect.setOnClickListener(v -> {
            textEffect = (textEffect + 1) % EFFECT_NAMES.length;
            textEffectValueTv.setText(EFFECT_NAMES[textEffect]);
            prefs.edit().putInt(p("text_effect"), textEffect).apply();
            refreshVisibility();
        });

        minusTextEffectColor.setOnClickListener(v -> {
            textEffectColor = (textEffectColor - 1 + EFFECT_COLOR_NAMES.length) % EFFECT_COLOR_NAMES.length;
            textEffectColorValueTv.setText(EFFECT_COLOR_NAMES[textEffectColor]);
            prefs.edit().putInt(p("text_effect_color"), textEffectColor).apply();
        });
        plusTextEffectColor.setOnClickListener(v -> {
            textEffectColor = (textEffectColor + 1) % EFFECT_COLOR_NAMES.length;
            textEffectColorValueTv.setText(EFFECT_COLOR_NAMES[textEffectColor]);
            prefs.edit().putInt(p("text_effect_color"), textEffectColor).apply();
        });

        initPagination(this::refreshVisibility);
        refreshVisibility();
    }

    private void refreshVisibility() {
        LinearLayout iconShapeLayout = findViewById(R.id.icon_shape_layout);
        LinearLayout iconEffectColorLayout = findViewById(R.id.icon_effect_color_layout);
        LinearLayout textEffectColorLayout = findViewById(R.id.text_effect_color_layout);
        iconShapeLayout.setVisibility(iconBackground ? View.VISIBLE : View.GONE);
        iconEffectColorLayout.setVisibility(iconEffect > 0 ? View.VISIBLE : View.GONE);
        textEffectColorLayout.setVisibility(textEffect > 0 ? View.VISIBLE : View.GONE);
    }
}
