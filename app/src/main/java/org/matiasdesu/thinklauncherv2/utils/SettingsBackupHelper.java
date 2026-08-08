package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Exports and imports the launcher configuration (SharedPreferences "prefs")
 * to/from a JSON file. The export uses the Storage Access Framework so no
 * storage permission is required.
 */
public class SettingsBackupHelper {

    private static final String PREFS_NAME = "prefs";
    private static final String KEY_VERSION = "version";
    private static final String KEY_SETTINGS = "settings";
    private static final int BACKUP_VERSION = 2;

    private static final String TYPE_STRING = "s";
    private static final String TYPE_INT = "i";
    private static final String TYPE_LONG = "l";
    private static final String TYPE_FLOAT = "f";
    private static final String TYPE_BOOL = "b";
    private static final String TYPE_ARRAY = "a";

    /**
     * Keys read with {@code getFloat} by the app. JSON exports whole-number
     * floats as integers, so these must always be repaired back to float on
     * import (and after legacy imports that stored them as ints).
     */
    private static final String[] FLOAT_KEYS = {
            "wallpaper_offset_x",
            "wallpaper_offset_y",
            "wallpaper_scale",
            "device_density",
            "device_scaled_density"
    };

    private SettingsBackupHelper() {
    }

    /**
     * Write the out-of-the-box defaults (theme light, settings and search
     * buttons visible, swipe down = notification panel, swipe up = app
     * launcher). Only writes keys that are not already set, so it is safe to
     * call both on first launch and after a full configuration reset.
     */
    public static void applyInitialDefaults(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = null;
        if (!prefs.contains("theme")) {
            if (editor == null) editor = prefs.edit();
            editor.putInt("theme", 0);
        }
        if (!prefs.contains("show_settings_button")) {
            if (editor == null) editor = prefs.edit();
            editor.putInt("show_settings_button", 1);
        }
        if (!prefs.contains("show_search_button")) {
            if (editor == null) editor = prefs.edit();
            editor.putInt("show_search_button", 1);
        }
        if (!prefs.contains("swipe_down_app")) {
            if (editor == null) editor = prefs.edit();
            editor.putString("swipe_down_app", "notification_panel");
        }
        if (!prefs.contains("swipe_up_app")) {
            if (editor == null) editor = prefs.edit();
            editor.putString("swipe_up_app", "app_launcher");
        }
        if (editor != null) {
            editor.apply();
        }
    }

    /**
     * Fix any float keys that were stored as int/long (can happen with legacy
     * v1 backups). Safe to call at app startup.
     */
    public static void repairFloatKeys(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = null;
        Map<String, ?> all = prefs.getAll();
        for (String key : FLOAT_KEYS) {
            Object value = all.get(key);
            if (value instanceof Integer) {
                if (editor == null) editor = prefs.edit();
                editor.putFloat(key, ((Integer) value).floatValue());
            } else if (value instanceof Long) {
                if (editor == null) editor = prefs.edit();
                editor.putFloat(key, ((Long) value).floatValue());
            }
        }
        if (editor != null) {
            editor.apply();
        }
    }

    /**
     * Serialize all launcher preferences into a JSON string. Every value is
     * wrapped with an explicit type tag so floats round-trip exactly.
     */
    public static String exportToJson(Context context) throws JSONException {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject settings = new JSONObject();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            JSONObject typed = new JSONObject();
            if (value instanceof String) {
                typed.put("t", TYPE_STRING);
                typed.put("v", (String) value);
            } else if (value instanceof Integer) {
                typed.put("t", TYPE_INT);
                typed.put("v", (Integer) value);
            } else if (value instanceof Long) {
                typed.put("t", TYPE_LONG);
                typed.put("v", (Long) value);
            } else if (value instanceof Float) {
                typed.put("t", TYPE_FLOAT);
                typed.put("v", (Float) value);
            } else if (value instanceof Boolean) {
                typed.put("t", TYPE_BOOL);
                typed.put("v", (Boolean) value);
            } else if (value instanceof Set) {
                JSONArray array = new JSONArray();
                for (Object item : (Set<?>) value) {
                    array.put(String.valueOf(item));
                }
                typed.put("t", TYPE_ARRAY);
                typed.put("v", array);
            } else {
                continue;
            }
            settings.put(entry.getKey(), typed);
        }
        JSONObject root = new JSONObject();
        root.put(KEY_VERSION, BACKUP_VERSION);
        root.put(KEY_SETTINGS, settings);
        return root.toString();
    }

    /**
     * Apply all settings from a JSON string produced by {@link #exportToJson}.
     * Clears the current preferences first, then writes the imported values.
     * Accepts both the typed (v2) and the legacy untyped (v1) format.
     */
    public static void importFromJson(Context context, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject settings = root.optJSONObject(KEY_SETTINGS);
        if (settings == null) {
            throw new JSONException("Invalid backup file: missing settings object");
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        Iterator<String> keys = settings.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = settings.get(key);
            if (value instanceof JSONObject) {
                JSONObject typed = (JSONObject) value;
                String type = typed.optString("t", "");
                switch (type) {
                    case TYPE_STRING:
                        editor.putString(key, typed.getString("v"));
                        break;
                    case TYPE_INT:
                        editor.putInt(key, typed.getInt("v"));
                        break;
                    case TYPE_LONG:
                        editor.putLong(key, typed.getLong("v"));
                        break;
                    case TYPE_FLOAT:
                        editor.putFloat(key, (float) typed.getDouble("v"));
                        break;
                    case TYPE_BOOL:
                        editor.putBoolean(key, typed.getBoolean("v"));
                        break;
                    case TYPE_ARRAY: {
                        JSONArray array = typed.getJSONArray("v");
                        Set<String> set = new HashSet<>();
                        for (int i = 0; i < array.length(); i++) {
                            set.add(array.getString(i));
                        }
                        editor.putStringSet(key, set);
                        break;
                    }
                    default:
                        break;
                }
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Double) {
                editor.putFloat(key, ((Double) value).floatValue());
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                Set<String> set = new HashSet<>();
                for (int i = 0; i < array.length(); i++) {
                    set.add(array.getString(i));
                }
                editor.putStringSet(key, set);
            }
        }
        editor.apply();
        repairFloatKeys(context);
    }
}
