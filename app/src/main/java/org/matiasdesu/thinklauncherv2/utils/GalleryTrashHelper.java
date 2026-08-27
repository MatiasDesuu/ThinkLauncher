package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class GalleryTrashHelper {

    private static final String PREFS_NAME = "prefs";
    private static final String KEY_TRASH = "gallery_trash_ids";

    private GalleryTrashHelper() {
    }

    public static Set<String> getTrashedIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_TRASH, null);
        if (set == null) return new HashSet<>();
        return new HashSet<>(set);
    }

    public static boolean isTrashed(Context context, long id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_TRASH, null);
        if (set == null) return false;
        return set.contains(String.valueOf(id));
    }

    public static void moveToTrash(Context context, long id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> current = prefs.getStringSet(KEY_TRASH, null);
        Set<String> updated = current == null ? new HashSet<>() : new HashSet<>(current);
        updated.add(String.valueOf(id));
        prefs.edit().putStringSet(KEY_TRASH, updated).apply();
    }

    public static void restore(Context context, long id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> current = prefs.getStringSet(KEY_TRASH, null);
        if (current == null) return;
        Set<String> updated = new HashSet<>(current);
        if (updated.remove(String.valueOf(id))) {
            prefs.edit().putStringSet(KEY_TRASH, updated).apply();
        }
    }

    public static void removeFromTrash(Context context, long id) {
        restore(context, id);
    }

    public static int getTrashCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_TRASH, null);
        return set == null ? 0 : set.size();
    }

    public static void pruneInvalidIds(Context context, Set<Long> existingIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> current = prefs.getStringSet(KEY_TRASH, null);
        if (current == null || current.isEmpty()) return;
        Set<String> existingStrings = new HashSet<>();
        for (Long id : existingIds) existingStrings.add(String.valueOf(id));
        Set<String> pruned = new HashSet<>();
        for (String s : current) if (existingStrings.contains(s)) pruned.add(s);
        if (pruned.size() != current.size()) {
            prefs.edit().putStringSet(KEY_TRASH, pruned).apply();
        }
    }
}
