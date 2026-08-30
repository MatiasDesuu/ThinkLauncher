package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class UpdateProgressDialog extends GuardedDialog {

    private TextView titleView;
    private TextView progressText;
    private TextView cancelButton;

    public UpdateProgressDialog(Context context, String title) {
        super(context, R.style.NoAnimationDialog);
        init(title);
    }

    private void init(String title) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_update_progress);
        setCancelable(false);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        titleView = findViewById(R.id.title_text);
        progressText = findViewById(R.id.progress_text);
        cancelButton = findViewById(R.id.cancel_button);

        titleView.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        progressText.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        if (title != null) titleView.setText(title);

        DialogEffectHelper.applyButtonTheme(cancelButton, theme, getContext(), surfaceColor);
        cancelButton.setOnClickListener(v -> dismiss());
    }

    public void setProgress(int percent, String text) {
        if (progressText != null && text != null) progressText.setText(text);
    }

    public void setCancelListener(View.OnClickListener listener) {
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onClick(v);
            });
        }
    }
}
