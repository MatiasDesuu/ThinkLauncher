package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class GalleryOptionsDialog extends Dialog {

    public interface OnGridChangedCallback {
        void onGridChanged(int columns, int rows);
    }

    public interface OnShowTitlesChangedCallback {
        void onShowTitlesChanged(boolean show);
    }

    public interface OnTrashClickCallback {
        void onTrashClick();
    }

    public interface OnFavoritesClickCallback {
        void onFavoritesClick();
    }

    public interface OnGroupChangedCallback {
        void onGroupChanged(int groupMode);
    }

    public interface OnEmptyTrashCallback {
        void onEmptyTrash();
    }

    public interface OnFilterChangedCallback {
        void onFilterChanged(int filterMode);
    }

    private OnGridChangedCallback gridCallback;
    private OnShowTitlesChangedCallback titlesCallback;
    private OnTrashClickCallback trashCallback;
    private OnFavoritesClickCallback favoritesCallback;
    private OnGroupChangedCallback groupCallback;
    private OnEmptyTrashCallback emptyTrashCallback;
    private OnFilterChangedCallback filterCallback;
    private int currentColumns;
    private int currentRows;
    private boolean currentShowTitles;
    private boolean isGridView;
    private boolean hasPagination;
    private boolean isTrashMode;
    private boolean isFavoritesMode;
    private int currentGroupMode;
    private int currentFilterMode;

    public GalleryOptionsDialog(Context context, int columns, int rows, boolean showTitles,
                                   boolean isGridView, boolean hasPagination, boolean isTrashMode, boolean isFavoritesMode,
                                   int groupMode,
                                   OnGridChangedCallback gridCallback,
                                   OnShowTitlesChangedCallback titlesCallback,
                                   OnGroupChangedCallback groupCallback,
                                   OnTrashClickCallback trashCallback,
                                   OnFavoritesClickCallback favoritesCallback) {
        this(context, columns, rows, showTitles, isGridView, hasPagination, isTrashMode, isFavoritesMode, groupMode, gridCallback, titlesCallback, groupCallback, trashCallback, favoritesCallback, null, 0, null);
    }

    public GalleryOptionsDialog(Context context, int columns, int rows, boolean showTitles,
                                   boolean isGridView, boolean hasPagination, boolean isTrashMode, boolean isFavoritesMode,
                                   int groupMode,
                                   OnGridChangedCallback gridCallback,
                                   OnShowTitlesChangedCallback titlesCallback,
                                   OnGroupChangedCallback groupCallback,
                                   OnTrashClickCallback trashCallback,
                                   OnFavoritesClickCallback favoritesCallback,
                                   OnEmptyTrashCallback emptyTrashCallback) {
        this(context, columns, rows, showTitles, isGridView, hasPagination, isTrashMode, isFavoritesMode, groupMode, gridCallback, titlesCallback, groupCallback, trashCallback, favoritesCallback, emptyTrashCallback, 0, null);
    }

    public GalleryOptionsDialog(Context context, int columns, int rows, boolean showTitles,
                                   boolean isGridView, boolean hasPagination, boolean isTrashMode, boolean isFavoritesMode,
                                   int groupMode,
                                   OnGridChangedCallback gridCallback,
                                   OnShowTitlesChangedCallback titlesCallback,
                                   OnGroupChangedCallback groupCallback,
                                   OnTrashClickCallback trashCallback,
                                   OnFavoritesClickCallback favoritesCallback,
                                   OnEmptyTrashCallback emptyTrashCallback,
                                   int filterMode,
                                   OnFilterChangedCallback filterCallback) {
        super(context, R.style.NoAnimationDialog);
        this.gridCallback = gridCallback;
        this.titlesCallback = titlesCallback;
        this.groupCallback = groupCallback;
        this.trashCallback = trashCallback;
        this.favoritesCallback = favoritesCallback;
        this.emptyTrashCallback = emptyTrashCallback;
        this.filterCallback = filterCallback;
        this.currentColumns = columns;
        this.currentRows = rows;
        this.currentShowTitles = showTitles;
        this.isGridView = isGridView;
        this.hasPagination = hasPagination;
        this.isTrashMode = isTrashMode;
        this.isFavoritesMode = isFavoritesMode;
        this.currentGroupMode = groupMode;
        this.currentFilterMode = filterMode;
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

        TextView colsLabel = findViewById(R.id.gallery_columns_label);
        colsLabel.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        TextView rowsLabel = findViewById(R.id.gallery_rows_label);
        rowsLabel.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        int[] colButtons = {R.id.btn_cols_2, R.id.btn_cols_3, R.id.btn_cols_4, R.id.btn_cols_5, R.id.btn_cols_6};
        int[] colValues = {2, 3, 4, 5, 6};
        for (int i = 0; i < colButtons.length; i++) {
            TextView btn = findViewById(colButtons[i]);
            DialogEffectHelper.applyButtonTheme(btn, theme, getContext(), surfaceColor);
            final int cols = colValues[i];
            if (cols == currentColumns) {
                btn.setTypeface(null, Typeface.BOLD);
            }
            btn.setOnClickListener(v -> {
                currentColumns = cols;
                updateColBold(colButtons, cols);
                if (gridCallback != null) gridCallback.onGridChanged(cols, currentRows);
            });
        }

        int[] rowButtons = {R.id.btn_rows_2, R.id.btn_rows_3, R.id.btn_rows_4, R.id.btn_rows_5, R.id.btn_rows_6};
        int[] rowValues = {2, 3, 4, 5, 6};
        for (int i = 0; i < rowButtons.length; i++) {
            TextView btn = findViewById(rowButtons[i]);
            DialogEffectHelper.applyButtonTheme(btn, theme, getContext(), surfaceColor);
            final int rows = rowValues[i];
            if (rows == currentRows) {
                btn.setTypeface(null, Typeface.BOLD);
            }
            btn.setOnClickListener(v -> {
                currentRows = rows;
                updateRowBold(rowButtons, rows);
                if (gridCallback != null) gridCallback.onGridChanged(currentColumns, rows);
            });
        }

        View rowsContainer = findViewById(R.id.rows_container);
        rowsContainer.setVisibility(hasPagination ? View.VISIBLE : View.GONE);

        TextView showTitlesButton = findViewById(R.id.show_titles_button);
        DialogEffectHelper.applyButtonTheme(showTitlesButton, theme, getContext(), surfaceColor);
        showTitlesButton.setText("Titles: " + (currentShowTitles ? "ON" : "OFF"));
        showTitlesButton.setOnClickListener(v -> {
            currentShowTitles = !currentShowTitles;
            showTitlesButton.setText("Titles: " + (currentShowTitles ? "ON" : "OFF"));
            if (titlesCallback != null) titlesCallback.onShowTitlesChanged(currentShowTitles);
        });

        TextView groupButton = findViewById(R.id.group_button);
        DialogEffectHelper.applyButtonTheme(groupButton, theme, getContext(), surfaceColor);
        updateGroupText(groupButton);
        groupButton.setOnClickListener(v -> {
            currentGroupMode = (currentGroupMode + 1) % 4;
            getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("gallery_group_by", currentGroupMode).apply();
            updateGroupText(groupButton);
            if (groupCallback != null) groupCallback.onGroupChanged(currentGroupMode);
        });

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

        TextView trashButton = findViewById(R.id.trash_button);
        DialogEffectHelper.applyButtonTheme(trashButton, theme, getContext(), surfaceColor);
        trashButton.setText(isTrashMode ? "Gallery" : "Trash");
        trashButton.setOnClickListener(v -> {
            dismiss();
            trashCallback.onTrashClick();
        });

        TextView favoritesButton = findViewById(R.id.favorites_button);
        DialogEffectHelper.applyButtonTheme(favoritesButton, theme, getContext(), surfaceColor);
        favoritesButton.setText(isFavoritesMode ? "Gallery" : "Favorites");
        favoritesButton.setOnClickListener(v -> {
            dismiss();
            if (favoritesCallback != null) favoritesCallback.onFavoritesClick();
        });

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
    }

    private void updateGroupText(TextView btn) {
        String label;
        if (currentGroupMode == 1) label = "Day";
        else if (currentGroupMode == 2) label = "Month";
        else if (currentGroupMode == 3) label = "Year";
        else label = "None";
        btn.setText("Group: " + label);
    }

    private void updateFilterText(TextView btn) {
        String label;
        if (currentFilterMode == 1) label = "Images";
        else if (currentFilterMode == 2) label = "Videos";
        else label = "All";
        btn.setText("Filter: " + label);
    }

    private void updateColBold(int[] colButtons, int selectedCols) {
        for (int i = 0; i < colButtons.length; i++) {
            TextView btn = findViewById(colButtons[i]);
            if (btn != null) btn.setTypeface(null, getColValue(colButtons[i]) == selectedCols ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void updateRowBold(int[] rowButtons, int selectedRows) {
        for (int i = 0; i < rowButtons.length; i++) {
            TextView btn = findViewById(rowButtons[i]);
            if (btn != null) btn.setTypeface(null, getRowValue(rowButtons[i]) == selectedRows ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private int getColValue(int id) {
        if (id == R.id.btn_cols_2) return 2;
        if (id == R.id.btn_cols_3) return 3;
        if (id == R.id.btn_cols_4) return 4;
        if (id == R.id.btn_cols_5) return 5;
        if (id == R.id.btn_cols_6) return 6;
        return -1;
    }

    private int getRowValue(int id) {
        if (id == R.id.btn_rows_2) return 2;
        if (id == R.id.btn_rows_3) return 3;
        if (id == R.id.btn_rows_4) return 4;
        if (id == R.id.btn_rows_5) return 5;
        if (id == R.id.btn_rows_6) return 6;
        return -1;
    }
}
