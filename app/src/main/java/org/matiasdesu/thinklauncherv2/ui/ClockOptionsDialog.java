package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;

public class ClockOptionsDialog extends Dialog {

    public interface OnOptionsChangedCallback {
        void onOptionsChanged();
    }

    public interface OnFilterChangedCallback {
        void onFilterChanged(int filterMode);
    }

    private boolean disableAlarmWallpaper;
    private final OnOptionsChangedCallback callback;
    private final OnFilterChangedCallback filterCallback;
    private int currentFilterMode;

    public ClockOptionsDialog(Context context, boolean disableAlarmWallpaper, int filterMode, OnOptionsChangedCallback callback, OnFilterChangedCallback filterCallback) {
        super(context, R.style.NoAnimationDialog);
        this.disableAlarmWallpaper = disableAlarmWallpaper;
        this.currentFilterMode = filterMode;
        this.callback = callback;
        this.filterCallback = filterCallback;
        init();
    }

    public ClockOptionsDialog(Context context, boolean disableAlarmWallpaper, OnOptionsChangedCallback callback) {
        this(context, disableAlarmWallpaper, context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("clock_filter_by", ClockActivity.FILTER_ALARMS), callback, null);
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_clock_options);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView wallpaperButton = findViewById(R.id.alarm_wallpaper_button);
        DialogEffectHelper.applyButtonTheme(wallpaperButton, theme, getContext(), surfaceColor);
        updateWallpaperText(wallpaperButton);
        wallpaperButton.setOnClickListener(v -> {
            disableAlarmWallpaper = !disableAlarmWallpaper;
            prefs.edit().putBoolean("alarm_disable_wallpaper", disableAlarmWallpaper).apply();
            updateWallpaperText(wallpaperButton);
            if (callback != null) callback.onOptionsChanged();
        });

        TextView filterButton = findViewById(R.id.filter_button);
        if (filterButton != null) {
            if (currentFilterMode != ClockActivity.FILTER_ALARMS && currentFilterMode != ClockActivity.FILTER_TIMERS) {
                currentFilterMode = ClockActivity.FILTER_ALARMS;
            }
            DialogEffectHelper.applyButtonTheme(filterButton, theme, getContext(), surfaceColor);
            updateFilterText(filterButton);
            filterButton.setOnClickListener(v -> {
                currentFilterMode = currentFilterMode == ClockActivity.FILTER_ALARMS ? ClockActivity.FILTER_TIMERS : ClockActivity.FILTER_ALARMS;
                prefs.edit().putInt("clock_filter_by", currentFilterMode).apply();
                updateFilterText(filterButton);
                if (filterCallback != null) filterCallback.onFilterChanged(currentFilterMode);
            });
        }
    }

    private void updateWallpaperText(TextView wallpaperButton) {
        wallpaperButton.setText("Enable wallpaper: " + (disableAlarmWallpaper ? "Off" : "On"));
    }

    private void updateFilterText(TextView btn) {
        String label = currentFilterMode == ClockActivity.FILTER_TIMERS ? "Timers" : "Alarms";
        btn.setText("Filter: " + label);
    }
}
