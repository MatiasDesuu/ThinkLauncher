package org.matiasdesu.thinklauncherv2.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UnlockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("UnlockReceiver", "Device unlocked, starting service: " + intent.getAction());
        BigmeShims.queryLauncherProvider(context);
    }
}