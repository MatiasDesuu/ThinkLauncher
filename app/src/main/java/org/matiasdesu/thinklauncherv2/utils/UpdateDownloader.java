package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateDownloader {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    public interface ProgressCallback {
        void onProgress(int percent, long downloaded, long total);
        void onSuccess(File apkFile);
        void onError(String error);
    }

    public static void download(Context ctx, String url, ProgressCallback cb) {
        executor.execute(() -> {
            File outFile = null;
            try {
                outFile = getApkFile(ctx);
                // ensure parent exists
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                // delete old file if exists
                if (outFile.exists()) outFile.delete();

                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("User-Agent", "ThinkLauncher-Updater");
                conn.setInstanceFollowRedirects(true);
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    String err = "HTTP " + code;
                    main.post(() -> cb.onError(err));
                    return;
                }
                long total = conn.getContentLengthLong();
                if (total <= 0) total = -1;
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(outFile);
                byte[] buf = new byte[8192];
                long downloaded = 0;
                int read;
                int lastPercent = -1;
                while ((read = is.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                    downloaded += read;
                    if (total > 0) {
                        int percent = (int) ((downloaded * 100) / total);
                        if (percent != lastPercent && percent % 1 == 0) {
                            int p = percent;
                            long d = downloaded;
                            long t = total;
                            main.post(() -> cb.onProgress(p, d, t));
                            lastPercent = percent;
                        }
                    } else {
                        long d = downloaded;
                        main.post(() -> cb.onProgress(-1, d, -1));
                    }
                }
                fos.flush();
                fos.close();
                is.close();
                conn.disconnect();
                File f = outFile;
                main.post(() -> cb.onSuccess(f));
            } catch (Exception e) {
                if (outFile != null && outFile.exists()) {
                    // keep partial? delete
                    // outFile.delete();
                }
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                main.post(() -> cb.onError(msg));
            }
        });
    }

    public static File getApkFile(Context ctx) {
        File dir = new File(ctx.getExternalFilesDir(null), "updates");
        if (!dir.exists()) dir.mkdirs();
        // also try cache as fallback
        return new File(dir, "ThinkLauncher-update.apk");
    }

    public static void install(Context ctx, File apkFile) {
        try {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String authority = ctx.getPackageName() + ".fileprovider";
                uri = FileProvider.getUriForFile(ctx, authority, apkFile);
            } else {
                uri = Uri.fromFile(apkFile);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(intent);
        } catch (Exception e) {
            // fallback to view
            Uri uri = Uri.fromFile(apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        }
    }

    public static boolean canInstallPackages(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ctx.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    public static void requestInstallPermission(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            }
        }
    }
}
