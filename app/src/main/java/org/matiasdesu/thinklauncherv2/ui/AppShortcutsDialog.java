package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;

import java.util.List;

public class AppShortcutsDialog extends Dialog {

    public interface OnEditCallback {
        void onEdit();
    }

    public interface OnShortcutClickCallback {
        void onShortcutClick(ShortcutInfo shortcut);
    }

    private List<ShortcutInfo> shortcuts;
    private OnEditCallback editCallback;
    private OnShortcutClickCallback shortcutCallback;

    public AppShortcutsDialog(Context context, List<ShortcutInfo> shortcuts,
            OnEditCallback editCallback, OnShortcutClickCallback shortcutCallback) {
        super(context, R.style.NoAnimationDialog);
        this.shortcuts = shortcuts;
        this.editCallback = editCallback;
        this.shortcutCallback = shortcutCallback;
        init();
    }

    private void init() {
        setContentView(R.layout.dialog_app_shortcuts);

        int theme = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("theme", 0);
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        LinearLayout buttonContainer = findViewById(R.id.button_container);
        TextView editButton = findViewById(R.id.edit_button);

        int marginDp = (int) (8 * getContext().getResources().getDisplayMetrics().density);

        if (shortcuts != null && !shortcuts.isEmpty()) {
            for (int i = 0; i < shortcuts.size(); i++) {
                ShortcutInfo si = shortcuts.get(i);
                TextView tv = new TextView(getContext());
                tv.setText(si.getShortLabel() != null ? si.getShortLabel() : si.getId());
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setTextSize(18);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = marginDp;
                tv.setLayoutParams(params);
                DialogEffectHelper.applyButtonTheme(tv, theme, getContext(), surfaceColor);
                final ShortcutInfo shortcut = si;
                tv.setOnClickListener(v -> {
                    if (shortcutCallback != null) {
                        shortcutCallback.onShortcutClick(shortcut);
                    }
                    dismiss();
                });
                buttonContainer.addView(tv, buttonContainer.getChildCount() - 1);
            }
        }

        DialogEffectHelper.applyButtonTheme(editButton, theme, getContext(), surfaceColor);
        editButton.setOnClickListener(v -> {
            if (editCallback != null) {
                editCallback.onEdit();
            }
            dismiss();
        });
    }
}
