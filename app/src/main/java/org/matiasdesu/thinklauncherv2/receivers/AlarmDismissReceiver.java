package org.matiasdesu.thinklauncherv2.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int notifId = intent.getIntExtra("notif_id", 0);
        int alarmId = intent.getIntExtra("alarm_id", -1);
        int timerId = intent.getIntExtra("timer_id", -1);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notifId);
        try {
            Intent si = new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class);
            si.setAction(org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.ACTION_STOP);
            if (timerId != -1) si.putExtra("timer_id", timerId);
            else si.putExtra("alarm_id", alarmId);
            context.startService(si);
        } catch (Exception ignored) {}
        try { context.stopService(new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class)); } catch (Exception ignored) {}
        if (timerId != -1) {
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove("clock_timer_running_" + timerId).apply();
            if (nm != null) nm.cancel(timerId + 910000);
            nm.cancel(timerId + 920000);
        } else if (alarmId != -1) {
            org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper.clearSnoozed(context, alarmId);
            org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper.rescheduleAfterFired(context, alarmId);
        }
    }
}