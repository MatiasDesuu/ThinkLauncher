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

public class DateSettingsActivity extends AppCompatActivity {

    private int timePosition;
    private int datePosition;
    private int dateFontSize;
    private int dateHorizontalPosition;
    private int dateVerticalPosition;
    private int fullMonthName;
    private int dateColor;
    private int dateEffect;
    private int dateEffectColor;
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
                    Intent mainIntent = new Intent(DateSettingsActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_date_settings);

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
        datePosition = prefs.getInt("date_position", 0);
        dateFontSize = prefs.getInt("date_font_size", 22);
        dateHorizontalPosition = prefs.getInt("date_horizontal_position", 0);
        dateVerticalPosition = prefs.getInt("date_vertical_position", 0);
        fullMonthName = prefs.getInt("full_month_name", 0);
        dateColor = prefs.getInt("date_color", 0);
        dateEffect = prefs.getInt("date_effect", 0);
        dateEffectColor = prefs.getInt("date_effect_color", 0);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View dateContainer = findViewById(R.id.date_container);
        TextView dateValueTv = dateContainer.findViewById(R.id.value_text);
        dateValueTv.setText(getOnOffText(datePosition));
        dateValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateValueTv, new String[] { "OFF", "ON" }));

        View dateFontSizeContainer = findViewById(R.id.date_font_size_container);
        TextView dateFontSizeValueTv = dateFontSizeContainer.findViewById(R.id.value_text);
        dateFontSizeValueTv.setText(String.valueOf(dateFontSize));

        View dateHorizontalContainer = findViewById(R.id.date_horizontal_container);
        TextView dateHorizontalValueTv = dateHorizontalContainer.findViewById(R.id.value_text);
        dateHorizontalValueTv.setText(getHorizontalPositionText(dateHorizontalPosition));
        dateHorizontalValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(dateHorizontalValueTv, new String[] { "LEFT", "CENTER", "RIGHT" }));

        View dateVerticalContainer = findViewById(R.id.date_vertical_container);
        TextView dateVerticalValueTv = dateVerticalContainer.findViewById(R.id.value_text);
        dateVerticalValueTv.setText(getVerticalPositionText(dateVerticalPosition));
        dateVerticalValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateVerticalValueTv, new String[] { "TOP", "BOTTOM" }));

        View fullMonthNameContainer = findViewById(R.id.full_month_name_container);
        TextView fullMonthNameValueTv = fullMonthNameContainer.findViewById(R.id.value_text);
        fullMonthNameValueTv.setText(getOnOffText(fullMonthName));
        fullMonthNameValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(fullMonthNameValueTv, new String[] { "OFF", "ON" }));

        View dateColorContainer = findViewById(R.id.date_color_container);
        TextView dateColorValueTv = dateColorContainer.findViewById(R.id.value_text);
        dateColorValueTv.setText(getDateColorText(dateColor));
        dateColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateColorValueTv,
                new String[] { "FOLLOW THEME", "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC LIGHT" }));

        TextView minusDateBtn = dateContainer.findViewById(R.id.btn_minus);
        TextView plusDateBtn = dateContainer.findViewById(R.id.btn_plus);
        TextView minusDateFontSizeBtn = dateFontSizeContainer.findViewById(R.id.btn_minus);
        TextView plusDateFontSizeBtn = dateFontSizeContainer.findViewById(R.id.btn_plus);
        TextView minusDateHorizontalBtn = dateHorizontalContainer.findViewById(R.id.btn_minus);
        TextView plusDateHorizontalBtn = dateHorizontalContainer.findViewById(R.id.btn_plus);
        TextView minusDateVerticalBtn = dateVerticalContainer.findViewById(R.id.btn_minus);
        TextView plusDateVerticalBtn = dateVerticalContainer.findViewById(R.id.btn_plus);
        TextView minusFullMonthBtn = fullMonthNameContainer.findViewById(R.id.btn_minus);
        TextView plusFullMonthBtn = fullMonthNameContainer.findViewById(R.id.btn_plus);
        TextView minusDateColorBtn = dateColorContainer.findViewById(R.id.btn_minus);
        TextView plusDateColorBtn = dateColorContainer.findViewById(R.id.btn_plus);

        View dateEffectContainer = findViewById(R.id.date_effect_container);
        TextView dateEffectValueTv = dateEffectContainer.findViewById(R.id.value_text);
        dateEffectValueTv.setText(getDateEffectText(dateEffect));
        dateEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateEffectValueTv,
                new String[] { "NOTHING", "SHADOW", "OUTLINE" }));

        TextView minusDateEffectBtn = dateEffectContainer.findViewById(R.id.btn_minus);
        TextView plusDateEffectBtn = dateEffectContainer.findViewById(R.id.btn_plus);

        View dateEffectColorContainer = findViewById(R.id.date_effect_color_container);
        TextView dateEffectColorValueTv = dateEffectColorContainer.findViewById(R.id.value_text);
        dateEffectColorValueTv.setText(getDateEffectColorText(dateEffectColor));
        dateEffectColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateEffectColorValueTv,
                new String[] { "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC WHITE" }));

        TextView minusDateEffectColorBtn = dateEffectColorContainer.findViewById(R.id.btn_minus);
        TextView plusDateEffectColorBtn = dateEffectColorContainer.findViewById(R.id.btn_plus);

        minusDateBtn.setOnClickListener(v -> {
            datePosition = (datePosition - 1 + 2) % 2;
            dateValueTv.setText(getOnOffText(datePosition));
            prefs.edit().putInt("date_position", datePosition).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusDateBtn.setOnClickListener(v -> {
            datePosition = (datePosition + 1) % 2;
            dateValueTv.setText(getOnOffText(datePosition));
            prefs.edit().putInt("date_position", datePosition).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusDateFontSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (dateFontSize > 10) {
                dateFontSize--;
                dateFontSizeValueTv.setText(String.valueOf(dateFontSize));
                prefs.edit().putInt("date_font_size", dateFontSize).apply();
            }
        }));

        plusDateFontSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (dateFontSize < 100) {
                dateFontSize++;
                dateFontSizeValueTv.setText(String.valueOf(dateFontSize));
                prefs.edit().putInt("date_font_size", dateFontSize).apply();
            }
        }));

        minusDateHorizontalBtn.setOnClickListener(v -> {
            dateHorizontalPosition = (dateHorizontalPosition - 1 + 3) % 3;
            dateHorizontalValueTv.setText(getHorizontalPositionText(dateHorizontalPosition));
            prefs.edit().putInt("date_horizontal_position", dateHorizontalPosition).apply();
        });

        plusDateHorizontalBtn.setOnClickListener(v -> {
            dateHorizontalPosition = (dateHorizontalPosition + 1) % 3;
            dateHorizontalValueTv.setText(getHorizontalPositionText(dateHorizontalPosition));
            prefs.edit().putInt("date_horizontal_position", dateHorizontalPosition).apply();
        });

        minusDateVerticalBtn.setOnClickListener(v -> {
            dateVerticalPosition = (dateVerticalPosition - 1 + 2) % 2;
            dateVerticalValueTv.setText(getVerticalPositionText(dateVerticalPosition));
            prefs.edit().putInt("date_vertical_position", dateVerticalPosition).apply();
        });

        plusDateVerticalBtn.setOnClickListener(v -> {
            dateVerticalPosition = (dateVerticalPosition + 1) % 2;
            dateVerticalValueTv.setText(getVerticalPositionText(dateVerticalPosition));
            prefs.edit().putInt("date_vertical_position", dateVerticalPosition).apply();
        });

        minusFullMonthBtn.setOnClickListener(v -> {
            fullMonthName = (fullMonthName - 1 + 2) % 2;
            fullMonthNameValueTv.setText(getOnOffText(fullMonthName));
            prefs.edit().putInt("full_month_name", fullMonthName).apply();
        });

        plusFullMonthBtn.setOnClickListener(v -> {
            fullMonthName = (fullMonthName + 1) % 2;
            fullMonthNameValueTv.setText(getOnOffText(fullMonthName));
            prefs.edit().putInt("full_month_name", fullMonthName).apply();
        });

        minusDateColorBtn.setOnClickListener(v -> {
            dateColor = (dateColor - 1 + 5) % 5;
            dateColorValueTv.setText(getDateColorText(dateColor));
            prefs.edit().putInt("date_color", dateColor).apply();
        });

        plusDateColorBtn.setOnClickListener(v -> {
            dateColor = (dateColor + 1) % 5;
            dateColorValueTv.setText(getDateColorText(dateColor));
            prefs.edit().putInt("date_color", dateColor).apply();
        });

        minusDateEffectBtn.setOnClickListener(v -> {
            dateEffect = (dateEffect - 1 + 3) % 3;
            dateEffectValueTv.setText(getDateEffectText(dateEffect));
            prefs.edit().putInt("date_effect", dateEffect).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        plusDateEffectBtn.setOnClickListener(v -> {
            dateEffect = (dateEffect + 1) % 3;
            dateEffectValueTv.setText(getDateEffectText(dateEffect));
            prefs.edit().putInt("date_effect", dateEffect).apply();
            refreshVisibility();
            if (paginationHelper != null) {
                paginationHelper.updateVisibleItemsList();
            }
        });

        minusDateEffectColorBtn.setOnClickListener(v -> {
            dateEffectColor = (dateEffectColor - 1 + 4) % 4;
            dateEffectColorValueTv.setText(getDateEffectColorText(dateEffectColor));
            prefs.edit().putInt("date_effect_color", dateEffectColor).apply();
        });

        plusDateEffectColorBtn.setOnClickListener(v -> {
            dateEffectColor = (dateEffectColor + 1) % 4;
            dateEffectColorValueTv.setText(getDateEffectColorText(dateEffectColor));
            prefs.edit().putInt("date_effect_color", dateEffectColor).apply();
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout fontSizeLayout = findViewById(R.id.date_font_size_layout);
        LinearLayout horizontalLayout = findViewById(R.id.date_horizontal_layout);
        LinearLayout verticalLayout = findViewById(R.id.date_vertical_layout);
        LinearLayout fullMonthLayout = findViewById(R.id.full_month_name_layout);
        LinearLayout dateColorLayout = findViewById(R.id.date_color_layout);
        LinearLayout dateEffectLayout = findViewById(R.id.date_effect_layout);
        LinearLayout dateEffectColorLayout = findViewById(R.id.date_effect_color_layout);

        if (datePosition == 0) {
            fontSizeLayout.setVisibility(View.GONE);
            horizontalLayout.setVisibility(View.GONE);
            verticalLayout.setVisibility(View.GONE);
            fullMonthLayout.setVisibility(View.GONE);
            dateColorLayout.setVisibility(View.GONE);
            dateEffectLayout.setVisibility(View.GONE);
            dateEffectColorLayout.setVisibility(View.GONE);
        } else {
            fontSizeLayout.setVisibility(View.VISIBLE);
            horizontalLayout.setVisibility(View.VISIBLE);
            fullMonthLayout.setVisibility(View.VISIBLE);
            dateColorLayout.setVisibility(View.VISIBLE);
            dateEffectLayout.setVisibility(View.VISIBLE);

            if (dateEffect == 0) {
                dateEffectColorLayout.setVisibility(View.GONE);
            } else {
                dateEffectColorLayout.setVisibility(View.VISIBLE);
            }

            // Vertical position only matters if Time is also ON
            if (timePosition == 1) {
                verticalLayout.setVisibility(View.VISIBLE);
            } else {
                verticalLayout.setVisibility(View.GONE);
            }
        }
    }

    private String getOnOffText(int pos) {
        return pos == 1 ? "ON" : "OFF";
    }

    private String getHorizontalPositionText(int pos) {
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

    private String getVerticalPositionText(int pos) {
        return pos == 1 ? "BOTTOM" : "TOP";
    }

    private String getDateColorText(int color) {
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

    private String getDateEffectText(int effect) {
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

    private String getDateEffectColorText(int color) {
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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        // Refresh timePosition in case it was changed in TimeSettingsActivity
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        timePosition = prefs.getInt("time_position", 0);
        refreshVisibility();

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
