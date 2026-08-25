package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.RepeatListener;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class MusicDockSettingsActivity extends BaseSettingsActivity {

    public static final String EXTRA_FROM_HOME = "from_home";

    private static final int ICON_SIZE_MIN = 16;
    private static final int ICON_SIZE_MAX = 64;
    private static final int TEXT_SIZE_MIN = 10;
    private static final int TEXT_SIZE_MAX = 48;
    private static final int HIDE_DELAY_MIN = 0;
    private static final int HIDE_DELAY_MAX = 120;

    private static final String PREFIX = "music_dock";

    private int enabled;
    private int orientation;
    private int iconSize;
    private int textSize;
    private int border;
    private int background;
    private int backdropOpacity;
    private int backdropBlur;
    private int keepActive;
    private int hideDelay;

    private String p(String key) {
        return PREFIX + "_" + key;
    }

    private boolean fromHome;

    @Override
    public void onBackPressed() {
        if (fromHome) {
            finish();
            overridePendingTransition(0, screenAnimations ? R.anim.dialog_fade_out : 0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_music_dock_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fromHome = getIntent().getBooleanExtra(EXTRA_FROM_HOME, false);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        enabled = prefs.getInt(p("enabled"), 0);
        orientation = prefs.getInt(p("orientation"), 0);
        iconSize = prefs.getInt(p("icon_size"), 20);
        textSize = prefs.getInt(p("text_size"), 16);
        border = prefs.getInt(p("border"), 0);
        background = prefs.getInt(p("background"), 0);
        backdropOpacity = prefs.getInt(p("backdrop_opacity"), 0);
        backdropBlur = prefs.getInt(p("backdrop_blur"), 0);
        keepActive = prefs.getInt(p("keep_active"), 0);
        hideDelay = prefs.getInt(p("hide_delay"), 0);

        View enabledContainer = findViewById(R.id.enabled_container);
        TextView enabledValueTv = enabledContainer.findViewById(R.id.value_text);
        enabledValueTv.setText(enabled == 1 ? "ON" : "OFF");
        enabledValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(enabledValueTv, new String[] { "ON", "OFF" }));

        View notificationAccessButton = findViewById(R.id.notification_access_button);
        notificationAccessButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception e) {
                // No handler for this action
            }
        });

        View positionButton = findViewById(R.id.position_button);
        positionButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppBarPositionActivity.class);
            intent.putExtra(AppBarPositionActivity.EXTRA_PREF_KEY, p("position"));
            intent.putExtra(AppBarPositionActivity.EXTRA_DEFAULT_POSITION, 7);
            intent.putExtra(AppBarPositionActivity.EXTRA_TITLE, "Music Dock Position");
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        View styleButton = findViewById(R.id.style_button);
        styleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MusicDockStyleActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        View orientationContainer = findViewById(R.id.orientation_container);
        View iconSizeContainer = findViewById(R.id.icon_size_container);
        View textSizeContainer = findViewById(R.id.text_size_container);
        refreshOrientationValue();
        refreshIconSizeValue();
        refreshTextSizeValue();

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

        View keepActiveContainer = findViewById(R.id.keep_active_container);
        TextView keepActiveValueTv = keepActiveContainer.findViewById(R.id.value_text);
        keepActiveValueTv.setText(keepActive == 1 ? "ON" : "OFF");
        keepActiveValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(keepActiveValueTv, new String[] { "ON", "OFF" }));

        View hideDelayContainer = findViewById(R.id.hide_delay_container);
        TextView hideDelayValueTv = hideDelayContainer.findViewById(R.id.value_text);
        hideDelayValueTv.setText(hideDelay + "s");

        ImageButton minusEnabled = enabledContainer.findViewById(R.id.btn_minus);
        ImageButton plusEnabled = enabledContainer.findViewById(R.id.btn_plus);
        ImageButton minusOrientation = orientationContainer.findViewById(R.id.btn_minus);
        ImageButton plusOrientation = orientationContainer.findViewById(R.id.btn_plus);
        ImageButton minusIconSize = iconSizeContainer.findViewById(R.id.btn_minus);
        ImageButton plusIconSize = iconSizeContainer.findViewById(R.id.btn_plus);
        ImageButton minusTextSize = textSizeContainer.findViewById(R.id.btn_minus);
        ImageButton plusTextSize = textSizeContainer.findViewById(R.id.btn_plus);
        ImageButton minusBorder = borderContainer.findViewById(R.id.btn_minus);
        ImageButton plusBorder = borderContainer.findViewById(R.id.btn_plus);
        ImageButton minusBackground = backgroundContainer.findViewById(R.id.btn_minus);
        ImageButton plusBackground = backgroundContainer.findViewById(R.id.btn_plus);
        ImageButton minusBackdropOpacity = backdropOpacityContainer.findViewById(R.id.btn_minus);
        ImageButton plusBackdropOpacity = backdropOpacityContainer.findViewById(R.id.btn_plus);
        ImageButton minusBackdropBlur = backdropBlurContainer.findViewById(R.id.btn_minus);
        ImageButton plusBackdropBlur = backdropBlurContainer.findViewById(R.id.btn_plus);
        ImageButton minusKeepActive = keepActiveContainer.findViewById(R.id.btn_minus);
        ImageButton plusKeepActive = keepActiveContainer.findViewById(R.id.btn_plus);
        ImageButton minusHideDelay = hideDelayContainer.findViewById(R.id.btn_minus);
        ImageButton plusHideDelay = hideDelayContainer.findViewById(R.id.btn_plus);

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

        minusOrientation.setOnClickListener(v -> {
            orientation = orientation == 1 ? 0 : 1;
            refreshOrientationValue();
            prefs.edit().putInt(p("orientation"), orientation).apply();
        });
        plusOrientation.setOnClickListener(v -> {
            orientation = orientation == 1 ? 0 : 1;
            refreshOrientationValue();
            prefs.edit().putInt(p("orientation"), orientation).apply();
        });

        minusIconSize.setOnTouchListener(new RepeatListener(v -> {
            if (iconSize > ICON_SIZE_MIN) {
                iconSize--;
                refreshIconSizeValue();
                prefs.edit().putInt(p("icon_size"), iconSize).apply();
            }
        }));
        plusIconSize.setOnTouchListener(new RepeatListener(v -> {
            if (iconSize < ICON_SIZE_MAX) {
                iconSize++;
                refreshIconSizeValue();
                prefs.edit().putInt(p("icon_size"), iconSize).apply();
            }
        }));

        minusTextSize.setOnTouchListener(new RepeatListener(v -> {
            if (textSize > TEXT_SIZE_MIN) {
                textSize--;
                refreshTextSizeValue();
                prefs.edit().putInt(p("text_size"), textSize).apply();
            }
        }));
        plusTextSize.setOnTouchListener(new RepeatListener(v -> {
            if (textSize < TEXT_SIZE_MAX) {
                textSize++;
                refreshTextSizeValue();
                prefs.edit().putInt(p("text_size"), textSize).apply();
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
            refreshVisibility();
        });
        plusBackground.setOnClickListener(v -> {
            background = background == 1 ? 0 : 1;
            backgroundValueTv.setText(background == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("background"), background).apply();
            refreshVisibility();
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

        minusKeepActive.setOnClickListener(v -> {
            keepActive = keepActive == 1 ? 0 : 1;
            keepActiveValueTv.setText(keepActive == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("keep_active"), keepActive).apply();
            refreshVisibility();
        });
        plusKeepActive.setOnClickListener(v -> {
            keepActive = keepActive == 1 ? 0 : 1;
            keepActiveValueTv.setText(keepActive == 1 ? "ON" : "OFF");
            prefs.edit().putInt(p("keep_active"), keepActive).apply();
            refreshVisibility();
        });

        minusHideDelay.setOnTouchListener(new RepeatListener(v -> {
            if (hideDelay > HIDE_DELAY_MIN) {
                hideDelay--;
                hideDelayValueTv.setText(hideDelay + "s");
                prefs.edit().putInt(p("hide_delay"), hideDelay).apply();
            }
        }));
        plusHideDelay.setOnTouchListener(new RepeatListener(v -> {
            if (hideDelay < HIDE_DELAY_MAX) {
                hideDelay++;
                hideDelayValueTv.setText(hideDelay + "s");
                prefs.edit().putInt(p("hide_delay"), hideDelay).apply();
            }
        }));

        initPagination(this::refreshVisibility);

        refreshVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private TextView refreshOrientationValue() {
        View container = findViewById(R.id.orientation_container);
        TextView tv = container.findViewById(R.id.value_text);
        tv.setText(orientation == 1 ? "Vertical" : "Horizontal");
        tv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(tv, new String[] { "Horizontal", "Vertical" }));
        return tv;
    }

    private TextView refreshIconSizeValue() {
        View container = findViewById(R.id.icon_size_container);
        TextView tv = container.findViewById(R.id.value_text);
        tv.setText(String.valueOf(iconSize));
        return tv;
    }

    private TextView refreshTextSizeValue() {
        View container = findViewById(R.id.text_size_container);
        TextView tv = container.findViewById(R.id.value_text);
        tv.setText(String.valueOf(textSize));
        return tv;
    }

    private boolean hasNotificationListenerAccess() {
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (flat != null) {
            String[] names = flat.split(":");
            for (String name : names) {
                android.content.ComponentName cn = android.content.ComponentName.unflattenFromString(name);
                if (cn != null && getPackageName().equals(cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void refreshVisibility() {
        View[] views = {
                findViewById(R.id.orientation_layout),
                findViewById(R.id.icon_size_layout),
                findViewById(R.id.text_size_layout),
                findViewById(R.id.border_layout),
                findViewById(R.id.background_layout),
                findViewById(R.id.style_button),
                findViewById(R.id.position_button)
        };
        int visibility = enabled == 1 ? View.VISIBLE : View.GONE;
        for (View v : views) {
            v.setVisibility(visibility);
        }
        findViewById(R.id.backdrop_opacity_layout).setVisibility(
                enabled == 1 && background == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.backdrop_blur_layout).setVisibility(
                enabled == 1 && background == 1 && backdropOpacity == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.keep_active_layout).setVisibility(visibility);
        findViewById(R.id.hide_delay_layout).setVisibility(
                enabled == 1 && keepActive == 0 ? View.VISIBLE : View.GONE);
    }
}
