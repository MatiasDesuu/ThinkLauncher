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
import org.matiasdesu.thinklauncherv2.ui.ClockActivity;
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "thinklauncher_alarms";
    private static final String CHANNEL_NAME = "Alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", -1);
        ClockAlarmHelper.Alarm alarm = alarmId != -1 ? ClockAlarmHelper.getById(context, alarmId) : null;

        String title = "Alarm";
        String text = alarm != null && alarm.label != null && !alarm.label.trim().isEmpty()
                ? alarm.label : (alarm != null ? alarm.getTimeText() : "Alarm");
        if (alarm != null && alarm.label != null && !alarm.label.trim().isEmpty()) {
            text = alarm.getTimeText() + " - " + alarm.label;
        } else if (alarm != null) {
            text = alarm.getTimeText();
        }

        ensureChannel(context);

        Intent openIntent = new Intent(context, ClockActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
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
                .setOngoing(false)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentPi)
                .addAction(0, "Dismiss", dismissPi);

        // Snooze 10 min action
        Intent snoozeIntent = new Intent(context, AlarmSnoozeReceiver.class);
        snoozeIntent.putExtra("alarm_id", alarmId);
        snoozeIntent.putExtra("notif_id", alarmId);
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, alarmId + 200000, snoozeIntent, flags);
        builder.addAction(0, "Snooze 10m", snoozePi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVibrate(new long[]{0, 500, 500, 500});
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(alarmId != -1 ? alarmId : 0, builder.build());

        // reschedule or disable
        if (alarmId != -1) ClockAlarmHelper.rescheduleAfterFired(context, alarmId);
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null) return;
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Alarm notifications");
            ch.enableVibration(true);
            ch.setShowBadge(true);
            ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
    }
}
