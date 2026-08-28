package org.matiasdesu.thinklauncherv2.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.WallpaperHelper;

public class AlarmActivity extends AppCompatActivity {

    private int alarmId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        boolean opacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1;
        boolean disableAlarmWallpaper = prefs.getBoolean("alarm_disable_wallpaper", true);
        setTheme(org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);

        if (disableAlarmWallpaper) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(ThemeUtils.getBgColor(theme, this));
                getWindow().setNavigationBarColor(ThemeUtils.getBgColor(theme, this));
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        setContentView(R.layout.activity_alarm);

        View contentLayout = findViewById(R.id.content_layout);
        int defaultPadding = (int) (24 * getResources().getDisplayMetrics().density);
        if (contentLayout != null) {
            contentLayout.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding);
        }

        ImageView wallpaperView = findViewById(R.id.wallpaper_view);
        int surfaceColor = ThemeUtils.getBgColor(theme, this);
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(disableAlarmWallpaper ? surfaceColor : android.graphics.Color.TRANSPARENT);
        }
        if (contentLayout != null) {
            contentLayout.setBackgroundColor(disableAlarmWallpaper ? surfaceColor : android.graphics.Color.TRANSPARENT);
        }

        if (disableAlarmWallpaper) {
            if (wallpaperView != null) {
                wallpaperView.setVisibility(View.GONE);
            }
        } else {
            if (wallpaperView != null) {
                int[] screen = WallpaperHelper.getScreenDimensions(this);
                android.graphics.Bitmap bitmap = WallpaperHelper.getWallpaperForScreenCached(this, screen[0], screen[1], false, 3);
                if (bitmap != null) {
                    wallpaperView.setImageBitmap(bitmap);
                    wallpaperView.setVisibility(View.VISIBLE);
                } else {
                    wallpaperView.setVisibility(View.GONE);
                }
            }
        }

        View divider = findViewById(R.id.divider);
        if (divider != null) divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        alarmId = getIntent().getIntExtra("alarm_id", -1);
        ClockAlarmHelper.Alarm alarm = alarmId != -1 ? ClockAlarmHelper.getById(this, alarmId) : null;

        TextView timeView = findViewById(R.id.alarm_time);
        TextView labelView = findViewById(R.id.alarm_label);
        TextView repeatView = findViewById(R.id.alarm_repeat);
        TextView snoozeBtn = findViewById(R.id.btn_snooze);
        TextView dismissBtn = findViewById(R.id.btn_dismiss);

        if (alarm != null) {
            timeView.setText(alarm.getTimeText());
            if (alarm.label != null && !alarm.label.trim().isEmpty()) {
                labelView.setText(alarm.label);
                labelView.setVisibility(View.VISIBLE);
            } else {
                labelView.setVisibility(View.GONE);
            }
            repeatView.setText(alarm.getRepeatText());
        } else {
            timeView.setText("Alarm");
            labelView.setVisibility(View.GONE);
            repeatView.setText("");
        }

        ThemeUtils.applyTextColor(timeView, theme, this);
        ThemeUtils.applyTextColor(labelView, theme, this);
        ThemeUtils.applyTextColor(repeatView, theme, this);
        FontHelper.applyToViewTree(this, findViewById(android.R.id.content));

        int surface = ThemeUtils.getBgColor(theme, this);
        org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.applyButtonTheme(snoozeBtn, theme, this, surface);
        org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.applyButtonTheme(dismissBtn, theme, this, surface);
        int txt = ThemeUtils.getTextColor(theme, this);
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(txt);
        d.setStroke((int)(2 * getResources().getDisplayMetrics().density), txt);
        int r = org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.getCornerRadiusPx(this);
        if (r > 0) d.setCornerRadius(r);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        dismissBtn.setBackground(d);
        dismissBtn.setTextColor(surface);
        dismissBtn.setPadding(pad, pad, pad, pad);

        snoozeBtn.setOnClickListener(v -> doSnooze());
        dismissBtn.setOnClickListener(v -> doDismiss());
    }

    private void stopServiceSound() {
        try {
            Intent si = new Intent(this, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class);
            si.setAction(org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.ACTION_STOP);
            si.putExtra("alarm_id", alarmId);
            startService(si);
        } catch (Exception ignored) {}
        try { stopService(new Intent(this, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class)); } catch (Exception ignored) {}
    }

    private void doSnooze() {
        stopServiceSound();
        try { android.widget.Toast.makeText(this, "Snoozed 10 minutes", android.widget.Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
        if (alarmId != -1) {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(alarmId);
            android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                long triggerAt = System.currentTimeMillis() + 10 * 60 * 1000L;
                ClockAlarmHelper.setSnoozed(this, alarmId, triggerAt);
                android.content.Intent ri = new android.content.Intent(this, org.matiasdesu.thinklauncherv2.receivers.AlarmReceiver.class);
                ri.putExtra("alarm_id", alarmId);
                ri.putExtra("is_snooze", true);
                int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
                android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(this, alarmId + 500000, ri, flags);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        am.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    } else {
                        am.set(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    }
                } catch (SecurityException e) {
                    am.set(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            }
        }
        finishAndRemoveTask();
    }

    private void doDismiss() {
        stopServiceSound();
        if (alarmId != -1) {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(alarmId);
            ClockAlarmHelper.clearSnoozed(this, alarmId);
            ClockAlarmHelper.rescheduleAfterFired(this, alarmId);
        }
        finishAndRemoveTask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

    }
}