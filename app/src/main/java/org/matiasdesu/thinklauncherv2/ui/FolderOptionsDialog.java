package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;

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
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView changeFolderNameButton = findViewById(R.id.change_folder_name_button);
        DialogEffectHelper.applyButtonTheme(changeFolderNameButton, theme, getContext(), surfaceColor);
        changeFolderNameButton.setOnClickListener(v -> {
            changeFolderNameCallback.onChangeFolderName();
            dismiss();
        });

        TextView sortButton = findViewById(R.id.sort_button);
        DialogEffectHelper.applyButtonTheme(sortButton, theme, getContext(), surfaceColor);
        
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
            DialogEffectHelper.applyButtonTheme(reorderButton, theme, getContext(), surfaceColor);
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
