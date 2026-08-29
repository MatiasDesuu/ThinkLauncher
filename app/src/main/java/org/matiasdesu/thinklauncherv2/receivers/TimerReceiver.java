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
import org.matiasdesu.thinklauncherv2.utils.ClockTimerHelper;

public class TimerReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "thinklauncher_alarms";
    private static final String CHANNEL_NAME = "Alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        int timerId = intent.getIntExtra("timer_id", -1);
        ClockTimerHelper.Timer timer = timerId != -1 ? ClockTimerHelper.getById(context, timerId) : null;

        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove("clock_timer_running_" + timerId).remove("clock_timer_paused_" + timerId).apply();
        android.app.NotificationManager nm2 = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm2 != null) nm2.cancel(timerId + 910000);

        String title = "Timer";
        String text = timer != null && timer.label != null && !timer.label.trim().isEmpty() ? timer.label : (timer != null ? timer.getDurationText() : "Timer finished");
        if (timer != null && timer.label != null && !timer.label.trim().isEmpty()) text = timer.getDurationText() + " - " + timer.label;

        Intent serviceIntent = new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class);
        serviceIntent.setAction(org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.ACTION_START);
        serviceIntent.putExtra("timer_id", timerId);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
            else context.startService(serviceIntent);
        } catch (Exception ignored) {
            ensureChannel(context);
            Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
            fullScreenIntent.putExtra("timer_id", timerId);
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent fullScreenPi = PendingIntent.getActivity(context, timerId + 920000, fullScreenIntent, flags);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.time)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(fullScreenPi, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) builder.setVibrate(new long[]{0, 500, 500, 500});
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(timerId + 920000, builder.build());
            try { context.startActivity(fullScreenIntent); } catch (Exception e2) {}
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
