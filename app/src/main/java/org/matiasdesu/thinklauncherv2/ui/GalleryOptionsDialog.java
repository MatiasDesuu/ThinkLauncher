package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class GalleryOptionsDialog extends GuardedDialog {

    public interface OnTrashClickCallback {
        void onTrashClick();
    }

    public interface OnFavoritesClickCallback {
        void onFavoritesClick();
    }

    public interface OnHiddenClickCallback {
        void onHiddenClick();
    }

    public interface OnEmptyTrashCallback {
        void onEmptyTrash();
    }

    public interface OnFilterChangedCallback {
        void onFilterChanged(int filterMode);
    }

    public interface OnSortChangedCallback {
        void onSortChanged(int sortMode);
    }

    public interface OnMoreOptionsClickCallback {
        void onMoreOptionsClick();
    }

    private OnTrashClickCallback trashCallback;
    private OnFavoritesClickCallback favoritesCallback;
    private OnHiddenClickCallback hiddenCallback;
    private OnEmptyTrashCallback emptyTrashCallback;
    private OnFilterChangedCallback filterCallback;
    private OnSortChangedCallback sortCallback;
    private OnMoreOptionsClickCallback moreOptionsCallback;
    private boolean isTrashMode;
    private boolean isFavoritesMode;
    private boolean isHiddenMode;
    private int currentFilterMode;
    private int currentSortMode;

    public GalleryOptionsDialog(Context context, boolean isTrashMode, boolean isFavoritesMode, boolean isHiddenMode,
                                int filterMode, OnFilterChangedCallback filterCallback,
                                int sortMode, OnSortChangedCallback sortCallback,
                                OnTrashClickCallback trashCallback,
                                OnFavoritesClickCallback favoritesCallback,
                                OnHiddenClickCallback hiddenCallback,
                                OnEmptyTrashCallback emptyTrashCallback,
                                OnMoreOptionsClickCallback moreOptionsCallback) {
        super(context, R.style.NoAnimationDialog);
        this.trashCallback = trashCallback;
        this.favoritesCallback = favoritesCallback;
        this.hiddenCallback = hiddenCallback;
        this.emptyTrashCallback = emptyTrashCallback;
        this.filterCallback = filterCallback;
        this.sortCallback = sortCallback;
        this.moreOptionsCallback = moreOptionsCallback;
        this.isTrashMode = isTrashMode;
        this.isFavoritesMode = isFavoritesMode;
        this.isHiddenMode = isHiddenMode;
        this.currentFilterMode = filterMode;
        this.currentSortMode = sortMode;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_gallery_options);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView filterButton = findViewById(R.id.filter_button);
        if (filterButton != null) {
            DialogEffectHelper.applyButtonTheme(filterButton, theme, getContext(), surfaceColor);
            updateFilterText(filterButton);
            filterButton.setOnClickListener(v -> {
                currentFilterMode = (currentFilterMode + 1) % 3;
                getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("gallery_filter_by", currentFilterMode).apply();
                updateFilterText(filterButton);
                if (filterCallback != null) filterCallback.onFilterChanged(currentFilterMode);
            });
        }

        View sortButton = findViewById(R.id.sort_button);
        TextView sortLabel = findViewById(R.id.sort_label);
        ImageView sortArrow = findViewById(R.id.sort_arrow);
        if (sortButton != null) {
            DialogEffectHelper.applySurface(sortButton, theme, getContext(), surfaceColor);
            int p = (int) (4 * getContext().getResources().getDisplayMetrics().density);
            sortButton.setPadding(p, p, p, p);
            if (sortLabel != null) sortLabel.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
            if (sortArrow != null) sortArrow.setColorFilter(ThemeUtils.getTextColor(theme, getContext()));
            updateSortText(sortLabel, sortArrow);
            sortButton.setOnClickListener(v -> {
                currentSortMode = (currentSortMode + 1) % 4;
                getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("gallery_sort_by", currentSortMode).apply();
                updateSortText(sortLabel, sortArrow);
                if (sortCallback != null) sortCallback.onSortChanged(currentSortMode);
            });
        }

        TextView trashButton = findViewById(R.id.trash_button);
        if (trashButton != null) {
            DialogEffectHelper.applyButtonTheme(trashButton, theme, getContext(), surfaceColor);
            trashButton.setText(isTrashMode ? "Gallery" : "Trash");
            trashButton.setOnClickListener(v -> {
                dismiss();
                trashCallback.onTrashClick();
            });
        }

        TextView favoritesButton = findViewById(R.id.favorites_button);
        if (favoritesButton != null) {
            DialogEffectHelper.applyButtonTheme(favoritesButton, theme, getContext(), surfaceColor);
            favoritesButton.setText(isFavoritesMode ? "Gallery" : "Favorites");
            favoritesButton.setOnClickListener(v -> {
                dismiss();
                if (favoritesCallback != null) favoritesCallback.onFavoritesClick();
            });
        }

        TextView hiddenButton = findViewById(R.id.hidden_button);
        if (hiddenButton != null) {
            DialogEffectHelper.applyButtonTheme(hiddenButton, theme, getContext(), surfaceColor);
            hiddenButton.setText(isHiddenMode ? "Gallery" : "Hidden");
            hiddenButton.setOnClickListener(v -> {
                dismiss();
                if (hiddenCallback != null) hiddenCallback.onHiddenClick();
            });
        }

        TextView emptyTrashButton = findViewById(R.id.empty_trash_button);
        if (emptyTrashButton != null) {
            DialogEffectHelper.applyButtonTheme(emptyTrashButton, theme, getContext(), surfaceColor);
            if (isTrashMode && emptyTrashCallback != null) {
                emptyTrashButton.setVisibility(View.VISIBLE);
                emptyTrashButton.setOnClickListener(v -> {
                    dismiss();
                    emptyTrashCallback.onEmptyTrash();
                });
            } else {
                emptyTrashButton.setVisibility(View.GONE);
            }
        }

        TextView moreButton = findViewById(R.id.more_options_button);
        if (moreButton != null) {
            DialogEffectHelper.applyButtonTheme(moreButton, theme, getContext(), surfaceColor);
            moreButton.setOnClickListener(v -> {
                dismiss();
                if (moreOptionsCallback != null) moreOptionsCallback.onMoreOptionsClick();
            });
        }
    }

    private void updateFilterText(TextView btn) {
        String label;
        if (currentFilterMode == 1) label = "Images";
        else if (currentFilterMode == 2) label = "Videos";
        else label = "All";
        btn.setText("Filter: " + label);
    }

    private void updateSortText(TextView labelView, ImageView arrowView) {
        String label;
        int rotation;
        if (currentSortMode == 1) { label = "Date"; rotation = 90; }
        else if (currentSortMode == 2) { label = "Size"; rotation = 270; }
        else if (currentSortMode == 3) { label = "Size"; rotation = 90; }
        else { label = "Date"; rotation = 270; }
        if (labelView != null) labelView.setText("Sort: " + label);
        if (arrowView != null) arrowView.setRotation(rotation);
    }
}
