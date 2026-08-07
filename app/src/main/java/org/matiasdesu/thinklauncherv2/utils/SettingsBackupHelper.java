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
    private static final int BACKUP_VERSION = 1;

    private SettingsBackupHelper() {
    }

    /**
     * Serialize all launcher preferences into a JSON string.
     */
    public static String exportToJson(Context context) throws JSONException {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject settings = new JSONObject();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                settings.put(entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                settings.put(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                settings.put(entry.getKey(), (Long) value);
            } else if (value instanceof Float) {
                settings.put(entry.getKey(), (Float) value);
            } else if (value instanceof Boolean) {
                settings.put(entry.getKey(), (Boolean) value);
            } else if (value instanceof Set) {
                JSONArray array = new JSONArray();
                for (Object item : (Set<?>) value) {
                    array.put(String.valueOf(item));
                }
                settings.put(entry.getKey(), array);
            }
        }
        JSONObject root = new JSONObject();
        root.put(KEY_VERSION, BACKUP_VERSION);
        root.put(KEY_SETTINGS, settings);
        return root.toString();
    }

    /**
     * Apply all settings from a JSON string produced by {@link #exportToJson}.
     * Clears the current preferences first, then writes the imported values.
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
            if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Double) {
                editor.putFloat(key, ((Double) value).floatValue());
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
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
    }
}
