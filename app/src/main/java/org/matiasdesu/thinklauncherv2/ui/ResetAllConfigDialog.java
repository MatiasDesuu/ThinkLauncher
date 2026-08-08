package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class ResetAllConfigDialog extends Dialog {

    public interface OnConfirmCallback {
        void onConfirm();
    }

    public ResetAllConfigDialog(Context context, OnConfirmCallback callback) {
        super(context, R.style.NoAnimationDialog);
        init(callback);
    }

    private void init(OnConfirmCallback callback) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_reset_all_config);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        DialogEffectHelper.setup(this, theme);
        int surfaceColor = ThemeUtils.getBgColor(theme, getContext());

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView messageText = findViewById(R.id.dialog_message);
        messageText.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        TextView cancelButton = findViewById(R.id.cancel_button);
        DialogEffectHelper.applyButtonTheme(cancelButton, theme, getContext(), surfaceColor);
        cancelButton.setOnClickListener(v -> dismiss());

        TextView resetButton = findViewById(R.id.reset_button);
        DialogEffectHelper.applyButtonTheme(resetButton, theme, getContext(), surfaceColor);
        resetButton.setOnClickListener(v -> {
            callback.onConfirm();
            dismiss();
        });
    }
}
