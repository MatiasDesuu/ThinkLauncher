package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryTrashHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GalleryDetailsDialog extends GuardedDialog {

    private final long imageId;
    private final int mediaType;
    private final String fallbackName;
    private final long fallbackDate;
    private final long fallbackSize;
    private final String fallbackPath;

    public GalleryDetailsDialog(Context context, long imageId, int mediaType, String fallbackName, long fallbackDate, long fallbackSize, String fallbackPath) {
        super(context, R.style.NoAnimationDialog);
        this.imageId = imageId;
        this.mediaType = mediaType;
        this.fallbackName = fallbackName;
        this.fallbackDate = fallbackDate;
        this.fallbackSize = fallbackSize;
        this.fallbackPath = fallbackPath;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_gallery_details);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        android.view.View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView nameView = findViewById(R.id.detail_name);
        TextView pathView = findViewById(R.id.detail_path);
        TextView dateView = findViewById(R.id.detail_date);
        TextView sizeView = findViewById(R.id.detail_size);
        TextView resView = findViewById(R.id.detail_resolution);
        TextView closeButton = findViewById(R.id.close_button);
        DialogEffectHelper.applyButtonTheme(closeButton, theme, getContext(), surfaceColor);
        closeButton.setOnClickListener(v -> dismiss());

        String name = fallbackName;
        long date = fallbackDate;
        long size = fallbackSize;
        String path = fallbackPath;
        String resolution = "";
        String durationStr = "";

        try {
            Uri baseUri = mediaType == GalleryTrashHelper.TYPE_VIDEO ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri uri = ContentUris.withAppendedId(baseUri, imageId);
            String[] proj;
            if (mediaType == GalleryTrashHelper.TYPE_VIDEO) {
                proj = new String[]{MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DATA, MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.HEIGHT, MediaStore.Video.Media.DURATION};
            } else {
                proj = new String[]{MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATA, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT};
            }
            try (Cursor c = getContext().getContentResolver().query(uri, proj, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    if (!c.isNull(0)) name = c.getString(0);
                    if (!c.isNull(1)) date = c.getLong(1);
                    if (!c.isNull(2)) size = c.getLong(2);
                    if (!c.isNull(3)) path = c.getString(3);
                    if (!c.isNull(4) && !c.isNull(5)) {
                        int w = c.getInt(4);
                        int h = c.getInt(5);
                        if (w > 0 && h > 0) resolution = w + " x " + h;
                    }
                    if (mediaType == GalleryTrashHelper.TYPE_VIDEO && c.getColumnCount() > 6 && !c.isNull(6)) {
                        long dur = c.getLong(6);
                        if (dur > 0) {
                            long sec = dur / 1000;
                            long m = sec / 60;
                            long s = sec % 60;
                            durationStr = String.format(Locale.getDefault(), "%d:%02d", m, s);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        nameView.setText("Name: " + name);
        ThemeUtils.applyTextColor(nameView, theme, getContext());
        if (path != null && !path.isEmpty()) {
            String loc = path;
            try { loc = new File(path).getParent(); if (loc == null) loc = path; } catch (Exception e) { loc = path; }
            pathView.setText("Location: " + loc);
            ThemeUtils.applyTextColor(pathView, theme, getContext());
            pathView.setVisibility(android.view.View.VISIBLE);
        } else {
            pathView.setVisibility(android.view.View.GONE);
        }
        SimpleDateFormat df = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());
        dateView.setText("Date: " + df.format(new Date(date * 1000)));
        ThemeUtils.applyTextColor(dateView, theme, getContext());
        sizeView.setText("Size: " + formatSize(size));
        ThemeUtils.applyTextColor(sizeView, theme, getContext());
        if (!resolution.isEmpty() || !durationStr.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (!resolution.isEmpty()) sb.append("Resolution: ").append(resolution);
            if (!durationStr.isEmpty()) {
                if (sb.length() > 0) sb.append("  ");
                else sb.append("Duration: ");
                if (resolution.isEmpty()) sb = new StringBuilder("Duration: " + durationStr);
                else sb.append(durationStr);
            }
            resView.setText(sb.toString());
            ThemeUtils.applyTextColor(resView, theme, getContext());
            resView.setVisibility(android.view.View.VISIBLE);
        } else {
            resView.setVisibility(android.view.View.GONE);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f));
        return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024f * 1024f * 1024f));
    }
}
