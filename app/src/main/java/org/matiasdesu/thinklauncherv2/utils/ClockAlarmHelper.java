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

    public static class Alarm {
        public int id;
        public int hour; // 0-23
        public int minute; // 0-59
        public boolean[] days = new boolean[7]; // 0=Mon ... 6=Sun (Calendar MONDAY=2)
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
            // weekdays Mon-Fri = 0..4
            for (int i = 0; i < 5; i++) if (!days[i]) weekdays = false;
            for (int i = 5; i < 7; i++) if (days[i]) weekdays = false;
            // weekend Sat Sun = 5,6
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
    }

    public static void setEnabled(Context context, int id, boolean enabled) {
        List<Alarm> list = loadAlarms(context);
        for (Alarm a : list) {
            if (a.id == id) {
                a.enabled = enabled;
                saveAlarms(context, list);
                if (enabled) schedule(context, a);
                else cancel(context, id);
                break;
            }
        }
    }

    public static Alarm getById(Context context, int id) {
        for (Alarm a : loadAlarms(context)) if (a.id == id) return a;
        return null;
    }

    // Scheduling

    public static void schedule(Context context, Alarm alarm) {
        if (!alarm.enabled) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long triggerAt = computeNextTriggerMillis(alarm);
        PendingIntent pi = getPendingIntent(context, alarm.id);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!am.canScheduleExactAlarms()) {
                    // fallback to inexact
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    return;
                }
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
    }

    public static void rescheduleAfterFired(Context context, int alarmId) {
        Alarm a = getById(context, alarmId);
        if (a == null || !a.enabled) return;
        if (a.hasRepeat()) {
            schedule(context, a);
        } else {
            // one-shot: disable after firing
            a.enabled = false;
            List<Alarm> list = loadAlarms(context);
            for (int i = 0; i < list.size(); i++) if (list.get(i).id == alarmId) { list.set(i, a); break; }
            saveAlarms(context, list);
        }
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
        // repeat: find next matching weekday
        // alarm days 0=Mon ... 6=Sun maps to Calendar MONDAY=2 ... SUNDAY=1
        for (int offset = 0; offset < 8; offset++) {
            Calendar c = (Calendar) next.clone();
            c.add(Calendar.DAY_OF_YEAR, offset);
            int dow = c.get(Calendar.DAY_OF_WEEK); // 1 Sun .. 7 Sat
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
        // Mon 0, Tue1, Wed2, Thu3, Fri4, Sat5, Sun6
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
