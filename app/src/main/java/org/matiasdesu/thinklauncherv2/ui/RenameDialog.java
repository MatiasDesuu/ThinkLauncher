package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;

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
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        EditText editText = findViewById(R.id.rename_edit_text);
        DialogEffectHelper.applyEditTextTheme(editText, theme, getContext(), surfaceColor);

        TextView cancelButton = findViewById(R.id.cancel_button);
        DialogEffectHelper.applyButtonTheme(cancelButton, theme, getContext(), surfaceColor);

        TextView okButton = findViewById(R.id.ok_button);
        DialogEffectHelper.applyButtonTheme(okButton, theme, getContext(), surfaceColor);

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
