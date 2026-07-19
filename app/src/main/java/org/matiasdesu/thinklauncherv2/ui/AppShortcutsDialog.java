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

    public interface OnSecondaryCallback {
        void onSecondary();
    }

    public interface OnShortcutClickCallback {
        void onShortcutClick(ShortcutInfo shortcut);
    }

    private List<ShortcutInfo> shortcuts;
    private String secondaryLabel;
    private OnSecondaryCallback secondaryCallback;
    private OnShortcutClickCallback shortcutCallback;

    public AppShortcutsDialog(Context context, List<ShortcutInfo> shortcuts,
            String secondaryLabel, OnSecondaryCallback secondaryCallback,
            OnShortcutClickCallback shortcutCallback) {
        super(context, R.style.NoAnimationDialog);
        this.shortcuts = shortcuts;
        this.secondaryLabel = secondaryLabel;
        this.secondaryCallback = secondaryCallback;
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
        TextView secondaryButton = findViewById(R.id.edit_button);

        int marginDp = (int) (8 * getContext().getResources().getDisplayMetrics().density);

        if (shortcuts != null && !shortcuts.isEmpty()) {
            for (int i = 0; i < shortcuts.size(); i++) {
                ShortcutInfo si = shortcuts.get(i);
                TextView tv = new TextView(getContext());
                CharSequence label = si.getLongLabel() != null ? si.getLongLabel() : si.getShortLabel();
                tv.setText(label != null ? label : si.getId());
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
                    v.post(this::dismiss);
                });
                buttonContainer.addView(tv, buttonContainer.getChildCount() - 1);
            }
        }

        secondaryButton.setText(secondaryLabel != null ? secondaryLabel : "Edit");
        DialogEffectHelper.applyButtonTheme(secondaryButton, theme, getContext(), surfaceColor);
        secondaryButton.setOnClickListener(v -> {
            if (secondaryCallback != null) {
                secondaryCallback.onSecondary();
            }
            v.post(this::dismiss);
        });
    }
}
