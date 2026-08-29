package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;

public class DeleteImageDialog extends GuardedDialog {

    public interface OnConfirmCallback {
        void onConfirm();
    }

    public DeleteImageDialog(Context context, OnConfirmCallback callback) {
        super(context, R.style.NoAnimationDialog);
        init(callback, null, null);
    }

    public DeleteImageDialog(Context context, String message, String confirmText, OnConfirmCallback callback) {
        super(context, R.style.NoAnimationDialog);
        init(callback, message, confirmText);
    }

    private void init(OnConfirmCallback callback, String message, String confirmText) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_delete_image);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView cancelButton = findViewById(R.id.cancel_button);
        DialogEffectHelper.applyButtonTheme(cancelButton, theme, getContext(), surfaceColor);
        cancelButton.setOnClickListener(v -> dismiss());

        TextView messageView = findViewById(R.id.dialog_message);
        messageView.setTextColor(org.matiasdesu.thinklauncherv2.utils.ThemeUtils.getTextColor(theme, getContext()));
        if (message != null) messageView.setText(message);
        TextView deleteButton = findViewById(R.id.delete_button);
        DialogEffectHelper.applyButtonTheme(deleteButton, theme, getContext(), surfaceColor);
        if (confirmText != null) deleteButton.setText(confirmText);
        deleteButton.setOnClickListener(v -> {
            callback.onConfirm();
            dismiss();
        });
    }
}
