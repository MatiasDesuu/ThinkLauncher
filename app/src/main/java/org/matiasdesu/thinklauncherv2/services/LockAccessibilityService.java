package org.matiasdesu.thinklauncherv2.services;

import android.accessibilityservice.AccessibilityService;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;

public class LockAccessibilityService extends AccessibilityService {
    private static LockAccessibilityService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public static boolean lockScreen() {
        if (instance != null) {
            // Try Device Admin first if available
            DevicePolicyManager dpm = (DevicePolicyManager) instance.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(instance, LockDeviceAdminReceiver.class);
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                dpm.lockNow();
            } else {
                // Fallback to Accessibility
                instance.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
            }
            return true;
        }
        return false;
    }

    public static boolean isServiceRunning() {
        return instance != null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used
    }

    @Override
    public void onInterrupt() {
        // Not used
    }
}