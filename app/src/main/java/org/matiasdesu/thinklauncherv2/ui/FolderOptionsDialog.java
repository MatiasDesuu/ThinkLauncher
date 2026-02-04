package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class FolderOptionsDialog extends Dialog {

    public interface OnChangeFolderNameCallback {
        void onChangeFolderName();
    }

    public interface OnToggleSortCallback {
        void onToggleSort();
    }

    public interface OnReorderCallback {
        void onReorder();
    }

    private OnChangeFolderNameCallback changeFolderNameCallback;
    private OnToggleSortCallback toggleSortCallback;
    private OnReorderCallback reorderCallback;
    private int sortMode;

    public FolderOptionsDialog(Context context, int sortMode, OnChangeFolderNameCallback changeFolderNameCallback, OnToggleSortCallback toggleSortCallback, OnReorderCallback reorderCallback) {
        super(context, R.style.NoAnimationDialog);
        this.changeFolderNameCallback = changeFolderNameCallback;
        this.toggleSortCallback = toggleSortCallback;
        this.reorderCallback = reorderCallback;
        this.sortMode = sortMode;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_folder_options);
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Aplicar colores
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, getContext());
            GradientDrawable drawable = (GradientDrawable) root.getBackground();
            drawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));
        }

        TextView changeFolderNameButton = findViewById(R.id.change_folder_name_button);
        ThemeUtils.applyButtonTheme(changeFolderNameButton, theme, getContext());
        changeFolderNameButton.setOnClickListener(v -> {
            changeFolderNameCallback.onChangeFolderName();
            dismiss();
        });

        TextView sortButton = findViewById(R.id.sort_button);
        ThemeUtils.applyButtonTheme(sortButton, theme, getContext());
        
        // Update button text based on current sort state
        if (sortMode == 0) {
            sortButton.setText("Sort: Added");
        } else if (sortMode == 1) {
            sortButton.setText("Sort: Alphabetical");
        } else {
            sortButton.setText("Sort: Custom");
        }
        
        sortButton.setOnClickListener(v -> {
            toggleSortCallback.onToggleSort();
            dismiss();
        });

        TextView reorderButton = findViewById(R.id.reorder_button);
        if (sortMode == 2) {
            reorderButton.setVisibility(View.VISIBLE);
            ThemeUtils.applyButtonTheme(reorderButton, theme, getContext());
            reorderButton.setOnClickListener(v -> {
                reorderCallback.onReorder();
                dismiss();
            });
            
            // Add margin to sort button when reorder button is visible
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sortButton.getLayoutParams();
            params.bottomMargin = (int) (8 * getContext().getResources().getDisplayMetrics().density);
            sortButton.setLayoutParams(params);
        } else {
            reorderButton.setVisibility(View.GONE);
            
            // Remove margin from sort button when reorder button is hidden
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sortButton.getLayoutParams();
            params.bottomMargin = 0;
            sortButton.setLayoutParams(params);
        }
    }
}
