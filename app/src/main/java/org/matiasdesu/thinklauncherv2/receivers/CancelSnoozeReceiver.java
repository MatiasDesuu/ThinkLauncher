package org.matiasdesu.thinklauncherv2.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;

public class CancelSnoozeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", -1);
        if (alarmId != -1) {
            ClockAlarmHelper.clearSnoozed(context, alarmId);
            ClockAlarmHelper.Alarm alarm = ClockAlarmHelper.getById(context, alarmId);
            if (alarm != null && !alarm.hasRepeat()) {
                ClockAlarmHelper.setEnabled(context, alarmId, false);
            }
            try { Toast.makeText(context, "Snooze cancelled", Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
        }
    }
}
