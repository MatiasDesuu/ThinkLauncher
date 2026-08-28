package org.matiasdesu.thinklauncherv2.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class AlarmActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private int alarmId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        boolean opacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1;
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);

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

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
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

        int surface = backdrop.surfaceColor;
        org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.applyButtonTheme(snoozeBtn, theme, this, surface);
        org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.applyButtonTheme(dismissBtn, theme, this, surface);

        snoozeBtn.setOnClickListener(v -> doSnooze());
        dismissBtn.setOnClickListener(v -> doDismiss());

        startAlarmSoundAndVibration();
    }

    private void startAlarmSoundAndVibration() {

        try {
            Uri toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (toneUri == null) toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (toneUri == null) toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (toneUri != null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, toneUri);
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (Exception e) {

            try {
                if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
                Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (fallback != null) {
                    mediaPlayer = MediaPlayer.create(this, fallback);
                    if (mediaPlayer != null) {
                        mediaPlayer.setLooping(true);
                        mediaPlayer.start();
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = new long[]{0, 500, 500, 500, 500};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception ignored) {}
    }

    private void stopSoundAndVibration() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception ignored) {}
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Exception ignored) {}
    }

    private void doSnooze() {
        stopSoundAndVibration();
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
        stopSoundAndVibration();
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
        stopSoundAndVibration();
    }

    @Override
    public void onBackPressed() {

    }
}