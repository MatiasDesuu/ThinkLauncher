package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;

public class MusicDockOptionsDialog extends Dialog {

    public interface OnAlwaysActiveChanged {
        void onChanged(boolean enabled);
    }

    public interface OnEditCallback {
        void onEdit();
    }

    private final OnAlwaysActiveChanged alwaysActiveChanged;
    private final OnEditCallback editCallback;
    private boolean alwaysActive;

    public MusicDockOptionsDialog(Context context, boolean alwaysActive,
            OnAlwaysActiveChanged alwaysActiveChanged, OnEditCallback editCallback) {
        super(context, R.style.NoAnimationDialog);
        this.alwaysActive = alwaysActive;
        this.alwaysActiveChanged = alwaysActiveChanged;
        this.editCallback = editCallback;
        init();
    }

    private void init() {
        int theme = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getInt("theme", 0);
        setContentView(R.layout.dialog_music_dock_options);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView alwaysActiveButton = findViewById(R.id.always_active_button);
        TextView editButton = findViewById(R.id.edit_button);
        updateAlwaysActiveText(alwaysActiveButton);
        DialogEffectHelper.applyButtonTheme(alwaysActiveButton, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(editButton, theme, getContext(), surfaceColor);

        alwaysActiveButton.setOnClickListener(v -> {
            alwaysActive = !alwaysActive;
            updateAlwaysActiveText(alwaysActiveButton);
            if (alwaysActiveChanged != null) {
                alwaysActiveChanged.onChanged(alwaysActive);
            }
        });

        editButton.setOnClickListener(v -> {
            if (editCallback != null) {
                editCallback.onEdit();
            }
            dismiss();
        });
    }

    private void updateAlwaysActiveText(TextView button) {
        button.setText("Always Active: " + (alwaysActive ? "ON" : "OFF"));
    }
}
