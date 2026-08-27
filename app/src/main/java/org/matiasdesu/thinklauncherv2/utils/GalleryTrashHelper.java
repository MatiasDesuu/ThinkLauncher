package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class GalleryTrashHelper {

    private static final String PREFS_NAME = "prefs";
    private static final String KEY_TRASH = "gallery_trash_ids";
    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_VIDEO = 1;

    private GalleryTrashHelper() {
    }

    private static String keyFor(long id, int type) {
        if (type == TYPE_VIDEO) return "video:" + id;
        return "image:" + id;
    }

    private static String legacyKeyFor(long id) {
        return String.valueOf(id);
    }

    public static boolean isTrashed(Context context, long id) {
        return isTrashed(context, id, TYPE_IMAGE);
    }

    public static boolean isTrashed(Context context, long id, int type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_TRASH, null);
        if (set == null) return false;
        if (type == TYPE_VIDEO) return set.contains(keyFor(id, TYPE_VIDEO));
        return set.contains(keyFor(id, TYPE_IMAGE)) || set.contains(legacyKeyFor(id));
    }

    public static void moveToTrash(Context context, long id) {
        moveToTrash(context, id, TYPE_IMAGE);
    }

    public static void moveToTrash(Context context, long id, int type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> current = prefs.getStringSet(KEY_TRASH, null);
        Set<String> updated = current == null ? new HashSet<>() : new HashSet<>(current);
        updated.add(keyFor(id, type));
        if (type == TYPE_IMAGE) updated.remove(legacyKeyFor(id));
        prefs.edit().putStringSet(KEY_TRASH, updated).apply();
    }

    public static void restore(Context context, long id) {
        restore(context, id, TYPE_IMAGE);
    }

    public static void restore(Context context, long id, int type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> current = prefs.getStringSet(KEY_TRASH, null);
        if (current == null) return;
        Set<String> updated = new HashSet<>(current);
        boolean changed = updated.remove(keyFor(id, type));
        if (type == TYPE_IMAGE) changed |= updated.remove(legacyKeyFor(id));
        if (changed) prefs.edit().putStringSet(KEY_TRASH, updated).apply();
    }

    public static void removeFromTrash(Context context, long id) {
        restore(context, id);
    }

    public static void removeFromTrash(Context context, long id, int type) {
        restore(context, id, type);
    }

    public static void pruneInvalidIds(Context context, Set<Long> existingIds) {
        pruneInvalidIds(context, existingIds, new HashSet<>());
    }

    public static void pruneInvalidIds(Context context, Set<Long> existingImageIds, Set<Long> existingVideoIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> current = prefs.getStringSet(KEY_TRASH, null);
        if (current == null || current.isEmpty()) return;
        Set<String> existingStrings = new HashSet<>();
        for (Long id : existingImageIds) {
            existingStrings.add(keyFor(id, TYPE_IMAGE));
            existingStrings.add(legacyKeyFor(id));
        }
        for (Long id : existingVideoIds) existingStrings.add(keyFor(id, TYPE_VIDEO));
        Set<String> pruned = new HashSet<>();
        for (String s : current) if (existingStrings.contains(s)) pruned.add(s);
        if (pruned.size() != current.size()) prefs.edit().putStringSet(KEY_TRASH, pruned).apply();
    }
}
