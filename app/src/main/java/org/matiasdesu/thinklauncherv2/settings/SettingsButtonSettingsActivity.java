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
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;

import android.os.Build;

public class SettingsButtonSettingsActivity extends AppCompatActivity {

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Dark", "White", "Dynamic Dark", "Dynamic Light" };

    private int showSettingsButton;
    private int settingsButtonSize;
    private int settingsButtonColor;
    private int settingsButtonEffect;
    private int settingsButtonEffectColor;
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
                    Intent mainIntent = new Intent(SettingsButtonSettingsActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_settings_button_settings);

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

        showSettingsButton = prefs.getInt("show_settings_button", 0);
        settingsButtonSize = prefs.getInt("settings_button_size", 42);
        settingsButtonColor = prefs.getInt("settings_button_color", 0);
        settingsButtonEffect = prefs.getInt("settings_button_effect", 0);
        settingsButtonEffectColor = prefs.getInt("settings_button_effect_color", 0);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View showSettingsButtonContainer = findViewById(R.id.show_settings_button_container);
        TextView showSettingsButtonValueTv = showSettingsButtonContainer.findViewById(R.id.value_text);
        showSettingsButtonValueTv.setText(getOnOffText(showSettingsButton));
        showSettingsButtonValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(showSettingsButtonValueTv, new String[] { "OFF", "ON" }));

        View settingsButtonSizeContainer = findViewById(R.id.settings_button_size_container);
        TextView settingsButtonSizeValueTv = settingsButtonSizeContainer.findViewById(R.id.value_text);
        settingsButtonSizeValueTv.setText(String.valueOf(settingsButtonSize));

        View settingsButtonColorContainer = findViewById(R.id.settings_button_color_container);
        TextView settingsButtonColorValueTv = settingsButtonColorContainer.findViewById(R.id.value_text);
        settingsButtonColorValueTv.setText(getSettingsButtonColorText(settingsButtonColor));
        settingsButtonColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(settingsButtonColorValueTv,
                new String[] { "FOLLOW THEME", "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC LIGHT" }));

        View settingsButtonEffectContainer = findViewById(R.id.settings_button_effect_container);
        TextView settingsButtonEffectValueTv = settingsButtonEffectContainer.findViewById(R.id.value_text);
        settingsButtonEffectValueTv.setText(EFFECT_NAMES[settingsButtonEffect]);
        settingsButtonEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(settingsButtonEffectValueTv, EFFECT_NAMES));

        View settingsButtonEffectColorContainer = findViewById(R.id.settings_button_effect_color_container);
        TextView settingsButtonEffectColorValueTv = settingsButtonEffectColorContainer.findViewById(R.id.value_text);
        settingsButtonEffectColorValueTv.setText(EFFECT_COLOR_NAMES[settingsButtonEffectColor]);
        settingsButtonEffectColorValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(settingsButtonEffectColorValueTv, EFFECT_COLOR_NAMES));

        TextView minusShowSettingsBtn = showSettingsButtonContainer.findViewById(R.id.btn_minus);
        TextView plusShowSettingsBtn = showSettingsButtonContainer.findViewById(R.id.btn_plus);
        TextView minusSettingsButtonSizeBtn = settingsButtonSizeContainer.findViewById(R.id.btn_minus);
        TextView plusSettingsButtonSizeBtn = settingsButtonSizeContainer.findViewById(R.id.btn_plus);
        TextView minusSettingsButtonColorBtn = settingsButtonColorContainer.findViewById(R.id.btn_minus);
        TextView plusSettingsButtonColorBtn = settingsButtonColorContainer.findViewById(R.id.btn_plus);
        TextView minusSettingsButtonEffectBtn = settingsButtonEffectContainer.findViewById(R.id.btn_minus);
        TextView plusSettingsButtonEffectBtn = settingsButtonEffectContainer.findViewById(R.id.btn_plus);
        TextView minusSettingsButtonEffectColorBtn = settingsButtonEffectColorContainer.findViewById(R.id.btn_minus);
        TextView plusSettingsButtonEffectColorBtn = settingsButtonEffectColorContainer.findViewById(R.id.btn_plus);

        minusShowSettingsBtn.setOnClickListener(v -> {
            showSettingsButton = (showSettingsButton - 1 + 2) % 2;
            showSettingsButtonValueTv.setText(getOnOffText(showSettingsButton));
            prefs.edit().putInt("show_settings_button", showSettingsButton).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusShowSettingsBtn.setOnClickListener(v -> {
            showSettingsButton = (showSettingsButton + 1) % 2;
            showSettingsButtonValueTv.setText(getOnOffText(showSettingsButton));
            prefs.edit().putInt("show_settings_button", showSettingsButton).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusSettingsButtonSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (settingsButtonSize > 10) {
                settingsButtonSize--;
                settingsButtonSizeValueTv.setText(String.valueOf(settingsButtonSize));
                prefs.edit().putInt("settings_button_size", settingsButtonSize).apply();
            }
        }));

        plusSettingsButtonSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (settingsButtonSize < 100) {
                settingsButtonSize++;
                settingsButtonSizeValueTv.setText(String.valueOf(settingsButtonSize));
                prefs.edit().putInt("settings_button_size", settingsButtonSize).apply();
            }
        }));

        minusSettingsButtonColorBtn.setOnClickListener(v -> {
            settingsButtonColor = (settingsButtonColor - 1 + 5) % 5;
            settingsButtonColorValueTv.setText(getSettingsButtonColorText(settingsButtonColor));
            prefs.edit().putInt("settings_button_color", settingsButtonColor).apply();
        });

        plusSettingsButtonColorBtn.setOnClickListener(v -> {
            settingsButtonColor = (settingsButtonColor + 1) % 5;
            settingsButtonColorValueTv.setText(getSettingsButtonColorText(settingsButtonColor));
            prefs.edit().putInt("settings_button_color", settingsButtonColor).apply();
        });

        minusSettingsButtonEffectBtn.setOnClickListener(v -> {
            settingsButtonEffect = (settingsButtonEffect - 1 + EFFECT_NAMES.length) % EFFECT_NAMES.length;
            settingsButtonEffectValueTv.setText(EFFECT_NAMES[settingsButtonEffect]);
            prefs.edit().putInt("settings_button_effect", settingsButtonEffect).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusSettingsButtonEffectBtn.setOnClickListener(v -> {
            settingsButtonEffect = (settingsButtonEffect + 1) % EFFECT_NAMES.length;
            settingsButtonEffectValueTv.setText(EFFECT_NAMES[settingsButtonEffect]);
            prefs.edit().putInt("settings_button_effect", settingsButtonEffect).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusSettingsButtonEffectColorBtn.setOnClickListener(v -> {
            settingsButtonEffectColor = (settingsButtonEffectColor - 1 + EFFECT_COLOR_NAMES.length) % EFFECT_COLOR_NAMES.length;
            settingsButtonEffectColorValueTv.setText(EFFECT_COLOR_NAMES[settingsButtonEffectColor]);
            prefs.edit().putInt("settings_button_effect_color", settingsButtonEffectColor).apply();
        });

        plusSettingsButtonEffectColorBtn.setOnClickListener(v -> {
            settingsButtonEffectColor = (settingsButtonEffectColor + 1) % EFFECT_COLOR_NAMES.length;
            settingsButtonEffectColorValueTv.setText(EFFECT_COLOR_NAMES[settingsButtonEffectColor]);
            prefs.edit().putInt("settings_button_effect_color", settingsButtonEffectColor).apply();
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout settingsButtonSizeLayout = findViewById(R.id.settings_button_size_layout);
        LinearLayout settingsButtonColorLayout = findViewById(R.id.settings_button_color_layout);
        LinearLayout settingsButtonEffectLayout = findViewById(R.id.settings_button_effect_layout);
        LinearLayout settingsButtonEffectColorLayout = findViewById(R.id.settings_button_effect_color_layout);

        if (showSettingsButton == 0) {
            settingsButtonSizeLayout.setVisibility(View.GONE);
            settingsButtonColorLayout.setVisibility(View.GONE);
            settingsButtonEffectLayout.setVisibility(View.GONE);
            settingsButtonEffectColorLayout.setVisibility(View.GONE);
        } else {
            settingsButtonSizeLayout.setVisibility(View.VISIBLE);
            settingsButtonColorLayout.setVisibility(View.VISIBLE);
            settingsButtonEffectLayout.setVisibility(View.VISIBLE);
            settingsButtonEffectColorLayout.setVisibility(settingsButtonEffect > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private String getSettingsButtonColorText(int color) {
        switch (color) {
            case 0:
                return "FOLLOW THEME";
            case 1:
                return "DARK";
            case 2:
                return "WHITE";
            case 3:
                return "DYNAMIC DARK";
            case 4:
                return "DYNAMIC LIGHT";
            default:
                return "FOLLOW THEME";
        }
    }

    private String getOnOffText(int pos) {
        return pos == 1 ? "ON" : "OFF";
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
