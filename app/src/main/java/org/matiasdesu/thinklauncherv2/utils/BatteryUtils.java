package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;

public class BatteryUtils {

    private static final long CACHE_TTL_MS = 30_000L;
    private static long lastFetchElapsed = -1;
    private static int cachedPercent = 0;

    public static int getBatteryPercentage(Context context) {
        long now = SystemClock.elapsedRealtime();
        if (lastFetchElapsed != -1 && now - lastFetchElapsed < CACHE_TTL_MS) {
            return cachedPercent;
        }
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int percent = 0;
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale != -1) {
                percent = (int) ((level / (float) scale) * 100);
            }
        }
        lastFetchElapsed = now;
        cachedPercent = percent;
        return percent;
    }
}
