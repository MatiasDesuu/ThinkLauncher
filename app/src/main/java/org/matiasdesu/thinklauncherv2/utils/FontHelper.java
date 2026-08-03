package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FontHelper {

    private static final String FONT_PREF = "custom_font_file";
    private static final String FONT_PREFIX = "custom_font_";
    private static final String FONT_SUFFIX = ".ttf";

    private static volatile Typeface cachedTypeface = null;

    public static boolean hasCustomFont(Context context) {
        return getCurrentFontFile(context) != null;
    }

    private static File getCurrentFontFile(Context context) {
        String fileName = context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString(FONT_PREF, null);
        if (fileName == null) {
            return null;
        }
        File file = new File(context.getFilesDir(), fileName);
        return file.exists() ? file : null;
    }

    public static Typeface getTypeface(Context context) {
        File file = getCurrentFontFile(context);
        if (file == null) {
            return null;
        }
        String key = file.getAbsolutePath() + "@" + file.lastModified();
        if (cachedTypeface == null || !key.equals(lastCacheKey)) {
            try {
                cachedTypeface = Typeface.createFromFile(file);
                lastCacheKey = key;
            } catch (Exception e) {
                return null;
            }
        }
        return cachedTypeface;
    }

    public static void clearCache() {
        cachedTypeface = null;
        lastCacheKey = null;
    }

    public static boolean saveFont(Context context, InputStream in) {
        removeFont(context);
        String fileName = FONT_PREFIX + System.currentTimeMillis() + FONT_SUFFIX;
        File file = new File(context.getFilesDir(), fileName);
        FileOutputStream fos = null;
        boolean written = false;
        try {
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            written = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!written) {
            return false;
        }
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(FONT_PREF, fileName).apply();
        clearCache();
        return true;
    }

    public static void removeFont(Context context) {
        File dir = context.getFilesDir();
        File[] files = dir == null ? null : dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(FONT_PREFIX) && f.getName().endsWith(FONT_SUFFIX)) {
                    f.delete();
                }
            }
        }
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().remove(FONT_PREF).apply();
        clearCache();
    }

    public static void applyFont(Context context, TextView tv) {
        if (tv == null) {
            return;
        }
        Typeface current = tv.getTypeface();
        boolean bold = current != null && current.isBold();
        boolean italic = current != null && current.isItalic();
        int style = (bold ? Typeface.BOLD : 0) | (italic ? Typeface.ITALIC : 0);
        Typeface custom = getTypeface(context);
        if (custom == null) {
            tv.setTypeface(null, style);
        } else {
            tv.setTypeface(Typeface.create(custom, style));
        }
    }

    public static void applyToViewTree(Context context, View view) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            applyFont(context, (TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyToViewTree(context, vg.getChildAt(i));
            }
        }
    }

    private static volatile String lastCacheKey = null;
}