package org.matiasdesu.thinklauncherv2.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KOReaderHistoryHelper {
    private static final String TAG = "KOReaderHistoryHelper";

    public static class BookItem {
        public String title;
        public String author;
        public String path;
        public long lastOpen;

        public BookItem(String title, String author, String path, long lastOpen) {
            this.title = title;
            this.author = author;
            this.path = path;
            this.lastOpen = lastOpen;
        }
    }

    public static List<BookItem> getRecentBooks(String customPath) {
        List<BookItem> books = new ArrayList<>();

        if (customPath == null || customPath.trim().isEmpty()) {
            return books;
        }

        String root = customPath.trim();
        if (root.endsWith("/"))
            root = root.substring(0, root.length() - 1);

        String[] statsPatterns = {
                "settings/statistics.sqlite3",
                "settings/statistics.sqlite",
                "statistics.sqlite3",
                "statistics.sqlite",
                ".koreader/statistics.sqlite3",
                ".koreader/statistics.sqlite"
        };

        String[] historyPatterns = {
                "history.sqlite",
                "settings/history.sqlite",
                ".koreader/history.sqlite"
        };

        for (String pattern : statsPatterns) {
            File statsDb = new File(root, pattern);
            if (statsDb.exists()) {

                File infoDb = null;
                File dir = statsDb.getParentFile();
                if (dir != null) {
                    File f1 = new File(dir, "bookinfo.sqlite3");
                    File f2 = new File(dir, "bookinfo.sqlite");
                    File f3 = new File(dir, "bookinfo_cache.sqlite3");
                    if (f1.exists()) {
                        infoDb = f1;
                    } else if (f2.exists()) {
                        infoDb = f2;
                    } else if (f3.exists()) {
                        infoDb = f3;
                    }
                }

                books = fetchFromStatistics(statsDb, infoDb);
                if (!books.isEmpty())
                    return books;
            }
        }

        for (String pattern : historyPatterns) {
            File historyDb = new File(root, pattern);
            if (historyDb.exists()) {
                books = fetchFromHistoryDb(historyDb);
                if (!books.isEmpty())
                    return books;
            }
        }

        return books;
    }

    private static List<BookItem> fetchFromStatistics(File statsDbFile, File infoDbFile) {
        List<BookItem> books = new ArrayList<>();
        SQLiteDatabase statsDb = null;
        SQLiteDatabase infoDb = null;
        try {
            statsDb = SQLiteDatabase.openDatabase(statsDbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

            String query = "SELECT id, title, authors, last_open FROM book ORDER BY last_open DESC LIMIT 50";
            Cursor cursor = statsDb.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                if (infoDbFile != null && infoDbFile.exists()) {
                    infoDb = SQLiteDatabase.openDatabase(infoDbFile.getAbsolutePath(), null,
                            SQLiteDatabase.OPEN_READONLY);
                }

                do {
                    String title = cursor.getString(1);
                    String authors = cursor.getString(2);
                    long lastOpen = cursor.getLong(3);
                    String path = null;

                    if (infoDb != null) {
                        try {
                            Cursor infoCursor = infoDb.rawQuery(
                                    "SELECT directory, filename FROM bookinfo WHERE title = ? AND authors = ?",
                                    new String[] { title, authors });
                            if (infoCursor.moveToFirst()) {
                                path = infoCursor.getString(0) + "/" + infoCursor.getString(1);
                                if (path.startsWith("/storage/emulated/0/")) {

                                } else if (path.startsWith("/sdcard/")) {
                                    path = "/storage/emulated/0/" + path.substring(8);
                                } else if (!path.startsWith("/") && !path.contains("://")) {
                                    path = "/storage/emulated/0/" + path;
                                }
                            }
                            infoCursor.close();
                        } catch (Exception e) {
                            // Table 'bookinfo' not found or error in query
                        }
                    }

                    if (path != null) {
                        books.add(new BookItem(title, authors, path, lastOpen));
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            // Error fetching from statistics
        } finally {
            if (statsDb != null)
                statsDb.close();
            if (infoDb != null)
                infoDb.close();
        }
        return books;
    }

    private static List<BookItem> fetchFromHistoryDb(File historyDbFile) {
        List<BookItem> books = new ArrayList<>();
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(historyDbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery("SELECT path, title, last_open FROM history ORDER BY last_open DESC LIMIT 50",
                    null);
            if (cursor.moveToFirst()) {
                do {
                    String path = cursor.getString(0);
                    String title = cursor.getString(1);
                    long lastOpen = cursor.getLong(2);
                    books.add(new BookItem(title, "", path, lastOpen));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null)
                db.close();
        }
        return books;
    }
}
