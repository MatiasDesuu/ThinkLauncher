package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class GalleryEmptyTrashConfirmDialog extends Dialog {

    public interface OnConfirmCallback {
        void onConfirm();
    }

    private final int count;
    private final OnConfirmCallback callback;

    public GalleryEmptyTrashConfirmDialog(Context context, int count, OnConfirmCallback callback) {
        super(context, R.style.NoAnimationDialog);
        this.count = count;
        this.callback = callback;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_gallery_empty_trash);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        android.view.View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView message = findViewById(R.id.empty_trash_message);
        message.setText("Permanently delete " + count + " item" + (count == 1 ? "" : "s") + "?");
        message.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        TextView title = findViewById(R.id.empty_trash_title);
        title.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        TextView cancelButton = findViewById(R.id.cancel_button);
        DialogEffectHelper.applyButtonTheme(cancelButton, theme, getContext(), surfaceColor);
        cancelButton.setOnClickListener(v -> dismiss());

        TextView deleteButton = findViewById(R.id.delete_button);
        DialogEffectHelper.applyButtonTheme(deleteButton, theme, getContext(), surfaceColor);
        deleteButton.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onConfirm();
        });
    }
}
