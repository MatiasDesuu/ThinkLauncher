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

    private OnGridChangedCallback gridCallback;
    private OnShowTitlesChangedCallback titlesCallback;
    private int currentColumns;
    private int currentRows;
    private boolean currentShowTitles;
    private boolean isGridView;

    public GalleryOptionsDialog(Context context, int columns, int rows, boolean showTitles,
                                boolean isGridView,
                                OnGridChangedCallback gridCallback,
                                OnShowTitlesChangedCallback titlesCallback) {
        super(context, R.style.NoAnimationDialog);
        this.gridCallback = gridCallback;
        this.titlesCallback = titlesCallback;
        this.currentColumns = columns;
        this.currentRows = rows;
        this.currentShowTitles = showTitles;
        this.isGridView = isGridView;
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

        View rowsContainer = findViewById(R.id.rows_container);
        if (isGridView) {
            rowsContainer.setVisibility(View.GONE);
        } else {
            TextView rowsLabel = findViewById(R.id.gallery_rows_label);
            rowsLabel.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        }

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
                gridCallback.onGridChanged(cols, currentRows);
                dismiss();
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
                gridCallback.onGridChanged(currentColumns, rows);
                dismiss();
            });
        }

        TextView showTitlesButton = findViewById(R.id.show_titles_button);
        DialogEffectHelper.applyButtonTheme(showTitlesButton, theme, getContext(), surfaceColor);
        showTitlesButton.setText("Titles: " + (currentShowTitles ? "ON" : "OFF"));
        showTitlesButton.setOnClickListener(v -> {
            titlesCallback.onShowTitlesChanged(!currentShowTitles);
            dismiss();
        });
    }
}
