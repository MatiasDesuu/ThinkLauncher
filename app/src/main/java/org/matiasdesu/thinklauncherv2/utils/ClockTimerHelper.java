package org.matiasdesu.thinklauncherv2.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ClockTimerHelper {

    private static final String PREF_TIMERS = "clock_timers_json";
    private static final String PREF_NEXT_ID = "clock_next_timer_id";
    private static final String PREF_RUNNING_PREFIX = "clock_timer_running_";
    private static final String PREF_PAUSED_PREFIX = "clock_timer_paused_";
    private static final String CHANNEL_ID = "thinklauncher_timers";
    private static final String CHANNEL_NAME = "Timers";

    public static class Timer {
        public int id;
        public int durationSec;
        public String label = "";

        public Timer() {}

        public Timer(int id, int durationSec, String label) {
            this.id = id;
            this.durationSec = durationSec;
            this.label = label == null ? "" : label;
        }

        public String getDurationText() {
            int h = durationSec / 3600;
            int m = (durationSec % 3600) / 60;
            int s = durationSec % 60;
            if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
            return String.format("%02d:%02d", m, s);
        }

        public String getCompactText() {
            int h = durationSec / 3600;
            int m = (durationSec % 3600) / 60;
            int s = durationSec % 60;
            StringBuilder sb = new StringBuilder();
            if (h > 0) sb.append(h).append("h ");
            if (m > 0) sb.append(m).append("m ");
            if (s > 0 || sb.length() == 0) sb.append(s).append("s");
            return sb.toString().trim();
        }
    }

    private ClockTimerHelper() {}

    public static List<Timer> loadTimers(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String json = prefs.getString(PREF_TIMERS, null);
        List<Timer> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Timer t = new Timer();
                t.id = o.optInt("id", 0);
                t.durationSec = o.optInt("d", 0);
                t.label = o.optString("l", "");
                out.add(t);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void saveTimers(Context context, List<Timer> timers) {
        JSONArray arr = new JSONArray();
        try {
            for (Timer t : timers) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("d", t.durationSec);
                o.put("l", t.label == null ? "" : t.label);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(PREF_TIMERS, arr.toString()).apply();
    }

    public static int nextId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int id = prefs.getInt(PREF_NEXT_ID, 1);
        prefs.edit().putInt(PREF_NEXT_ID, id + 1).apply();
        return id;
    }

    public static void addOrUpdate(Context context, Timer timer) {
        List<Timer> list = loadTimers(context);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == timer.id) {
                list.set(i, timer);
                found = true;
                break;
            }
        }
        if (!found) list.add(timer);
        saveTimers(context, list);
    }

    public static void delete(Context context, int id) {
        cancelRunning(context, id);
        clearPaused(context, id);
        List<Timer> list = loadTimers(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        saveTimers(context, list);
    }

    public static Timer getById(Context context, int id) {
        for (Timer t : loadTimers(context)) if (t.id == id) return t;
        return null;
    }

    private static void cancelOthers(Context context, int keepId) {
        List<Timer> all = loadTimers(context);
        for (Timer t : all) {
            if (t.id == keepId) continue;
            if (isRunning(context, t.id) || isPaused(context, t.id)) {
                cancelRunning(context, t.id);
                clearPaused(context, t.id);
            }
        }
    }

    public static void start(Context context, int timerId) {
        Timer t = getById(context, timerId);
        if (t == null || t.durationSec <= 0) return;
        cancelOthers(context, timerId);
        clearPaused(context, timerId);
        long triggerAt = System.currentTimeMillis() + (long) t.durationSec * 1000L;
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putLong(PREF_RUNNING_PREFIX + timerId, triggerAt).apply();
        scheduleAlarm(context, timerId, triggerAt);
        showRunningNotification(context, timerId, triggerAt);
    }

    public static void startWithRemaining(Context context, int timerId, int remainingSec) {
        if (remainingSec <= 0) return;
        cancelOthers(context, timerId);
        clearPaused(context, timerId);
        long triggerAt = System.currentTimeMillis() + (long) remainingSec * 1000L;
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putLong(PREF_RUNNING_PREFIX + timerId, triggerAt).apply();
        scheduleAlarm(context, timerId, triggerAt);
        showRunningNotification(context, timerId, triggerAt);
    }

    public static void cancelRunning(Context context, int timerId) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove(PREF_RUNNING_PREFIX + timerId).remove(PREF_PAUSED_PREFIX + timerId).apply();
        cancelAlarm(context, timerId);
        cancelRunningNotification(context, timerId);
    }

    public static void pause(Context context, int timerId) {
        if (!isRunning(context, timerId)) return;
        int remaining = getRemainingSec(context, timerId);
        if (remaining <= 0) remaining = 1;
        cancelRunning(context, timerId);
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt(PREF_PAUSED_PREFIX + timerId, remaining).apply();
    }

    public static void resume(Context context, int timerId) {
        if (!isPaused(context, timerId)) return;
        int remaining = getPausedRemaining(context, timerId);
        if (remaining <= 0) {
            clearPaused(context, timerId);
            return;
        }
        cancelOthers(context, timerId);
        clearPaused(context, timerId);
        startWithRemaining(context, timerId, remaining);
    }

    public static boolean handleExpired(Context context, int timerId) {
        long end = getEndMillis(context, timerId);
        if (end == 0) return false;
        if (System.currentTimeMillis() < end) return false;
        Timer t = getById(context, timerId);
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove(PREF_RUNNING_PREFIX + timerId).remove(PREF_PAUSED_PREFIX + timerId).apply();
        cancelAlarm(context, timerId);
        cancelRunningNotification(context, timerId);
        Intent serviceIntent = new Intent(context, org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.class);
        serviceIntent.setAction(org.matiasdesu.thinklauncherv2.services.AlarmForegroundService.ACTION_START);
        serviceIntent.putExtra("timer_id", timerId);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
            else context.startService(serviceIntent);
        } catch (Exception ignored) {
            Intent fullScreenIntent = new Intent(context, org.matiasdesu.thinklauncherv2.ui.AlarmActivity.class);
            fullScreenIntent.putExtra("timer_id", timerId);
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try { context.startActivity(fullScreenIntent); } catch (Exception e2) {}
        }
        return true;
    }

    public static boolean isPaused(Context context, int timerId) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).contains(PREF_PAUSED_PREFIX + timerId);
    }

    public static int getPausedRemaining(Context context, int timerId) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt(PREF_PAUSED_PREFIX + timerId, 0);
    }

    public static void clearPaused(Context context, int timerId) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove(PREF_PAUSED_PREFIX + timerId).apply();
    }

    public static boolean isRunning(Context context, int timerId) {
        long end = getEndMillis(context, timerId);
        if (end == 0) return false;
        return System.currentTimeMillis() <= end;
    }

    public static long getEndMillis(Context context, int timerId) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getLong(PREF_RUNNING_PREFIX + timerId, 0);
    }

    public static int getRemainingSec(Context context, int timerId) {
        if (isPaused(context, timerId)) return getPausedRemaining(context, timerId);
        long end = getEndMillis(context, timerId);
        if (end == 0) return 0;
        long diff = end - System.currentTimeMillis();
        if (diff <= 0) return 0;
        return (int) ((diff + 999) / 1000L);
    }

    public static String formatRemaining(int sec) {
        if (sec <= 0) return "00:00";
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    private static void scheduleAlarm(Context context, int timerId, long triggerAt) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = getPendingIntent(context, timerId);
        Intent showIntent = new Intent(context, org.matiasdesu.thinklauncherv2.ui.ClockActivity.class);
        showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int sFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) sFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent showPi = PendingIntent.getActivity(context, timerId + 800000, showIntent, sFlags);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAt, showPi);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    return;
                }
            }
            try {
                am.setAlarmClock(info, pi);
                return;
            } catch (Exception e) {
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi); } catch (Exception ignored) {}
        }
    }

    private static void cancelAlarm(Context context, int timerId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = getPendingIntent(context, timerId);
        am.cancel(pi);
        pi.cancel();
    }

    private static PendingIntent getPendingIntent(Context context, int timerId) {
        Intent intent = new Intent(context, org.matiasdesu.thinklauncherv2.receivers.TimerReceiver.class);
        intent.putExtra("timer_id", timerId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, timerId + 900000, intent, flags);
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            android.app.NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null) {
                if (existing.getImportance() >= android.app.NotificationManager.IMPORTANCE_DEFAULT) return;
                nm.deleteNotificationChannel(CHANNEL_ID);
            }
            android.app.NotificationChannel ch = new android.app.NotificationChannel(CHANNEL_ID, CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Timers");
            ch.setShowBadge(true);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        }
    }

    private static void showRunningNotification(Context context, int timerId, long triggerAt) {
        ensureChannel(context);
        Timer t = getById(context, timerId);
        String title = "Timer running";
        String text = t != null ? t.getDurationText() : "";
        if (t != null && t.label != null && !t.label.trim().isEmpty()) text = text + " " + t.label;
        Intent cancelIntent = new Intent(context, org.matiasdesu.thinklauncherv2.receivers.TimerCancelReceiver.class);
        cancelIntent.putExtra("timer_id", timerId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent cancelPi = PendingIntent.getBroadcast(context, timerId + 910000, cancelIntent, flags);
        Intent contentIntent = new Intent(context, org.matiasdesu.thinklauncherv2.ui.ClockActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(context, timerId + 911000, contentIntent, flags);
        androidx.core.app.NotificationCompat.Builder b = new androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(org.matiasdesu.thinklauncherv2.R.drawable.time)
                .setContentTitle(title)
                .setContentText(text.isEmpty() ? "Running" : text)
                .setContentIntent(contentPi)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(0, "Cancel", cancelPi);
        android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(timerId + 910000, b.build());
    }

    private static void cancelRunningNotification(Context context, int timerId) {
        android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(timerId + 910000);
    }

    public static void clearExpiredRunning(Context context) {
        List<Timer> timers = loadTimers(context);
        for (Timer t : timers) {
            long end = context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getLong(PREF_RUNNING_PREFIX + t.id, 0);
            if (end != 0 && System.currentTimeMillis() > end + 2000) {
                context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove(PREF_RUNNING_PREFIX + t.id).apply();
                cancelAlarm(context, t.id);
                cancelRunningNotification(context, t.id);
            }
        }
    }
}
