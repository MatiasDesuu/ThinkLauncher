package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryHiddenHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryTrashHelper;

public class GalleryViewerOptionsDialog extends GuardedDialog {

    public interface OnFavoriteCallback {
        void onFavorite();
    }

    public interface OnHiddenCallback {
        void onHidden();
    }

    public interface OnTrashCallback {
        void onTrash();
    }

    public interface OnWallpaperCallback {
        void onWallpaper();
    }

    private final long imageId;
    private final int mediaType;
    private final boolean isTrashMode;
    private final boolean isHiddenMode;
    private final OnFavoriteCallback favCallback;
    private final OnHiddenCallback hiddenCallback;
    private final OnTrashCallback trashCallback;
    private final OnWallpaperCallback wallpaperCallback;

    public GalleryViewerOptionsDialog(Context context, long imageId, int mediaType, boolean isTrashMode, boolean isHiddenMode,
                                       OnFavoriteCallback favCallback, OnHiddenCallback hiddenCallback, OnTrashCallback trashCallback) {
        this(context, imageId, mediaType, isTrashMode, isHiddenMode, favCallback, hiddenCallback, trashCallback, null);
    }

    public GalleryViewerOptionsDialog(Context context, long imageId, int mediaType, boolean isTrashMode, boolean isHiddenMode,
                                       OnFavoriteCallback favCallback, OnHiddenCallback hiddenCallback, OnTrashCallback trashCallback, OnWallpaperCallback wallpaperCallback) {
        super(context, R.style.NoAnimationDialog);
        this.imageId = imageId;
        this.mediaType = mediaType;
        this.isTrashMode = isTrashMode;
        this.isHiddenMode = isHiddenMode;
        this.favCallback = favCallback;
        this.hiddenCallback = hiddenCallback;
        this.trashCallback = trashCallback;
        this.wallpaperCallback = wallpaperCallback;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_gallery_viewer_options);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView favButton = findViewById(R.id.option_favorite);
        TextView hiddenButton = findViewById(R.id.option_hidden);
        TextView trashButton = findViewById(R.id.option_trash);
        TextView wallpaperButton = findViewById(R.id.option_wallpaper);

        DialogEffectHelper.applyButtonTheme(favButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(hiddenButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(trashButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(wallpaperButton, theme, getContext(), surfaceColor);

        boolean isFav = isFavorite();
        boolean isHidden = GalleryHiddenHelper.isHidden(getContext(), imageId, mediaType);

        if (isTrashMode) {
            favButton.setText("Restore");
            hiddenButton.setVisibility(View.GONE);
            trashButton.setText("Delete permanently");
        } else if (isHiddenMode) {
            favButton.setText(isFav ? "Unfavorite" : "Favorite");
            hiddenButton.setText("Unhide");
            trashButton.setText("Move to trash");
        } else {
            favButton.setText(isFav ? "Unfavorite" : "Favorite");
            hiddenButton.setText(isHidden ? "Unhide" : "Hide");
            trashButton.setText("Move to trash");
        }

        boolean showWallpaper = !isTrashMode && mediaType != GalleryTrashHelper.TYPE_VIDEO && wallpaperCallback != null;
        wallpaperButton.setVisibility(showWallpaper ? View.VISIBLE : View.GONE);

        favButton.setOnClickListener(v -> {
            dismiss();
            if (favCallback != null) favCallback.onFavorite();
        });
        hiddenButton.setOnClickListener(v -> {
            dismiss();
            if (hiddenCallback != null) hiddenCallback.onHidden();
        });
        trashButton.setOnClickListener(v -> {
            dismiss();
            if (trashCallback != null) trashCallback.onTrash();
        });
        wallpaperButton.setOnClickListener(v -> {
            dismiss();
            if (wallpaperCallback != null) wallpaperCallback.onWallpaper();
        });
    }

    private boolean isFavorite() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        java.util.Set<String> s = prefs.getStringSet("gallery_favorite_ids", null);
        if (s == null) return false;
        return s.contains("image:" + imageId) || s.contains("video:" + imageId) || s.contains(String.valueOf(imageId));
    }
}
