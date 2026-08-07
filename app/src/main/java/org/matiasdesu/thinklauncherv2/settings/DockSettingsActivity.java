package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.RepeatListener;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class DockSettingsActivity extends BaseSettingsActivity {

    private static final int ICON_SIZE_MIN = 16;
    private static final int ICON_SIZE_MAX = 64;
    private static final int NUM_APPS_MIN = 1;
    private static final int NUM_APPS_MAX = 10;

    private static final String PREFIX = "dock";

    private int enabled;
    private int iconSize;
    private int numApps;
    private int border;
    private int background;
    private int backdropOpacity;
    private int backdropBlur;

    private String p(String key) {
        return PREFIX + "_" + key;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_dock_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        enabled = prefs.getInt(p("enabled"), 0);
        iconSize = prefs.getInt(p("icon_size"), 24);
        numApps = prefs.getInt(p("num_apps"), 4);
        border = prefs.getInt(p("border"), 0);
        background = prefs.getInt(p("background"), 0);
        backdropOpacity = prefs.getInt(p("backdrop_opacity"), 0);
        backdropBlur = prefs.getInt(p("backdrop_blur"), 0);

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

        View backdropOpacityContainer = findViewById(R.id.backdrop_opacity_container);
        TextView backdropOpacityValueTv = backdropOpacityContainer.findViewById(R.id.value_text);
        backdropOpacityValueTv.setText(backdropOpacity == 1 ? "ON" : "OFF");
        backdropOpacityValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(backdropOpacityValueTv, new String[] { "ON", "OFF" }));

        View backdropBlurContainer = findViewById(R.id.backdrop_blur_container);
        TextView backdropBlurValueTv = backdropBlurContainer.findViewById(R.id.value_text);
        backdropBlurValueTv.setText(backdropBlur == 1 ? "ON" : "OFF");
        backdropBlurValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(backdropBlurValueTv, new String[] { "ON", "OFF" }));

        ImageButton minusEnabled = enabledContainer.findViewById(R.id.btn_minus);
        ImageButton plusEnabled = enabledContainer.findViewById(R.id.btn_plus);
        ImageButton minusIconSize = iconSizeContainer.findViewById(R.id.btn_minus);
        ImageButton plusIconSize = iconSizeContainer.findViewById(R.id.btn_plus);
        ImageButton minusNumApps = numAppsContainer.findViewById(R.id.btn_minus);
        ImageButton plusNumApps = numAppsContainer.findViewById(R.id.btn_plus);
        ImageButton minusBorder = borderContainer.findViewById(R.id.btn_minus);
        ImageButton plusBorder = borderContainer.findViewById(R.id.btn_plus);
        ImageButton minusBackground = backgroundContainer.findViewById(R.id.btn_minus);
        ImageButton plusBackground = backgroundContainer.findViewById(R.id.btn_plus);
        ImageButton minusBackdropOpacity = backdropOpacityContainer.findViewById(R.id.btn_minus);
        ImageButton plusBackdropOpacity = backdropOpacityContainer.findViewById(R.id.btn_plus);
        ImageButton minusBackdropBlur = backdropBlurContainer.findViewById(R.id.btn_minus);
        ImageButton plusBackdropBlur = backdropBlurContainer.findViewById(R.id.btn_plus);

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

        minusBackdropOpacity.setOnClickListener(v -> {
            backdropOpacity = backdropOpacity == 1 ? 0 : 1;
            backdropOpacityValueTv.setText(backdropOpacity == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("backdrop_opacity"), backdropOpacity).apply();
            refreshVisibility();
        });
        plusBackdropOpacity.setOnClickListener(v -> {
            backdropOpacity = backdropOpacity == 1 ? 0 : 1;
            backdropOpacityValueTv.setText(backdropOpacity == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("backdrop_opacity"), backdropOpacity).apply();
            refreshVisibility();
        });

        minusBackdropBlur.setOnClickListener(v -> {
            backdropBlur = backdropBlur == 1 ? 0 : 1;
            backdropBlurValueTv.setText(backdropBlur == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("backdrop_blur"), backdropBlur).apply();
        });
        plusBackdropBlur.setOnClickListener(v -> {
            backdropBlur = backdropBlur == 1 ? 0 : 1;
            backdropBlurValueTv.setText(backdropBlur == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("backdrop_blur"), backdropBlur).apply();
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

        initPagination(this::refreshVisibility);

        refreshVisibility();
    }

    private void refreshVisibility() {
        View[] views = {
                findViewById(R.id.icon_size_layout),
                findViewById(R.id.num_apps_layout),
                findViewById(R.id.border_layout),
                findViewById(R.id.background_layout),
                findViewById(R.id.backdrop_opacity_layout),
                findViewById(R.id.backdrop_blur_layout),
                findViewById(R.id.style_button),
                findViewById(R.id.select_apps_button)
        };
        int visibility = enabled == 1 ? View.VISIBLE : View.GONE;
        for (View v : views) {
            v.setVisibility(visibility);
        }
        findViewById(R.id.backdrop_blur_layout).setVisibility(
                enabled == 1 && backdropOpacity == 1 ? View.VISIBLE : View.GONE);
    }
}
