package org.matiasdesu.thinklauncherv2.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.matiasdesu.thinklauncherv2.utils.ClockTimerHelper;

public class TimerCancelReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int timerId = intent.getIntExtra("timer_id", -1);
        if (timerId != -1) {
            ClockTimerHelper.cancelRunning(context, timerId);
            ClockTimerHelper.clearPaused(context, timerId);
        }
    }
}
