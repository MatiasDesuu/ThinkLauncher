package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShortcutHelper {

    public static List<ShortcutInfo> getShortcuts(Context context, String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return Collections.emptyList();
        }

        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (launcherApps == null || !launcherApps.hasShortcutHostPermission()) {
            return Collections.emptyList();
        }

        UserHandle user = Process.myUserHandle();
        LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
        query.setPackage(packageName);
        query.setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
        );

        try {
            List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, user);
            return shortcuts != null ? shortcuts : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static boolean hasShortcuts(Context context, String packageName) {
        return !getShortcuts(context, packageName).isEmpty();
    }
}
