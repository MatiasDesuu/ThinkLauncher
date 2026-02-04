package org.matiasdesu.thinklauncherv2.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/* BIGME shim. Since the default launcher is used to control the gestures, we need to recreate the process,
 * if it has been killed. We will do that every time we enter this launcher, and everytime we reload, for now.
 */
public class BigmeShims {
    private static final String ELAUNCHER_TAG = "eLauncher";
    private static final String BIGME_LAUNCHER_AUTHORITY = "com.xrz.LauncherProvider";

    public static void queryLauncherProvider(@NonNull Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q)
            return;

        Log.d(ELAUNCHER_TAG, "device brand: " + android.os.Build.BRAND);

        // check if the device vendor is bigme hibreak, exit otherwise
        // Build.MODEL == "HiBreak"
        if (!"HiBreak".equals(android.os.Build.MODEL))
            return;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;

        try {
            Log.d(ELAUNCHER_TAG, "call started");
            Bundle ret = contentResolver.call(BIGME_LAUNCHER_AUTHORITY, "custom_key", "false", null);
            Log.d(ELAUNCHER_TAG, "call succeeded");
        } catch (Exception e) {
            Log.e(ELAUNCHER_TAG, "call failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void registerUnlockReceiver(@NonNull Context context) {
        // Register the UnlockReceiver to listen for ACTION_USER_PRESENT
        if (!"HiBreak".equals(android.os.Build.MODEL))
            return;
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HIDE_BAKCLOGO");
        filter.addAction("android.intent.action.SHOW_BACKLOGO");
        UnlockReceiver unlockReceiver = new UnlockReceiver();
        ContextCompat.registerReceiver(context, unlockReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }
}