package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class ClockStopwatchHelper {

    private static final String KEY_START = "clock_stopwatch_start";
    private static final String KEY_ELAPSED = "clock_stopwatch_elapsed";
    private static final String KEY_RUNNING = "clock_stopwatch_running";
    private static final String KEY_PAUSED = "clock_stopwatch_paused";

    private ClockStopwatchHelper() {}

    public static void start(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (isRunning(context)) return;
        long now = System.currentTimeMillis();
        int elapsed = prefs.getInt(KEY_ELAPSED, 0);
        if (isPaused(context)) elapsed = prefs.getInt(KEY_PAUSED, elapsed);
        prefs.edit().putLong(KEY_START, now - (long) elapsed * 1000L)
                .putInt(KEY_RUNNING, 1)
                .remove(KEY_PAUSED)
                .apply();
    }

    public static void pause(Context context) {
        if (!isRunning(context)) return;
        int elapsed = getElapsedSec(context);
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_PAUSED, elapsed)
                .putInt(KEY_RUNNING, 0)
                .remove(KEY_START)
                .apply();
    }

    public static void resume(Context context) {
        if (!isPaused(context)) return;
        int elapsed = getPausedElapsed(context);
        long now = System.currentTimeMillis();
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_START, now - (long) elapsed * 1000L)
                .putInt(KEY_RUNNING, 1)
                .remove(KEY_PAUSED)
                .apply();
    }

    public static void reset(Context context) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
                .remove(KEY_START)
                .remove(KEY_ELAPSED)
                .remove(KEY_RUNNING)
                .remove(KEY_PAUSED)
                .apply();
    }

    public static void stop(Context context) {
        reset(context);
    }

    public static boolean isRunning(Context context) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt(KEY_RUNNING, 0) == 1
                && context.getSharedPreferences("prefs", Context.MODE_PRIVATE).contains(KEY_START);
    }

    public static boolean isPaused(Context context) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).contains(KEY_PAUSED);
    }

    public static int getElapsedSec(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (isPaused(context)) return prefs.getInt(KEY_PAUSED, 0);
        if (isRunning(context)) {
            long start = prefs.getLong(KEY_START, 0);
            if (start == 0) return prefs.getInt(KEY_ELAPSED, 0);
            long diff = System.currentTimeMillis() - start;
            if (diff < 0) diff = 0;
            return (int) (diff / 1000L);
        }
        return prefs.getInt(KEY_ELAPSED, 0);
    }

    public static int getPausedElapsed(Context context) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt(KEY_PAUSED, 0);
    }

    public static String formatElapsed(int sec) {
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}
