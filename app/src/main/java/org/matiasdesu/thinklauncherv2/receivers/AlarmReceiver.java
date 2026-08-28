package org.matiasdesu.thinklauncherv2.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.AlarmActivity;
import org.matiasdesu.thinklauncherv2.ui.ClockActivity;
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "thinklauncher_alarms";
    private static final String CHANNEL_NAME = "Alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", -1);
        boolean isSnooze = intent.getBooleanExtra("is_snooze", false);
        ClockAlarmHelper.Alarm alarm = alarmId != -1 ? ClockAlarmHelper.getById(context, alarmId) : null;

        String title = "Alarm";
        String text = alarm != null && alarm.label != null && !alarm.label.trim().isEmpty()
                ? alarm.label : (alarm != null ? alarm.getTimeText() : "Alarm");
        if (alarm != null && alarm.label != null && !alarm.label.trim().isEmpty()) {
            text = alarm.getTimeText() + " - " + alarm.label;
        } else if (alarm != null) {
            text = alarm.getTimeText();
        }
        if (isSnooze) {
            title = "Snoozed Alarm";
            org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper.clearSnoozed(context, alarmId);
        }

        Intent serviceIntent = new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class);
        serviceIntent.setAction(org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.ACTION_START);
        serviceIntent.putExtra("alarm_id", alarmId);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
            else context.startService(serviceIntent);
        } catch (Exception ignored) {
            ensureChannel(context);
            Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
            fullScreenIntent.putExtra("alarm_id", alarmId);
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent fullScreenPi = PendingIntent.getActivity(context, alarmId + 900000, fullScreenIntent, flags);
            Intent openIntent = new Intent(context, ClockActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentPi = PendingIntent.getActivity(context, alarmId, openIntent, flags);
            Intent dismissIntent = new Intent(context, AlarmDismissReceiver.class);
            dismissIntent.putExtra("alarm_id", alarmId);
            dismissIntent.putExtra("notif_id", alarmId);
            PendingIntent dismissPi = PendingIntent.getBroadcast(context, alarmId + 100000, dismissIntent, flags);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.time)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(contentPi)
                    .setFullScreenIntent(fullScreenPi, true)
                    .addAction(0, "Dismiss", dismissPi);
            Intent snoozeIntent = new Intent(context, AlarmSnoozeReceiver.class);
            snoozeIntent.putExtra("alarm_id", alarmId);
            snoozeIntent.putExtra("notif_id", alarmId);
            PendingIntent snoozePi = PendingIntent.getBroadcast(context, alarmId + 200000, snoozeIntent, flags);
            builder.addAction(0, "Snooze 10m", snoozePi);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) builder.setVibrate(new long[]{0, 500, 500, 500});
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(alarmId != -1 ? alarmId : 0, builder.build());
            try { context.startActivity(fullScreenIntent); } catch (Exception e2) {}
        }

        if (!isSnooze && alarm != null && alarm.hasRepeat()) {
            org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper.rescheduleAfterFired(context, alarmId);
        } else if (!isSnooze && alarm != null && !alarm.hasRepeat()) {

        }

    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null) {

                if (existing.getSound() != null) return;
                nm.deleteNotificationChannel(CHANNEL_ID);
            }
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Alarm notifications");
            ch.enableVibration(true);
            ch.setBypassDnd(true);
            ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            android.net.Uri alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
            android.media.AudioAttributes attrs = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            ch.setSound(alarmSound, attrs);
            ch.setShowBadge(true);
            nm.createNotificationChannel(ch);
        }
    }
}