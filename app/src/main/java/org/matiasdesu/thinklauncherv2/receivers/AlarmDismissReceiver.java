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
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notifId);
        try {
            Intent si = new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class);
            si.setAction(org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.ACTION_STOP);
            si.putExtra("alarm_id", alarmId);
            context.startService(si);
        } catch (Exception ignored) {}
        try { context.stopService(new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class)); } catch (Exception ignored) {}
        if (alarmId != -1) {
            org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper.clearSnoozed(context, alarmId);
            org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper.rescheduleAfterFired(context, alarmId);
        }
    }
}