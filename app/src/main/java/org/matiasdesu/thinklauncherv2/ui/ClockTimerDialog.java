package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ClockTimerHelper;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.RepeatListener;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class ClockTimerDialog extends GuardedDialog {

    public interface OnTimerSavedCallback {
        void onSaved(ClockTimerHelper.Timer timer);
    }
    public interface OnTimerDeletedCallback {
        void onDeleted(int timerId);
    }

    private final ClockTimerHelper.Timer editing;
    private final boolean isNew;
    private final OnTimerSavedCallback saveCallback;
    private final OnTimerDeletedCallback deleteCallback;

    private int hour;
    private int minute;
    private int second;
    private TextView hourValue;
    private TextView minuteValue;
    private TextView secondValue;
    private int theme;
    private int surfaceColor;

    public ClockTimerDialog(Context context, ClockTimerHelper.Timer timer, OnTimerSavedCallback saveCallback, OnTimerDeletedCallback deleteCallback) {
        super(context, R.style.NoAnimationDialog);
        this.editing = timer;
        this.isNew = timer == null;
        this.saveCallback = saveCallback;
        this.deleteCallback = deleteCallback;
        if (timer != null) {
            int total = timer.durationSec;
            this.hour = total / 3600;
            this.minute = (total % 3600) / 60;
            this.second = total % 60;
        } else {
            this.hour = 0;
            this.minute = 5;
            this.second = 0;
        }
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_clock_timer);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        hourValue = findViewById(R.id.hour_value);
        minuteValue = findViewById(R.id.minute_value);
        secondValue = findViewById(R.id.second_value);
        TextView hourLabel = findViewById(R.id.hour_label);
        TextView minuteLabel = findViewById(R.id.minute_label);
        TextView secondLabel = findViewById(R.id.second_label);
        TextView btnHourMinus = findViewById(R.id.btn_hour_minus);
        TextView btnHourPlus = findViewById(R.id.btn_hour_plus);
        TextView btnMinuteMinus = findViewById(R.id.btn_minute_minus);
        TextView btnMinutePlus = findViewById(R.id.btn_minute_plus);
        TextView btnSecondMinus = findViewById(R.id.btn_second_minus);
        TextView btnSecondPlus = findViewById(R.id.btn_second_plus);
        EditText labelEdit = findViewById(R.id.timer_label_edit);
        TextView btnCancel = findViewById(R.id.btn_cancel);
        TextView btnSave = findViewById(R.id.btn_save);
        TextView btnDelete = findViewById(R.id.btn_delete);

        ThemeUtils.applyTextColor(hourLabel, theme, getContext());
        ThemeUtils.applyTextColor(minuteLabel, theme, getContext());
        ThemeUtils.applyTextColor(secondLabel, theme, getContext());
        TextView presetsLabel = findViewById(R.id.presets_label);
        ThemeUtils.applyTextColor(presetsLabel, theme, getContext());
        ThemeUtils.applyTextColor(hourValue, theme, getContext());
        ThemeUtils.applyTextColor(minuteValue, theme, getContext());
        ThemeUtils.applyTextColor(secondValue, theme, getContext());
        DialogEffectHelper.applyButtonTheme(btnHourMinus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnHourPlus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnMinuteMinus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnMinutePlus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnSecondMinus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnSecondPlus, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyEditTextTheme(labelEdit, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnCancel, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnSave, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnDelete, theme, getContext(), surfaceColor);

        TextView preset1m = findViewById(R.id.preset_1m);
        TextView preset5m = findViewById(R.id.preset_5m);
        TextView preset10m = findViewById(R.id.preset_10m);
        TextView preset15m = findViewById(R.id.preset_15m);
        TextView presetPomoWork = findViewById(R.id.preset_pomo_work);
        TextView presetPomoBreak = findViewById(R.id.preset_pomo_break);
        if (preset1m != null) DialogEffectHelper.applyButtonTheme(preset1m, theme, getContext(), surfaceColor);
        if (preset5m != null) DialogEffectHelper.applyButtonTheme(preset5m, theme, getContext(), surfaceColor);
        if (preset10m != null) DialogEffectHelper.applyButtonTheme(preset10m, theme, getContext(), surfaceColor);
        if (preset15m != null) DialogEffectHelper.applyButtonTheme(preset15m, theme, getContext(), surfaceColor);
        if (presetPomoWork != null) DialogEffectHelper.applyButtonTheme(presetPomoWork, theme, getContext(), surfaceColor);
        if (presetPomoBreak != null) DialogEffectHelper.applyButtonTheme(presetPomoBreak, theme, getContext(), surfaceColor);
        if (preset1m != null) preset1m.setOnClickListener(v -> applyPreset(0, 1, 0, null));
        if (preset5m != null) preset5m.setOnClickListener(v -> applyPreset(0, 5, 0, null));
        if (preset10m != null) preset10m.setOnClickListener(v -> applyPreset(0, 10, 0, null));
        if (preset15m != null) preset15m.setOnClickListener(v -> applyPreset(0, 15, 0, null));
        if (presetPomoWork != null) presetPomoWork.setOnClickListener(v -> applyPreset(0, 25, 0, "Pomodoro"));
        if (presetPomoBreak != null) presetPomoBreak.setOnClickListener(v -> applyPreset(0, 5, 0, "Break"));

        updateTimeTexts();

        btnHourMinus.setOnTouchListener(new RepeatListener(v -> { hour = (hour - 1 + 99) % 99; if (hour < 0) hour = 98; updateTimeTexts(); }));
        btnHourPlus.setOnTouchListener(new RepeatListener(v -> { hour = (hour + 1) % 99; updateTimeTexts(); }));
        btnMinuteMinus.setOnTouchListener(new RepeatListener(v -> { minute = (minute - 1 + 60) % 60; updateTimeTexts(); }));
        btnMinutePlus.setOnTouchListener(new RepeatListener(v -> { minute = (minute + 1) % 60; updateTimeTexts(); }));
        btnSecondMinus.setOnTouchListener(new RepeatListener(v -> { second = (second - 1 + 60) % 60; updateTimeTexts(); }));
        btnSecondPlus.setOnTouchListener(new RepeatListener(v -> { second = (second + 1) % 60; updateTimeTexts(); }));

        if (editing != null && editing.label != null) labelEdit.setText(editing.label);

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
            int total = hour * 3600 + minute * 60 + second;
            if (total <= 0) {
                Toast.makeText(getContext(), "Duration must be > 0", Toast.LENGTH_SHORT).show();
                return;
            }
            String label = labelEdit.getText().toString().trim();
            ClockTimerHelper.Timer out;
            if (isNew) {
                int newId = ClockTimerHelper.nextId(getContext());
                out = new ClockTimerHelper.Timer(newId, total, label);
            } else {
                out = new ClockTimerHelper.Timer(editing.id, total, label);
            }
            dismiss();
            if (saveCallback != null) saveCallback.onSaved(out);
        });
    }

    private void applyPreset(int h, int m, int s, String label) {
        this.hour = h % 99;
        this.minute = m % 60;
        this.second = s % 60;
        updateTimeTexts();
        if (label != null) {
            EditText labelEdit = findViewById(R.id.timer_label_edit);
            if (labelEdit != null) labelEdit.setText(label);
        }
    }

    private void updateTimeTexts() {
        if (hourValue != null) hourValue.setText(String.format("%02d", hour));
        if (minuteValue != null) minuteValue.setText(String.format("%02d", minute));
        if (secondValue != null) secondValue.setText(String.format("%02d", second));
    }
}
