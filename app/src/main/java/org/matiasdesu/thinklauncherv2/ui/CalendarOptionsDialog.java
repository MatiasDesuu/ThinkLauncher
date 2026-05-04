package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class CalendarOptionsDialog extends Dialog {

    public interface OnOptionsChangedCallback {
        void onOptionsChanged();
    }

    private boolean showAccount;
    private int eventLimit;
    private boolean highlightToday;
    private boolean showMonthSeparators;
    private boolean highlightEventTimes;
    private OnOptionsChangedCallback callback;

    public CalendarOptionsDialog(Context context, boolean showAccount, int eventLimit, boolean highlightToday,
            boolean showMonthSeparators, boolean highlightEventTimes, OnOptionsChangedCallback callback) {
        super(context, R.style.NoAnimationDialog);
        this.showAccount = showAccount;
        this.eventLimit = eventLimit;
        this.highlightToday = highlightToday;
        this.showMonthSeparators = showMonthSeparators;
        this.highlightEventTimes = highlightEventTimes;
        this.callback = callback;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_calendar_options);
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView accountButton = findViewById(R.id.account_button);
        TextView eventLimitButton = findViewById(R.id.event_limit_button);
        TextView todayHighlightButton = findViewById(R.id.today_highlight_button);
        TextView monthSeparatorsButton = findViewById(R.id.month_separators_button);
        TextView timeHighlightButton = findViewById(R.id.time_highlight_button);

        DialogEffectHelper.applyButtonTheme(accountButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(eventLimitButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(todayHighlightButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(monthSeparatorsButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(timeHighlightButton, theme, getContext(), surfaceColor);

        updateTexts(accountButton, eventLimitButton, todayHighlightButton, monthSeparatorsButton, timeHighlightButton);

        accountButton.setOnClickListener(v -> {
            showAccount = !showAccount;
            prefs.edit().putBoolean("calendar_show_account", showAccount).apply();
            callback.onOptionsChanged();
            dismiss();
        });

        eventLimitButton.setOnClickListener(v -> {
            eventLimit = getNextEventLimit(eventLimit);
            prefs.edit().putInt("calendar_event_limit", eventLimit).apply();
            callback.onOptionsChanged();
            dismiss();
        });

        todayHighlightButton.setOnClickListener(v -> {
            highlightToday = !highlightToday;
            prefs.edit().putBoolean("calendar_highlight_today", highlightToday).apply();
            callback.onOptionsChanged();
            dismiss();
        });

        monthSeparatorsButton.setOnClickListener(v -> {
            showMonthSeparators = !showMonthSeparators;
            prefs.edit().putBoolean("calendar_month_separators", showMonthSeparators).apply();
            callback.onOptionsChanged();
            dismiss();
        });

        timeHighlightButton.setOnClickListener(v -> {
            highlightEventTimes = !highlightEventTimes;
            prefs.edit().putBoolean("calendar_highlight_event_times", highlightEventTimes).apply();
            callback.onOptionsChanged();
            dismiss();
        });
    }

    private void updateTexts(TextView accountButton, TextView eventLimitButton, TextView todayHighlightButton,
            TextView monthSeparatorsButton, TextView timeHighlightButton) {
        accountButton.setText(showAccount ? "Account: On" : "Account: Off");
        eventLimitButton.setText("Events: " + eventLimit);
        todayHighlightButton.setText(highlightToday ? "Today dot: On" : "Today dot: Off");
        monthSeparatorsButton.setText(showMonthSeparators ? "Month separators: On" : "Month separators: Off");
        timeHighlightButton.setText(highlightEventTimes ? "Highlight time: On" : "Highlight time: Off");
    }

    private int getNextEventLimit(int current) {
        if (current == 10) {
            return 25;
        }
        if (current == 25) {
            return 50;
        }
        return 10;
    }

}
