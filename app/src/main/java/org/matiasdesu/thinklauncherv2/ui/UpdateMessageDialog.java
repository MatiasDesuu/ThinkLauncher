package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class UpdateMessageDialog extends GuardedDialog {

    private TextView titleView;
    private TextView messageView;
    private TextView positiveButton;
    private TextView negativeButton;
    private TextView neutralButton;

    public UpdateMessageDialog(Context context, String title, String message) {
        super(context, R.style.NoAnimationDialog);
        init(title, message);
    }

    private void init(String title, String message) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_update_message);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        titleView = findViewById(R.id.dialog_title);
        messageView = findViewById(R.id.dialog_message);
        positiveButton = findViewById(R.id.button_positive);
        negativeButton = findViewById(R.id.button_negative);
        neutralButton = findViewById(R.id.button_neutral);

        titleView.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        messageView.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        if (title != null) titleView.setText(title);
        if (message != null) messageView.setText(message);

        DialogEffectHelper.applyButtonTheme(positiveButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(negativeButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(neutralButton, theme, getContext(), surfaceColor);
    }

    public UpdateMessageDialog setPositiveButton(String text, View.OnClickListener listener) {
        if (positiveButton != null) {
            positiveButton.setText(text);
            positiveButton.setVisibility(View.VISIBLE);
            positiveButton.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onClick(v);
            });
        }
        return this;
    }

    public UpdateMessageDialog setNegativeButton(String text, View.OnClickListener listener) {
        if (negativeButton != null) {
            negativeButton.setText(text);
            negativeButton.setVisibility(View.VISIBLE);
            negativeButton.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onClick(v);
            });
        }
        return this;
    }

    public UpdateMessageDialog setNeutralButton(String text, View.OnClickListener listener) {
        if (neutralButton != null) {
            neutralButton.setText(text);
            neutralButton.setVisibility(View.VISIBLE);
            neutralButton.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onClick(v);
            });
        }
        return this;
    }

    public UpdateMessageDialog hidePositiveButton() {
        if (positiveButton != null) positiveButton.setVisibility(View.GONE);
        return this;
    }
}
