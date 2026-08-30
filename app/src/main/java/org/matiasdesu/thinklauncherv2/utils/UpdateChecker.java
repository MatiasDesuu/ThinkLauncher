package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateChecker {

    private static final String GITHUB_LATEST_URL = "https://api.github.com/repos/MatiasDesuu/ThinkLauncher/releases/latest";
    private static final int TIMEOUT_MS = 15000;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(UpdateInfo updateOrNull, String error); // updateOrNull==null means no update or error, check error
    }

    public static void checkForUpdate(Context context, Callback cb) {
        executor.execute(() -> {
            String error = null;
            UpdateInfo info = null;
            try {
                URL url = new URL(GITHUB_LATEST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "ThinkLauncher-Updater");
                int code = conn.getResponseCode();
                InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String json = readAll(is);
                if (code < 200 || code >= 300) {
                    error = "GitHub " + code + ": " + json;
                } else {
                    JSONObject obj = new JSONObject(json);
                    String tag = obj.optString("tag_name", "");
                    String name = obj.optString("name", tag);
                    String body = obj.optString("body", "");
                    String htmlUrl = obj.optString("html_url", "");
                    JSONArray assets = obj.optJSONArray("assets");
                    String apkUrl = null;
                    long apkSize = 0;
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject a = assets.getJSONObject(i);
                            String assetName = a.optString("name", "");
                            String download = a.optString("browser_download_url", "");
                            if (assetName.endsWith(".apk") && download != null && !download.isEmpty()) {
                                apkUrl = download;
                                apkSize = a.optLong("size", 0);
                                break;
                            }
                        }
                        // fallback: first asset if no .apk found
                        if (apkUrl == null && assets.length() > 0) {
                            JSONObject a = assets.getJSONObject(0);
                            apkUrl = a.optString("browser_download_url", null);
                            apkSize = a.optLong("size", 0);
                        }
                    }
                    if (tag.isEmpty()) {
                        error = "No tag in release";
                    } else if (apkUrl == null || apkUrl.isEmpty()) {
                        error = "No APK in latest release";
                    } else {
                        String ver = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
                        // strip leading v
                        info = new UpdateInfo(tag, ver, name, body, apkUrl, htmlUrl, apkSize);
                        // check if newer than current
                        String currentVer = getCurrentVersionName(context);
                        if (!info.isNewerThan(currentVer)) {
                            // no update
                            info = null; // signal no update
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : e.toString();
            }
            UpdateInfo fInfo = info;
            String fErr = error;
            main.post(() -> cb.onResult(fInfo, fErr));
        });
    }

    private static String getCurrentVersionName(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        return sb.toString();
    }
}
