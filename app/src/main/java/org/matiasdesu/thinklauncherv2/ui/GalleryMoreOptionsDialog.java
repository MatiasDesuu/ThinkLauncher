package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class GalleryMoreOptionsDialog extends GuardedDialog {

    public interface OnGridChangedCallback {
        void onGridChanged(int columns, int rows);
    }

    public interface OnShowTitlesChangedCallback {
        void onShowTitlesChanged(boolean show);
    }

    public interface OnGroupChangedCallback {
        void onGroupChanged(int groupMode);
    }

    public interface OnSidebarChangedCallback {
        void onSidebarChanged(boolean enabled);
    }

    public interface OnBackCallback {
        void onBack();
    }

    private OnGridChangedCallback gridCallback;
    private OnShowTitlesChangedCallback titlesCallback;
    private OnGroupChangedCallback groupCallback;
    private OnSidebarChangedCallback sidebarCallback;
    private OnBackCallback backCallback;
    private int currentColumns;
    private int currentRows;
    private boolean currentShowTitles;
    private boolean isGridView;
    private boolean hasPagination;
    private int currentGroupMode;
    private boolean currentSidebarEnabled;

    public GalleryMoreOptionsDialog(Context context, int columns, int rows, boolean showTitles,
                                    boolean isGridView, boolean hasPagination,
                                    int groupMode, boolean sidebarEnabled,
                                    OnGridChangedCallback gridCallback,
                                    OnShowTitlesChangedCallback titlesCallback,
                                    OnGroupChangedCallback groupCallback,
                                    OnSidebarChangedCallback sidebarCallback,
                                    OnBackCallback backCallback) {
        super(context, R.style.NoAnimationDialog);
        this.gridCallback = gridCallback;
        this.titlesCallback = titlesCallback;
        this.groupCallback = groupCallback;
        this.sidebarCallback = sidebarCallback;
        this.backCallback = backCallback;
        this.currentColumns = columns;
        this.currentRows = rows;
        this.currentShowTitles = showTitles;
        this.isGridView = isGridView;
        this.hasPagination = hasPagination;
        this.currentGroupMode = groupMode;
        this.currentSidebarEnabled = sidebarEnabled;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_gallery_more_options);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView colsLabel = findViewById(R.id.gallery_columns_label);
        if (colsLabel != null) colsLabel.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        TextView rowsLabel = findViewById(R.id.gallery_rows_label);
        if (rowsLabel != null) rowsLabel.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        int[] colButtons = {R.id.btn_cols_2, R.id.btn_cols_3, R.id.btn_cols_4, R.id.btn_cols_5, R.id.btn_cols_6};
        int[] colValues = {2, 3, 4, 5, 6};
        for (int i = 0; i < colButtons.length; i++) {
            TextView btn = findViewById(colButtons[i]);
            if (btn == null) continue;
            DialogEffectHelper.applyButtonTheme(btn, theme, getContext(), surfaceColor);
            final int cols = colValues[i];
            if (cols == currentColumns) btn.setTypeface(null, Typeface.BOLD);
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
            if (btn == null) continue;
            DialogEffectHelper.applyButtonTheme(btn, theme, getContext(), surfaceColor);
            final int rows = rowValues[i];
            if (rows == currentRows) btn.setTypeface(null, Typeface.BOLD);
            btn.setOnClickListener(v -> {
                currentRows = rows;
                updateRowBold(rowButtons, rows);
                if (gridCallback != null) gridCallback.onGridChanged(currentColumns, rows);
            });
        }

        View rowsContainer = findViewById(R.id.rows_container);
        if (rowsContainer != null) rowsContainer.setVisibility(hasPagination ? View.VISIBLE : View.GONE);

        TextView showTitlesButton = findViewById(R.id.show_titles_button);
        if (showTitlesButton != null) {
            DialogEffectHelper.applyButtonTheme(showTitlesButton, theme, getContext(), surfaceColor);
            showTitlesButton.setText("Titles: " + (currentShowTitles ? "ON" : "OFF"));
            showTitlesButton.setOnClickListener(v -> {
                currentShowTitles = !currentShowTitles;
                showTitlesButton.setText("Titles: " + (currentShowTitles ? "ON" : "OFF"));
                if (titlesCallback != null) titlesCallback.onShowTitlesChanged(currentShowTitles);
            });
        }

        TextView groupButton = findViewById(R.id.group_button);
        if (groupButton != null) {
            DialogEffectHelper.applyButtonTheme(groupButton, theme, getContext(), surfaceColor);
            updateGroupText(groupButton);
            groupButton.setOnClickListener(v -> {
                currentGroupMode = (currentGroupMode + 1) % 4;
                getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("gallery_group_by", currentGroupMode).apply();
                updateGroupText(groupButton);
                if (groupCallback != null) groupCallback.onGroupChanged(currentGroupMode);
            });
        }

        TextView sidebarButton = findViewById(R.id.sidebar_button);
        if (sidebarButton != null) {
            DialogEffectHelper.applyButtonTheme(sidebarButton, theme, getContext(), surfaceColor);
            sidebarButton.setText("Sidebar: " + (currentSidebarEnabled ? "ON" : "OFF"));
            sidebarButton.setOnClickListener(v -> {
                currentSidebarEnabled = !currentSidebarEnabled;
                getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("gallery_index_sidebar", currentSidebarEnabled ? 1 : 0).apply();
                sidebarButton.setText("Sidebar: " + (currentSidebarEnabled ? "ON" : "OFF"));
                if (sidebarCallback != null) sidebarCallback.onSidebarChanged(currentSidebarEnabled);
            });
        }

        TextView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            DialogEffectHelper.applyButtonTheme(backButton, theme, getContext(), surfaceColor);
            backButton.setOnClickListener(v -> {
                dismiss();
                if (backCallback != null) backCallback.onBack();
            });
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

    private void updateColBold(int[] colButtons, int selectedCols) {
        for (int id : colButtons) {
            TextView btn = findViewById(id);
            if (btn != null) btn.setTypeface(null, getColValue(id) == selectedCols ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void updateRowBold(int[] rowButtons, int selectedRows) {
        for (int id : rowButtons) {
            TextView btn = findViewById(id);
            if (btn != null) btn.setTypeface(null, getRowValue(id) == selectedRows ? Typeface.BOLD : Typeface.NORMAL);
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
