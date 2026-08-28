package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class GalleryFolderOptionsDialog extends Dialog {

    public interface OnPinToggleCallback {
        void onPinToggle();
    }

    public interface OnOpenCallback {
        void onOpen();
    }

    public interface OnRenameCallback {
        void onRename();
    }

    private final String folderName;
    private final boolean isPinned;
    private final OnPinToggleCallback pinCallback;
    private final OnOpenCallback openCallback;
    private final OnRenameCallback renameCallback;

    public GalleryFolderOptionsDialog(Context context, String folderName, boolean isPinned,
                                      OnPinToggleCallback pinCallback, OnOpenCallback openCallback, OnRenameCallback renameCallback) {
        super(context, R.style.NoAnimationDialog);
        this.folderName = folderName;
        this.isPinned = isPinned;
        this.pinCallback = pinCallback;
        this.openCallback = openCallback;
        this.renameCallback = renameCallback;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_gallery_folder_options);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        android.view.View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView label = findViewById(R.id.folder_name_label);
        label.setText(folderName);
        label.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        TextView pinButton = findViewById(R.id.pin_button);
        DialogEffectHelper.applyButtonTheme(pinButton, theme, getContext(), surfaceColor);
        pinButton.setText(isPinned ? "Unpin" : "Pin to top");
        pinButton.setOnClickListener(v -> {
            boolean currentlyPinned = pinButton.getText().toString().equals("Unpin");
            pinButton.setText(currentlyPinned ? "Pin to top" : "Unpin");
            if (pinCallback != null) pinCallback.onPinToggle();
        });

        TextView renameButton = findViewById(R.id.rename_button);
        DialogEffectHelper.applyButtonTheme(renameButton, theme, getContext(), surfaceColor);
        renameButton.setOnClickListener(v -> {
            dismiss();
            if (renameCallback != null) renameCallback.onRename();
        });

        TextView openButton = findViewById(R.id.open_button);
        DialogEffectHelper.applyButtonTheme(openButton, theme, getContext(), surfaceColor);
        openButton.setOnClickListener(v -> {
            dismiss();
            if (openCallback != null) openCallback.onOpen();
        });
    }
}
