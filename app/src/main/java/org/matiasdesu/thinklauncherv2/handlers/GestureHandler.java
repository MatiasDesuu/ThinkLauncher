package org.matiasdesu.thinklauncherv2.handlers;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import java.lang.reflect.Method;

import org.matiasdesu.thinklauncherv2.ui.AppLauncherActivity;

public class GestureHandler {

    private Activity activity;
    private String leftApp, rightApp, downApp, upApp;
    private GestureDetector gestureDetector;
    private Runnable doubleTapAction;

    public GestureHandler(Activity activity) {
        this.activity = activity;
        loadApps();
        GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float xDiff = e2.getX() - e1.getX();
                float yDiff = e2.getY() - e1.getY();
                if (Math.abs(xDiff) > Math.abs(yDiff) && Math.abs(xDiff) > 100 && Math.abs(velocityX) > 100) {

                    if (xDiff > 0) {
                        launchApp(rightApp);
                    } else {
                        launchApp(leftApp);
                    }
                } else if (Math.abs(yDiff) > Math.abs(xDiff) && Math.abs(yDiff) > 100 && Math.abs(velocityY) > 100) {

                    if (yDiff > 0) {
                        launchApp(downApp);
                    } else {
                        launchApp(upApp);
                    }
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (doubleTapAction != null) {
                    doubleTapAction.run();
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {

                try {
                    Class<?> clazz = Class.forName("org.matiasdesu.thinklauncherv2.settings.SettingsActivity");
                    Intent intent = new Intent(activity, clazz);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    activity.startActivity(intent);
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        };
        gestureDetector = new GestureDetector(activity.getApplicationContext(), gestureListener);
    }

    public void loadApps() {
        SharedPreferences prefs = activity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        leftApp = prefs.getString("swipe_left_app", "");
        rightApp = prefs.getString("swipe_right_app", "");
        downApp = prefs.getString("swipe_down_app", "");
        upApp = prefs.getString("swipe_up_app", "");
    }

    public void setDoubleTapAction(Runnable action) {
        this.doubleTapAction = action;
    }

    private void launchApp(String packageName) {
        if ("notification_panel".equals(packageName)) {
            try {
                Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel")
                        .invoke(activity.getSystemService("statusbar"));
            } catch (Exception e) {

            }
        } else if ("app_launcher".equals(packageName)) {
            Intent intent = new Intent(activity, AppLauncherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
        } else if ("koreader_history".equals(packageName)) {
            Intent intent = new Intent(activity, org.matiasdesu.thinklauncherv2.ui.KOReaderHistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
        } else if ("launcher_settings".equals(packageName)) {
            try {
                Class<?> clazz = Class.forName("org.matiasdesu.thinklauncherv2.settings.SettingsActivity");
                Intent intent = new Intent(activity, clazz);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivity(intent);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        } else if (!packageName.isEmpty()) {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                activity.startActivity(intent);
            }
        }
    }
}