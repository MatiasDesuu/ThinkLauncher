package org.matiasdesu.thinklauncherv2.receivers;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmSnoozeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", -1);
        int notifId = intent.getIntExtra("notif_id", 0);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notifId);
        try {
            Intent si = new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class);
            si.setAction(org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.ACTION_STOP);
            si.putExtra("alarm_id", alarmId);
            context.startService(si);
        } catch (Exception ignored) {}
        try { context.stopService(new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class)); } catch (Exception ignored) {}
        try { android.widget.Toast.makeText(context, "Snoozed 10 minutes", android.widget.Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
        long triggerAt = System.currentTimeMillis() + 10 * 60 * 1000L;
        org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper.setSnoozed(context, alarmId, triggerAt);
        Intent ri = new Intent(context, AlarmReceiver.class);
        ri.putExtra("alarm_id", alarmId);
        ri.putExtra("is_snooze", true);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

        int snoozeCode = alarmId + 500000;
        PendingIntent pi = PendingIntent.getBroadcast(context, snoozeCode, ri, flags);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            } catch (SecurityException e) {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
    }
}