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
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class DateSettingsActivity extends BaseSettingsActivity {

    private int timePosition;
    private int datePosition;
    private int dateFontSize;
    private int dateHorizontalPosition;
    private int dateVerticalPosition;
    private int dateFormat;
    private int dateColor;
    private int dateEffect;
    private int dateEffectColor;
    private int dateCalendarEvents;
    private int calendarEventFontSize;
    private int batteryInfo;
    private int batteryPosition;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {

                    Intent mainIntent = new Intent(DateSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_date_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        timePosition = prefs.getInt("time_position", 0);
        datePosition = prefs.getInt("date_position", 0);
        dateFontSize = prefs.getInt("date_font_size", 22);
        dateHorizontalPosition = prefs.getInt("date_horizontal_position", 0);
        dateVerticalPosition = prefs.getInt("date_vertical_position", 0);
        dateFormat = prefs.contains("date_format") ? prefs.getInt("date_format", 0)
                : (prefs.getInt("full_month_name", 0) == 1 ? 1 : 0);
        dateColor = prefs.getInt("date_color", 0);
        dateEffect = prefs.getInt("date_effect", 0);
        dateEffectColor = prefs.getInt("date_effect_color", 0);
        dateCalendarEvents = prefs.getInt("date_calendar_events", 0);
        calendarEventFontSize = prefs.getInt("calendar_event_font_size", 16);
        batteryInfo = prefs.getInt("battery_info", 0);
        batteryPosition = prefs.getInt("battery_position", 1); // Default to Right

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

        View dateFormatContainer = findViewById(R.id.date_format_container);
        TextView dateFormatValueTv = dateFormatContainer.findViewById(R.id.value_text);
        dateFormatValueTv.setText(getDateFormatText(dateFormat));
        dateFormatValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateFormatValueTv,
                new String[] { "DD MMM YYYY", "DD MMMM YYYY", "MMM DD, YYYY", "MMMM DD, YYYY", "YYYY-MM-DD" }));

        View dateColorContainer = findViewById(R.id.date_color_container);
        TextView dateColorValueTv = dateColorContainer.findViewById(R.id.value_text);
        dateColorValueTv.setText(getDateColorText(dateColor));
        dateColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateColorValueTv,
                new String[] { "FOLLOW THEME", "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC LIGHT" }));

        ImageButton minusDateBtn = dateContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateBtn = dateContainer.findViewById(R.id.btn_plus);
        ImageButton minusDateFontSizeBtn = dateFontSizeContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateFontSizeBtn = dateFontSizeContainer.findViewById(R.id.btn_plus);
        ImageButton minusDateHorizontalBtn = dateHorizontalContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateHorizontalBtn = dateHorizontalContainer.findViewById(R.id.btn_plus);
        ImageButton minusDateVerticalBtn = dateVerticalContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateVerticalBtn = dateVerticalContainer.findViewById(R.id.btn_plus);
        ImageButton minusDateFormatBtn = dateFormatContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateFormatBtn = dateFormatContainer.findViewById(R.id.btn_plus);
        ImageButton minusDateColorBtn = dateColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateColorBtn = dateColorContainer.findViewById(R.id.btn_plus);

        View dateEffectContainer = findViewById(R.id.date_effect_container);
        TextView dateEffectValueTv = dateEffectContainer.findViewById(R.id.value_text);
        dateEffectValueTv.setText(getDateEffectText(dateEffect));
        dateEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateEffectValueTv,
                new String[] { "NOTHING", "SHADOW", "OUTLINE" }));

        ImageButton minusDateEffectBtn = dateEffectContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateEffectBtn = dateEffectContainer.findViewById(R.id.btn_plus);

        View dateEffectColorContainer = findViewById(R.id.date_effect_color_container);
        TextView dateEffectColorValueTv = dateEffectColorContainer.findViewById(R.id.value_text);
        dateEffectColorValueTv.setText(getDateEffectColorText(dateEffectColor));
        dateEffectColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(dateEffectColorValueTv,
                new String[] { "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC WHITE" }));

        ImageButton minusDateEffectColorBtn = dateEffectColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateEffectColorBtn = dateEffectColorContainer.findViewById(R.id.btn_plus);

        View dateCalendarEventsContainer = findViewById(R.id.date_calendar_events_container);
        TextView dateCalendarEventsValueTv = dateCalendarEventsContainer.findViewById(R.id.value_text);
        dateCalendarEventsValueTv.setText(getOnOffText(dateCalendarEvents));
        dateCalendarEventsValueTv.setMinWidth(
            TextWidthHelper.getMaxTextWidthPx(dateCalendarEventsValueTv, new String[] { "OFF", "ON" }));

        ImageButton minusDateCalendarEventsBtn = dateCalendarEventsContainer.findViewById(R.id.btn_minus);
        ImageButton plusDateCalendarEventsBtn = dateCalendarEventsContainer.findViewById(R.id.btn_plus);

        View calendarEventFontSizeContainer = findViewById(R.id.calendar_event_font_size_container);
        TextView calendarEventFontSizeValueTv = calendarEventFontSizeContainer.findViewById(R.id.value_text);
        calendarEventFontSizeValueTv.setText(String.valueOf(calendarEventFontSize));

        ImageButton minusCalendarEventFontSizeBtn = calendarEventFontSizeContainer.findViewById(R.id.btn_minus);
        ImageButton plusCalendarEventFontSizeBtn = calendarEventFontSizeContainer.findViewById(R.id.btn_plus);

        View batteryInfoContainer = findViewById(R.id.battery_info_container);
        TextView batteryInfoValueTv = batteryInfoContainer.findViewById(R.id.value_text);
        batteryInfoValueTv.setText(getOnOffText(batteryInfo));
        batteryInfoValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(batteryInfoValueTv, new String[]{"OFF", "ON"}));

        ImageButton minusBatteryInfoBtn = batteryInfoContainer.findViewById(R.id.btn_minus);
        ImageButton plusBatteryInfoBtn = batteryInfoContainer.findViewById(R.id.btn_plus);

        View batteryPositionContainer = findViewById(R.id.battery_position_container);
        TextView batteryPositionValueTv = batteryPositionContainer.findViewById(R.id.value_text);
        batteryPositionValueTv.setText(getLeftRightText(batteryPosition));
        batteryPositionValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(batteryPositionValueTv, new String[]{"LEFT", "RIGHT"}));

        ImageButton minusBatteryPositionBtn = batteryPositionContainer.findViewById(R.id.btn_minus);
        ImageButton plusBatteryPositionBtn = batteryPositionContainer.findViewById(R.id.btn_plus);

        minusDateBtn.setOnClickListener(v -> {
            datePosition = (datePosition - 1 + 2) % 2;
            dateValueTv.setText(getOnOffText(datePosition));
            prefs.edit().putInt("date_position", datePosition).apply();
            refreshVisibility();
            refreshPagination();
        });

        plusDateBtn.setOnClickListener(v -> {
            datePosition = (datePosition + 1) % 2;
            dateValueTv.setText(getOnOffText(datePosition));
            prefs.edit().putInt("date_position", datePosition).apply();
            refreshVisibility();
            refreshPagination();
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

        minusDateFormatBtn.setOnClickListener(v -> {
            dateFormat = (dateFormat - 1 + 5) % 5;
            dateFormatValueTv.setText(getDateFormatText(dateFormat));
            prefs.edit().putInt("date_format", dateFormat).apply();
        });

        plusDateFormatBtn.setOnClickListener(v -> {
            dateFormat = (dateFormat + 1) % 5;
            dateFormatValueTv.setText(getDateFormatText(dateFormat));
            prefs.edit().putInt("date_format", dateFormat).apply();
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
            refreshPagination();
        });

        plusDateEffectBtn.setOnClickListener(v -> {
            dateEffect = (dateEffect + 1) % 3;
            dateEffectValueTv.setText(getDateEffectText(dateEffect));
            prefs.edit().putInt("date_effect", dateEffect).apply();
            refreshVisibility();
            refreshPagination();
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

        minusDateCalendarEventsBtn.setOnClickListener(v -> {
            dateCalendarEvents = (dateCalendarEvents - 1 + 2) % 2;
            dateCalendarEventsValueTv.setText(getOnOffText(dateCalendarEvents));
            prefs.edit().putInt("date_calendar_events", dateCalendarEvents).apply();
            refreshVisibility();
            refreshPagination();
        });

        plusDateCalendarEventsBtn.setOnClickListener(v -> {
            dateCalendarEvents = (dateCalendarEvents + 1) % 2;
            dateCalendarEventsValueTv.setText(getOnOffText(dateCalendarEvents));
            prefs.edit().putInt("date_calendar_events", dateCalendarEvents).apply();
            refreshVisibility();
            refreshPagination();
        });

        minusCalendarEventFontSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (calendarEventFontSize > 10) {
                calendarEventFontSize--;
                calendarEventFontSizeValueTv.setText(String.valueOf(calendarEventFontSize));
                prefs.edit().putInt("calendar_event_font_size", calendarEventFontSize).apply();
            }
        }));

        plusCalendarEventFontSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (calendarEventFontSize < 100) {
                calendarEventFontSize++;
                calendarEventFontSizeValueTv.setText(String.valueOf(calendarEventFontSize));
                prefs.edit().putInt("calendar_event_font_size", calendarEventFontSize).apply();
            }
        }));

        minusBatteryInfoBtn.setOnClickListener(v -> {
            batteryInfo = (batteryInfo - 1 + 2) % 2;
            batteryInfoValueTv.setText(getOnOffText(batteryInfo));
            prefs.edit().putInt("battery_info", batteryInfo).apply();
            refreshVisibility();
            refreshPagination();
        });

        plusBatteryInfoBtn.setOnClickListener(v -> {
            batteryInfo = (batteryInfo + 1) % 2;
            batteryInfoValueTv.setText(getOnOffText(batteryInfo));
            prefs.edit().putInt("battery_info", batteryInfo).apply();
            refreshVisibility();
            refreshPagination();
        });

        minusBatteryPositionBtn.setOnClickListener(v -> {
            batteryPosition = (batteryPosition - 1 + 2) % 2;
            batteryPositionValueTv.setText(getLeftRightText(batteryPosition));
            prefs.edit().putInt("battery_position", batteryPosition).apply();
        });

        plusBatteryPositionBtn.setOnClickListener(v -> {
            batteryPosition = (batteryPosition + 1) % 2;
            batteryPositionValueTv.setText(getLeftRightText(batteryPosition));
            prefs.edit().putInt("battery_position", batteryPosition).apply();
        });

        initPagination(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout fontSizeLayout = findViewById(R.id.date_font_size_layout);
        LinearLayout horizontalLayout = findViewById(R.id.date_horizontal_layout);
        LinearLayout verticalLayout = findViewById(R.id.date_vertical_layout);
        LinearLayout dateFormatLayout = findViewById(R.id.date_format_layout);
        LinearLayout dateColorLayout = findViewById(R.id.date_color_layout);
        LinearLayout dateEffectLayout = findViewById(R.id.date_effect_layout);
        LinearLayout dateEffectColorLayout = findViewById(R.id.date_effect_color_layout);
        LinearLayout dateCalendarEventsLayout = findViewById(R.id.date_calendar_events_layout);
        LinearLayout calendarEventFontSizeLayout = findViewById(R.id.calendar_event_font_size_layout);
        LinearLayout batteryInfoLayout = findViewById(R.id.battery_info_layout);
        LinearLayout batteryPositionLayout = findViewById(R.id.battery_position_layout);

        if (datePosition == 0) {
            fontSizeLayout.setVisibility(View.GONE);
            horizontalLayout.setVisibility(View.GONE);
            verticalLayout.setVisibility(View.GONE);
            dateFormatLayout.setVisibility(View.GONE);
            dateColorLayout.setVisibility(View.GONE);
            dateEffectLayout.setVisibility(View.GONE);
            dateEffectColorLayout.setVisibility(View.GONE);
            dateCalendarEventsLayout.setVisibility(View.GONE);
            calendarEventFontSizeLayout.setVisibility(View.GONE);
            batteryInfoLayout.setVisibility(View.GONE);
            batteryPositionLayout.setVisibility(View.GONE);
        } else {
            fontSizeLayout.setVisibility(View.VISIBLE);
            horizontalLayout.setVisibility(View.VISIBLE);
            dateFormatLayout.setVisibility(View.VISIBLE);
            dateColorLayout.setVisibility(View.VISIBLE);
            dateEffectLayout.setVisibility(View.VISIBLE);
            dateCalendarEventsLayout.setVisibility(View.VISIBLE);
            calendarEventFontSizeLayout.setVisibility(dateCalendarEvents == 1 ? View.VISIBLE : View.GONE);
            batteryInfoLayout.setVisibility(View.VISIBLE);

            if (batteryInfo == 1) {
                batteryPositionLayout.setVisibility(View.VISIBLE);
            } else {
                batteryPositionLayout.setVisibility(View.GONE);
            }

            if (dateEffect == 0) {
                dateEffectColorLayout.setVisibility(View.GONE);
            } else {
                dateEffectColorLayout.setVisibility(View.VISIBLE);
            }

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

    private String getLeftRightText(int pos) {
        return pos == 1 ? "RIGHT" : "LEFT";
    }

    private String getDateFormatText(int format) {
        switch (format) {
            case 0:
                return "DD MMM YYYY";
            case 1:
                return "DD MMMM YYYY";
            case 2:
                return "MMM DD, YYYY";
            case 3:
                return "MMMM DD, YYYY";
            case 4:
                return "YYYY-MM-DD";
            default:
                return "DD MMM YYYY";
        }
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
        timePosition = prefs.getInt("time_position", 0);
        refreshVisibility();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }
}
