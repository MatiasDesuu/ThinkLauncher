package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class ClockOptionsDialog extends Dialog {

    public interface OnOptionsChangedCallback {
        void onOptionsChanged();
    }

    private boolean disableAlarmWallpaper;
    private final OnOptionsChangedCallback callback;

    public ClockOptionsDialog(Context context, boolean disableAlarmWallpaper, OnOptionsChangedCallback callback) {
        super(context, R.style.NoAnimationDialog);
        this.disableAlarmWallpaper = disableAlarmWallpaper;
        this.callback = callback;
        init();
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
    }

    private void updateWallpaperText(TextView wallpaperButton) {
        wallpaperButton.setText(disableAlarmWallpaper ? "Alarm wallpaper: Off" : "Alarm wallpaper: On");
    }
}
