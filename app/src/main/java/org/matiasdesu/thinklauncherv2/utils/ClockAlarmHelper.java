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
import java.util.Calendar;
import java.util.List;

public final class ClockAlarmHelper {

    private static final String PREF_ALARMS = "clock_alarms_json";
    private static final String PREF_NEXT_ID = "clock_next_alarm_id";
    private static final String PREF_SNOOZE_PREFIX = "clock_snooze_until_";
    private static final String SNOOZE_CHANNEL_ID = "thinklauncher_snoozed";
    private static final String SNOOZE_CHANNEL_NAME = "Snoozed Alarms";

    public static class Alarm {
        public int id;
        public int hour;
        public int minute;
        public boolean[] days = new boolean[7];
        public String label = "";
        public boolean enabled = true;

        public Alarm() {}

        public Alarm(int id, int hour, int minute, boolean[] days, String label, boolean enabled) {
            this.id = id;
            this.hour = hour;
            this.minute = minute;
            if (days != null && days.length == 7) this.days = days.clone();
            this.label = label == null ? "" : label;
            this.enabled = enabled;
        }

        public boolean hasRepeat() {
            for (boolean d : days) if (d) return true;
            return false;
        }

        public String getRepeatText() {
            if (!hasRepeat()) return "Once";
            boolean all = true;
            boolean weekdays = true;
            boolean weekend = true;
            for (int i = 0; i < 7; i++) {
                if (!days[i]) all = false;
            }
            if (all) return "Every day";

            for (int i = 0; i < 5; i++) if (!days[i]) weekdays = false;
            for (int i = 5; i < 7; i++) if (days[i]) weekdays = false;

            for (int i = 0; i < 5; i++) if (days[i]) weekend = false;
            for (int i = 5; i < 7; i++) if (!days[i]) weekend = false;
            if (weekdays) return "Weekdays";
            if (weekend) return "Weekend";
            String[] names = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 7; i++) if (days[i]) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(names[i]);
            }
            return sb.toString();
        }

        public String getTimeText() {
            return String.format("%02d:%02d", hour, minute);
        }
    }

    private ClockAlarmHelper() {}

    public static List<Alarm> loadAlarms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String json = prefs.getString(PREF_ALARMS, null);
        List<Alarm> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Alarm a = new Alarm();
                a.id = o.optInt("id", 0);
                a.hour = o.optInt("h", 0);
                a.minute = o.optInt("m", 0);
                a.label = o.optString("l", "");
                a.enabled = o.optBoolean("e", true);
                String daysStr = o.optString("d", "0000000");
                for (int d = 0; d < 7 && d < daysStr.length(); d++) {
                    a.days[d] = daysStr.charAt(d) == '1';
                }
                out.add(a);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void saveAlarms(Context context, List<Alarm> alarms) {
        JSONArray arr = new JSONArray();
        try {
            for (Alarm a : alarms) {
                JSONObject o = new JSONObject();
                o.put("id", a.id);
                o.put("h", a.hour);
                o.put("m", a.minute);
                o.put("l", a.label == null ? "" : a.label);
                o.put("e", a.enabled);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 7; i++) sb.append(a.days[i] ? '1' : '0');
                o.put("d", sb.toString());
                arr.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(PREF_ALARMS, arr.toString()).apply();
    }

    public static int nextId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int id = prefs.getInt(PREF_NEXT_ID, 1);
        prefs.edit().putInt(PREF_NEXT_ID, id + 1).apply();
        return id;
    }

    public static void addOrUpdate(Context context, Alarm alarm) {
        List<Alarm> list = loadAlarms(context);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == alarm.id) {
                list.set(i, alarm);
                found = true;
                break;
            }
        }
        if (!found) list.add(alarm);
        saveAlarms(context, list);
        if (alarm.enabled) schedule(context, alarm);
        else cancel(context, alarm.id);
    }

    public static void delete(Context context, int id) {
        List<Alarm> list = loadAlarms(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        saveAlarms(context, list);
        cancel(context, id);
        clearSnoozed(context, id);
    }

    public static void setEnabled(Context context, int id, boolean enabled) {
        List<Alarm> list = loadAlarms(context);
        for (Alarm a : list) {
            if (a.id == id) {
                a.enabled = enabled;
                saveAlarms(context, list);
                if (enabled) schedule(context, a);
                else {
                    cancel(context, id);
                    clearSnoozed(context, id);
                }
                break;
            }
        }
    }

    public static Alarm getById(Context context, int id) {
        for (Alarm a : loadAlarms(context)) if (a.id == id) return a;
        return null;
    }

    public static void schedule(Context context, Alarm alarm) {
        if (!alarm.enabled) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long triggerAt = computeNextTriggerMillis(alarm);
        PendingIntent pi = getPendingIntent(context, alarm.id);

        Intent showIntent = new Intent(context, org.matiasdesu.thinklauncherv2.ui.ClockActivity.class);
        showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int sFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) sFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent showPi = PendingIntent.getActivity(context, alarm.id + 800000, showIntent, sFlags);
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

    public static void scheduleAll(Context context) {
        for (Alarm a : loadAlarms(context)) if (a.enabled) schedule(context, a);
    }

    public static void cancel(Context context, int alarmId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = getPendingIntent(context, alarmId);
        am.cancel(pi);
        pi.cancel();
        clearSnoozed(context, alarmId);
    }

    public static void rescheduleAfterFired(Context context, int alarmId) {
        Alarm a = getById(context, alarmId);
        if (a == null || !a.enabled) return;
        if (a.hasRepeat()) {
            schedule(context, a);
        } else {
            a.enabled = false;
            List<Alarm> list = loadAlarms(context);
            for (int i = 0; i < list.size(); i++) if (list.get(i).id == alarmId) { list.set(i, a); break; }
            saveAlarms(context, list);
        }
    }

    public static void setSnoozed(Context context, int alarmId, long untilMillis) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putLong(PREF_SNOOZE_PREFIX + alarmId, untilMillis).apply();
        showSnoozedNotification(context, alarmId, untilMillis);
    }

    public static long getSnoozedUntil(Context context, int alarmId) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getLong(PREF_SNOOZE_PREFIX + alarmId, 0);
    }

    public static boolean isSnoozed(Context context, int alarmId) {
        long until = getSnoozedUntil(context, alarmId);
        if (until == 0) return false;
        if (System.currentTimeMillis() > until) {
            clearSnoozed(context, alarmId);
            return false;
        }
        return true;
    }

    public static void clearSnoozed(Context context, int alarmId) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove(PREF_SNOOZE_PREFIX + alarmId).apply();
        cancelSnoozedNotification(context, alarmId);
        cancelSnoozeAlarm(context, alarmId);
    }

    public static void cancelSnoozeAlarm(Context context, int alarmId) {
        android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, org.matiasdesu.thinklauncherv2.receivers.AlarmReceiver.class);
        intent.putExtra("alarm_id", alarmId);
        intent.putExtra("is_snooze", true);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, alarmId + 500000, intent, flags);
        am.cancel(pi);
        pi.cancel();
    }

    public static String formatSnoozedUntil(Context context, long untilMillis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(untilMillis));
    }

    private static void ensureSnoozeChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (nm.getNotificationChannel(SNOOZE_CHANNEL_ID) != null) return;
            android.app.NotificationChannel ch = new android.app.NotificationChannel(SNOOZE_CHANNEL_ID, SNOOZE_CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Snoozed alarms");
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        }
    }

    private static void showSnoozedNotification(Context context, int alarmId, long untilMillis) {
        ensureSnoozeChannel(context);
        Alarm alarm = getById(context, alarmId);
        String time = formatSnoozedUntil(context, untilMillis);
        String title = "Snoozed until " + time;
        String text = alarm != null ? alarm.getTimeText() : "";
        if (alarm != null && alarm.label != null && !alarm.label.trim().isEmpty()) text = text + " " + alarm.label;
        Intent cancelIntent = new Intent(context, org.matiasdesu.thinklauncherv2.receivers.CancelSnoozeReceiver.class);
        cancelIntent.putExtra("alarm_id", alarmId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent cancelPi = PendingIntent.getBroadcast(context, alarmId + 600000, cancelIntent, flags);
        androidx.core.app.NotificationCompat.Builder b = new androidx.core.app.NotificationCompat.Builder(context, SNOOZE_CHANNEL_ID)
                .setSmallIcon(org.matiasdesu.thinklauncherv2.R.drawable.time)
                .setContentTitle(title)
                .setContentText(text.isEmpty() ? "Tap to cancel snooze" : text)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(0, "Cancel snooze", cancelPi);
        android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(alarmId + 700000, b.build());
    }

    private static void cancelSnoozedNotification(Context context, int alarmId) {
        android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(alarmId + 700000);
    }

    private static PendingIntent getPendingIntent(Context context, int alarmId) {
        Intent intent = new Intent(context, org.matiasdesu.thinklauncherv2.receivers.AlarmReceiver.class);
        intent.putExtra("alarm_id", alarmId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, alarmId, intent, flags);
    }

    public static long computeNextTriggerMillis(Alarm alarm) {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        next.set(Calendar.HOUR_OF_DAY, alarm.hour);
        next.set(Calendar.MINUTE, alarm.minute);

        if (!alarm.hasRepeat()) {
            if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1);
            return next.getTimeInMillis();
        }

        for (int offset = 0; offset < 8; offset++) {
            Calendar c = (Calendar) next.clone();
            c.add(Calendar.DAY_OF_YEAR, offset);
            int dow = c.get(Calendar.DAY_OF_WEEK);
            int idx = dowToIdx(dow);
            if (alarm.days[idx]) {
                if (offset == 0 && !c.after(now)) continue;
                c.set(Calendar.HOUR_OF_DAY, alarm.hour);
                c.set(Calendar.MINUTE, alarm.minute);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                return c.getTimeInMillis();
            }
        }
        return next.getTimeInMillis();
    }

    private static int dowToIdx(int dow) {

        switch (dow) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return 0;
        }
    }
}