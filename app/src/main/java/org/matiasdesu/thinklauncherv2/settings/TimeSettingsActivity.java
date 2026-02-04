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

public class TimeSettingsActivity extends AppCompatActivity {

    private int timePosition;
    private int timeHorizontalPosition;
    private int timeFontSize;
    private int timeColor;
    private int timeEffect;
    private int timeEffectColor;
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
                    Intent mainIntent = new Intent(TimeSettingsActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_time_settings);

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

        timePosition = prefs.getInt("time_position", 0);
        timeHorizontalPosition = prefs.getInt("time_horizontal_position", 0);
        timeFontSize = prefs.getInt("time_font_size", 54);
        timeColor = prefs.getInt("time_color", 0);
        timeEffect = prefs.getInt("time_effect", 0);
        timeEffectColor = prefs.getInt("time_effect_color", 0);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View timePositionContainer = findViewById(R.id.time_position_container);
        TextView timePositionValueTv = timePositionContainer.findViewById(R.id.value_text);
        timePositionValueTv.setText(getTimePositionText(timePosition));
        timePositionValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(timePositionValueTv, new String[] { "OFF", "ON" }));

        View timeHorizontalContainer = findViewById(R.id.time_horizontal_container);
        TextView timeHorizontalValueTv = timeHorizontalContainer.findViewById(R.id.value_text);
        timeHorizontalValueTv.setText(getTimeHorizontalPositionText(timeHorizontalPosition));
        timeHorizontalValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(timeHorizontalValueTv, new String[] { "LEFT", "CENTER", "RIGHT" }));

        View timeFontSizeContainer = findViewById(R.id.time_font_size_container);
        TextView timeFontSizeValueTv = timeFontSizeContainer.findViewById(R.id.value_text);
        timeFontSizeValueTv.setText(String.valueOf(timeFontSize));

        TextView minusTimeBtn = timePositionContainer.findViewById(R.id.btn_minus);
        TextView plusTimeBtn = timePositionContainer.findViewById(R.id.btn_plus);
        TextView minusTimeHorizontalBtn = timeHorizontalContainer.findViewById(R.id.btn_minus);
        TextView plusTimeHorizontalBtn = timeHorizontalContainer.findViewById(R.id.btn_plus);
        TextView minusTimeFontSizeBtn = timeFontSizeContainer.findViewById(R.id.btn_minus);
        TextView plusTimeFontSizeBtn = timeFontSizeContainer.findViewById(R.id.btn_plus);

        View timeColorContainer = findViewById(R.id.time_color_container);
        TextView timeColorValueTv = timeColorContainer.findViewById(R.id.value_text);
        timeColorValueTv.setText(getTimeColorText(timeColor));
        timeColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(timeColorValueTv,
                new String[] { "FOLLOW THEME", "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC LIGHT" }));

        TextView minusTimeColorBtn = timeColorContainer.findViewById(R.id.btn_minus);
        TextView plusTimeColorBtn = timeColorContainer.findViewById(R.id.btn_plus);

        View timeEffectContainer = findViewById(R.id.time_effect_container);
        TextView timeEffectValueTv = timeEffectContainer.findViewById(R.id.value_text);
        timeEffectValueTv.setText(getTimeEffectText(timeEffect));
        timeEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(timeEffectValueTv,
                new String[] { "NOTHING", "SHADOW", "OUTLINE" }));

        TextView minusTimeEffectBtn = timeEffectContainer.findViewById(R.id.btn_minus);
        TextView plusTimeEffectBtn = timeEffectContainer.findViewById(R.id.btn_plus);

        View timeEffectColorContainer = findViewById(R.id.time_effect_color_container);
        TextView timeEffectColorValueTv = timeEffectColorContainer.findViewById(R.id.value_text);
        timeEffectColorValueTv.setText(getTimeEffectColorText(timeEffectColor));
        timeEffectColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(timeEffectColorValueTv,
                new String[] { "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC WHITE" }));

        TextView minusTimeEffectColorBtn = timeEffectColorContainer.findViewById(R.id.btn_minus);
        TextView plusTimeEffectColorBtn = timeEffectColorContainer.findViewById(R.id.btn_plus);

        minusTimeBtn.setOnClickListener(v -> {
            timePosition = (timePosition - 1 + 2) % 2;
            timePositionValueTv.setText(getTimePositionText(timePosition));
            prefs.edit().putInt("time_position", timePosition).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusTimeBtn.setOnClickListener(v -> {
            timePosition = (timePosition + 1) % 2;
            timePositionValueTv.setText(getTimePositionText(timePosition));
            prefs.edit().putInt("time_position", timePosition).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusTimeHorizontalBtn.setOnClickListener(v -> {
            timeHorizontalPosition = (timeHorizontalPosition - 1 + 3) % 3;
            timeHorizontalValueTv.setText(getTimeHorizontalPositionText(timeHorizontalPosition));
            prefs.edit().putInt("time_horizontal_position", timeHorizontalPosition).apply();
        });

        plusTimeHorizontalBtn.setOnClickListener(v -> {
            timeHorizontalPosition = (timeHorizontalPosition + 1) % 3;
            timeHorizontalValueTv.setText(getTimeHorizontalPositionText(timeHorizontalPosition));
            prefs.edit().putInt("time_horizontal_position", timeHorizontalPosition).apply();
        });

        minusTimeFontSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (timeFontSize > 10) {
                timeFontSize--;
                timeFontSizeValueTv.setText(String.valueOf(timeFontSize));
                prefs.edit().putInt("time_font_size", timeFontSize).apply();
            }
        }));

        plusTimeFontSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (timeFontSize < 100) {
                timeFontSize++;
                timeFontSizeValueTv.setText(String.valueOf(timeFontSize));
                prefs.edit().putInt("time_font_size", timeFontSize).apply();
            }
        }));

        minusTimeColorBtn.setOnClickListener(v -> {
            timeColor = (timeColor - 1 + 5) % 5;
            timeColorValueTv.setText(getTimeColorText(timeColor));
            prefs.edit().putInt("time_color", timeColor).apply();
        });

        plusTimeColorBtn.setOnClickListener(v -> {
            timeColor = (timeColor + 1) % 5;
            timeColorValueTv.setText(getTimeColorText(timeColor));
            prefs.edit().putInt("time_color", timeColor).apply();
        });

        minusTimeEffectBtn.setOnClickListener(v -> {
            timeEffect = (timeEffect - 1 + 3) % 3;
            timeEffectValueTv.setText(getTimeEffectText(timeEffect));
            prefs.edit().putInt("time_effect", timeEffect).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusTimeEffectBtn.setOnClickListener(v -> {
            timeEffect = (timeEffect + 1) % 3;
            timeEffectValueTv.setText(getTimeEffectText(timeEffect));
            prefs.edit().putInt("time_effect", timeEffect).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusTimeEffectColorBtn.setOnClickListener(v -> {
            timeEffectColor = (timeEffectColor - 1 + 4) % 4;
            timeEffectColorValueTv.setText(getTimeEffectColorText(timeEffectColor));
            prefs.edit().putInt("time_effect_color", timeEffectColor).apply();
        });

        plusTimeEffectColorBtn.setOnClickListener(v -> {
            timeEffectColor = (timeEffectColor + 1) % 4;
            timeEffectColorValueTv.setText(getTimeEffectColorText(timeEffectColor));
            prefs.edit().putInt("time_effect_color", timeEffectColor).apply();
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout timeFontSizeLayout = findViewById(R.id.time_font_size_layout);
        LinearLayout timeHorizontalLayout = findViewById(R.id.time_horizontal_layout);
        LinearLayout timeColorLayout = findViewById(R.id.time_color_layout);
        LinearLayout timeEffectLayout = findViewById(R.id.time_effect_layout);
        LinearLayout timeEffectColorLayout = findViewById(R.id.time_effect_color_layout);

        if (timePosition == 0) {
            timeFontSizeLayout.setVisibility(View.GONE);
            timeHorizontalLayout.setVisibility(View.GONE);
            timeColorLayout.setVisibility(View.GONE);
            timeEffectLayout.setVisibility(View.GONE);
            timeEffectColorLayout.setVisibility(View.GONE);
        } else {
            timeFontSizeLayout.setVisibility(View.VISIBLE);
            timeHorizontalLayout.setVisibility(View.VISIBLE);
            timeColorLayout.setVisibility(View.VISIBLE);
            timeEffectLayout.setVisibility(View.VISIBLE);

            if (timeEffect == 0) {
                timeEffectColorLayout.setVisibility(View.GONE);
            } else {
                timeEffectColorLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getTimeColorText(int color) {
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

    private String getTimeEffectText(int effect) {
        switch (effect) {
            case 0:
                return "NOTHING";
            case 1:
                return "SHADOW";
            case 2:
                return "OUTLINE";
            default:
                return "NOTHING";
        }
    }

    private String getTimeEffectColorText(int color) {
        switch (color) {
            case 0:
                return "DARK";
            case 1:
                return "WHITE";
            case 2:
                return "DYNAMIC DARK";
            case 3:
                return "DYNAMIC WHITE";
            default:
                return "DARK";
        }
    }

    private String getTimePositionText(int pos) {
        return pos == 1 ? "ON" : "OFF";
    }

    private String getTimeHorizontalPositionText(int pos) {
        switch (pos) {
            case 0:
                return "LEFT";
            case 1:
                return "CENTER";
            case 2:
                return "RIGHT";
            default:
                return "LEFT";
        }
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
