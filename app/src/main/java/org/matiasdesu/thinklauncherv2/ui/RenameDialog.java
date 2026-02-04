package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;

public class RenameDialog extends Dialog {

    public interface RenameCallback {
        void onRenameAccepted(String newName);
    }

    private RenameCallback callback;

    public RenameDialog(Context context, String initialName, RenameCallback callback) {
        super(context, R.style.NoAnimationDialog);
        this.callback = callback;
        init(initialName);
    }

    private void init(String initialName) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_rename_app);
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Aplicar colores
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, getContext());
            GradientDrawable drawable = (GradientDrawable) root.getBackground();
            drawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));
        }

        EditText editText = findViewById(R.id.rename_edit_text);
        ThemeUtils.applyEditTextTheme(editText, theme, getContext());
        GradientDrawable editDrawable = (GradientDrawable) editText.getBackground();
        editDrawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));

        TextView cancelButton = findViewById(R.id.cancel_button);
        ThemeUtils.applyButtonTheme(cancelButton, theme, getContext());

        TextView okButton = findViewById(R.id.ok_button);
        ThemeUtils.applyButtonTheme(okButton, theme, getContext());

        editText.setText(initialName);

        TextView cb = findViewById(R.id.cancel_button);
        cb.setOnClickListener(v -> dismiss());

        TextView ob = findViewById(R.id.ok_button);
        ob.setOnClickListener(v -> {
            String newName = editText.getText().toString().trim();
            if (!newName.isEmpty()) {
                callback.onRenameAccepted(newName);
            }
            dismiss();
        });
    }
}