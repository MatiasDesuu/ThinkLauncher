package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.RepeatListener;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class ClockAlarmDialog extends Dialog {

    public interface OnAlarmSavedCallback {
        void onSaved(ClockAlarmHelper.Alarm alarm);
    }
    public interface OnAlarmDeletedCallback {
        void onDeleted(int alarmId);
    }

    private final ClockAlarmHelper.Alarm editing;
    private final boolean isNew;
    private final OnAlarmSavedCallback saveCallback;
    private final OnAlarmDeletedCallback deleteCallback;

    private int hour;
    private int minute;
    private final boolean[] days = new boolean[7];
    private TextView hourValue;
    private TextView minuteValue;
    private int theme;
    private int surfaceColor;

    public ClockAlarmDialog(Context context, ClockAlarmHelper.Alarm alarm, OnAlarmSavedCallback saveCallback, OnAlarmDeletedCallback deleteCallback) {
        super(context, R.style.NoAnimationDialog);
        this.editing = alarm;
        this.isNew = alarm == null;
        this.saveCallback = saveCallback;
        this.deleteCallback = deleteCallback;
        if (alarm != null) {
            this.hour = alarm.hour;
            this.minute = alarm.minute;
            System.arraycopy(alarm.days, 0, this.days, 0, 7);
        } else {

            this.hour = 7;
            this.minute = 0;
        }
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_clock_alarm);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        hourValue = findViewById(R.id.hour_value);
        minuteValue = findViewById(R.id.minute_value);
        TextView hourLabel = findViewById(R.id.hour_label);
        TextView minuteLabel = findViewById(R.id.minute_label);
        TextView btnHourMinus = findViewById(R.id.btn_hour_minus);
        TextView btnHourPlus = findViewById(R.id.btn_hour_plus);
        TextView btnMinuteMinus = findViewById(R.id.btn_minute_minus);
        TextView btnMinutePlus = findViewById(R.id.btn_minute_plus);
        EditText labelEdit = findViewById(R.id.alarm_label_edit);
        TextView btnCancel = findViewById(R.id.btn_cancel);
        TextView btnSave = findViewById(R.id.btn_save);
        TextView btnDelete = findViewById(R.id.btn_delete);

        ThemeUtils.applyTextColor(hourLabel, theme, getContext());
        ThemeUtils.applyTextColor(minuteLabel, theme, getContext());
        ThemeUtils.applyTextColor(hourValue, theme, getContext());
        ThemeUtils.applyTextColor(minuteValue, theme, getContext());
        DialogEffectHelper.applyButtonTheme(btnHourMinus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnHourPlus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnMinuteMinus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnMinutePlus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyEditTextTheme(labelEdit, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnCancel, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnSave, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnDelete, theme, getContext(), surfaceColor);

        updateTimeTexts();

        btnHourMinus.setOnTouchListener(new RepeatListener(v -> { hour = (hour - 1 + 24) % 24; updateTimeTexts(); }));
        btnHourPlus.setOnTouchListener(new RepeatListener(v -> { hour = (hour + 1) % 24; updateTimeTexts(); }));
        btnMinuteMinus.setOnTouchListener(new RepeatListener(v -> { minute = (minute - 1 + 60) % 60; updateTimeTexts(); }));
        btnMinutePlus.setOnTouchListener(new RepeatListener(v -> { minute = (minute + 1) % 60; updateTimeTexts(); }));

        if (editing != null && editing.label != null) labelEdit.setText(editing.label);

        int[] dayIds = {R.id.day_0,R.id.day_1,R.id.day_2,R.id.day_3,R.id.day_4,R.id.day_5,R.id.day_6};
        for (int i = 0; i < 7; i++) {
            TextView tv = findViewById(dayIds[i]);
            DialogEffectHelper.applyButtonTheme(tv, theme, getContext(), surfaceColor);
            final int idx = i;
            updateDayButton(tv, days[idx]);
            tv.setOnClickListener(v -> {
                days[idx] = !days[idx];
                updateDayButton(tv, days[idx]);
            });
        }

        if (isNew) {
            btnDelete.setVisibility(View.GONE);
        } else {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                dismiss();
                if (deleteCallback != null) deleteCallback.onDeleted(editing.id);
            });
        }

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> {
            String label = labelEdit.getText().toString().trim();
            ClockAlarmHelper.Alarm out;
            if (isNew) {
                int newId = ClockAlarmHelper.nextId(getContext());
                out = new ClockAlarmHelper.Alarm(newId, hour, minute, days.clone(), label, true);
            } else {
                out = new ClockAlarmHelper.Alarm(editing.id, hour, minute, days.clone(), label, editing.enabled);
            }
            dismiss();
            if (saveCallback != null) saveCallback.onSaved(out);
        });
    }

    private void updateTimeTexts() {
        if (hourValue != null) hourValue.setText(String.format("%02d", hour));
        if (minuteValue != null) minuteValue.setText(String.format("%02d", minute));
    }

    private void updateDayButton(TextView tv, boolean selected) {
        tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);

        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        int textColor = org.matiasdesu.thinklauncherv2.utils.ThemeUtils.getTextColor(theme, getContext());
        int bgColor = org.matiasdesu.thinklauncherv2.utils.ThemeUtils.getBgColor(theme, getContext());
        drawable.setColor(selected ? textColor : surfaceColor);
        drawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), textColor);
        DialogEffectHelper.applyCornerRadius(drawable, getContext());
        int padding = (int) (4 * getContext().getResources().getDisplayMetrics().density);
        tv.setBackground(drawable);
        tv.setPadding(padding, padding, padding, padding);
        tv.setTextColor(selected ? bgColor : textColor);

        tv.setAlpha(1.0f);
    }
}