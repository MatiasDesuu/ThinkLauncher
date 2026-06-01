package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;

public class ClearAllGesturesDialog extends Dialog {

    public interface OnConfirmCallback {
        void onConfirm();
    }

    public ClearAllGesturesDialog(Context context, OnConfirmCallback callback) {
        super(context, R.style.NoAnimationDialog);
        init(callback);
    }

    private void init(OnConfirmCallback callback) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_clear_all_gestures);
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView messageText = findViewById(R.id.dialog_message);
        messageText.setTextColor(org.matiasdesu.thinklauncherv2.utils.ThemeUtils.getTextColor(theme, getContext()));

        TextView cancelButton = findViewById(R.id.cancel_button);
        DialogEffectHelper.applyButtonTheme(cancelButton, theme, getContext(), surfaceColor);
        cancelButton.setOnClickListener(v -> dismiss());

        TextView clearButton = findViewById(R.id.clear_button);
        DialogEffectHelper.applyButtonTheme(clearButton, theme, getContext(), surfaceColor);
        clearButton.setOnClickListener(v -> {
            callback.onConfirm();
            dismiss();
        });
    }
}
