package org.matiasdesu.thinklauncherv2.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.receivers.AlarmDismissReceiver;
import org.matiasdesu.thinklauncherv2.receivers.AlarmSnoozeReceiver;
import org.matiasdesu.thinklauncherv2.ui.AlarmActivity;
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;

public class AlarmForegroundService extends Service {

    public static final String ACTION_START = "org.matiasdesu.thinklauncherv2.START_ALARM";
    public static final String ACTION_STOP = "org.matiasdesu.thinklauncherv2.STOP_ALARM";
    private static final String CHANNEL_ID = "thinklauncher_alarms";
    private static final String CHANNEL_NAME = "Alarms";
    private static final int NOTIF_ID = 2001;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private int currentAlarmId = -1;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        int alarmId = intent.getIntExtra("alarm_id", -1);
        if (ACTION_STOP.equals(action)) {
            stopAlarm(alarmId);
            return START_NOT_STICKY;
        }
        if (alarmId == -1) return START_NOT_STICKY;
        startAlarm(alarmId);
        return START_STICKY;
    }

    private void startAlarm(int alarmId) {
        currentAlarmId = alarmId;
        ClockAlarmHelper.Alarm alarm = ClockAlarmHelper.getById(this, alarmId);
        String title = "Alarm";
        String text = alarm != null && alarm.label != null && !alarm.label.trim().isEmpty() ? alarm.label : (alarm != null ? alarm.getTimeText() : "Alarm");
        if (alarm != null && alarm.label != null && !alarm.label.trim().isEmpty()) text = alarm.getTimeText() + " - " + alarm.label;
        else if (alarm != null) text = alarm.getTimeText();

        ensureChannel();

        Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
        fullScreenIntent.putExtra("alarm_id", alarmId);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent fullScreenPi = PendingIntent.getActivity(this, alarmId + 900000, fullScreenIntent, pendingFlags);

        Intent dismissIntent = new Intent(this, AlarmDismissReceiver.class);
        dismissIntent.putExtra("alarm_id", alarmId);
        dismissIntent.putExtra("notif_id", NOTIF_ID);
        PendingIntent dismissPi = PendingIntent.getBroadcast(this, alarmId + 100000, dismissIntent, pendingFlags);

        Intent snoozeIntent = new Intent(this, AlarmSnoozeReceiver.class);
        snoozeIntent.putExtra("alarm_id", alarmId);
        snoozeIntent.putExtra("notif_id", NOTIF_ID);
        PendingIntent snoozePi = PendingIntent.getBroadcast(this, alarmId + 200000, snoozeIntent, pendingFlags);

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.time)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPi, true)
                .addAction(0, "Dismiss", dismissPi)
                .addAction(0, "Snooze 10m", snoozePi)
                .setVibrate(new long[]{0, 500, 500, 500})
                .build();

        startForeground(NOTIF_ID, notif);

        Intent activityIntent = new Intent(this, AlarmActivity.class);
        activityIntent.putExtra("alarm_id", alarmId);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try { startActivity(activityIntent); } catch (Exception ignored) {}

        startSoundAndVibration();
    }

    private void stopAlarm(int alarmId) {
        if (currentAlarmId != -1 && currentAlarmId != alarmId) return;
        stopSoundAndVibration();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void startSoundAndVibration() {
        try {
            Uri toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (toneUri == null) toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (toneUri == null) toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (toneUri != null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, toneUri);
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
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
                    if (mediaPlayer != null) { mediaPlayer.setLooping(true); mediaPlayer.start(); }
                }
            } catch (Exception ignored) {}
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = new long[]{0, 500, 500, 500, 500};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                else vibrator.vibrate(pattern, 0);
            }
        } catch (Exception ignored) {}
    }

    private void stopSoundAndVibration() {
        try { if (mediaPlayer != null) { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); mediaPlayer.release(); mediaPlayer = null; } } catch (Exception ignored) {}
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) {}
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null && existing.getSound() == null) nm.deleteNotificationChannel(CHANNEL_ID);
            else if (existing != null) return;
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Alarm notifications");
            ch.enableVibration(true);
            ch.setBypassDnd(true);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            ch.setSound(alarmSound, attrs);
            ch.setShowBadge(true);
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        stopSoundAndVibration();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
