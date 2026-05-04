package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;

public class AppOptionsDialog extends Dialog {

    public interface OnRemoveCallback {
        void onRemove();
    }

    public interface OnMoreInfoCallback {
        void onMoreInfo();
    }

    public interface OnRenameCallback {
        void onRename();
    }

    private String packageName;
    private OnRemoveCallback removeCallback;
    private OnMoreInfoCallback moreInfoCallback;
    private OnRenameCallback renameCallback;

    public AppOptionsDialog(Context context, String packageName) {
        super(context, R.style.NoAnimationDialog);
        this.packageName = packageName;
        this.removeCallback = null;
        this.moreInfoCallback = null;
        this.renameCallback = null;
        init();
    }

    public AppOptionsDialog(Context context, String packageName, OnRemoveCallback removeCallback) {
        super(context, R.style.NoAnimationDialog);
        this.packageName = packageName;
        this.removeCallback = removeCallback;
        this.moreInfoCallback = null;
        this.renameCallback = null;
        init();
    }

    public AppOptionsDialog(Context context, String packageName, OnRemoveCallback removeCallback,
            OnMoreInfoCallback moreInfoCallback) {
        super(context, R.style.NoAnimationDialog);
        this.packageName = packageName;
        this.removeCallback = removeCallback;
        this.moreInfoCallback = moreInfoCallback;
        this.renameCallback = null;
        init();
    }

    public AppOptionsDialog(Context context, String packageName, OnRemoveCallback removeCallback,
            OnMoreInfoCallback moreInfoCallback, OnRenameCallback renameCallback) {
        super(context, R.style.NoAnimationDialog);
        this.packageName = packageName;
        this.removeCallback = removeCallback;
        this.moreInfoCallback = moreInfoCallback;
        this.renameCallback = renameCallback;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_app_options);
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView renameButton = findViewById(R.id.rename_button);
        TextView moreInfoButton = findViewById(R.id.more_info_button);
        TextView uninstallButton = findViewById(R.id.uninstall_button);
        TextView removeButton = findViewById(R.id.remove_button);

        if (renameCallback != null) {
            renameButton.setVisibility(View.VISIBLE);
            DialogEffectHelper.applyButtonTheme(renameButton, theme, getContext(), surfaceColor);
            renameButton.setOnClickListener(v -> {
                renameCallback.onRename();
                dismiss();
            });
        } else {
            renameButton.setVisibility(View.GONE);
        }

        DialogEffectHelper.applyButtonTheme(moreInfoButton, theme, getContext(), surfaceColor);
        if (packageName != null && packageName.startsWith("webapp_")) {
            moreInfoButton.setText("Edit Web App");
        }
        if (packageName != null && (packageName.startsWith("folder_")
                || packageName.equals("launcher_settings") || packageName.equals("app_launcher")
                || packageName.equals("notification_panel") || packageName.equals("koreader_history")
                || packageName.equals("calendar"))) {
            moreInfoButton.setVisibility(View.GONE);
        } else {
            moreInfoButton.setVisibility(View.VISIBLE);
            moreInfoButton.setOnClickListener(v -> {
                if (moreInfoCallback != null) {
                    moreInfoCallback.onMoreInfo();
                } else {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));
                    try {
                        getContext().startActivity(intent);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                dismiss();
            });
        }

        DialogEffectHelper.applyButtonTheme(uninstallButton, theme, getContext(), surfaceColor);
        if (packageName != null && (packageName.startsWith("webapp_") || packageName.startsWith("folder_")
                || packageName.equals("launcher_settings") || packageName.equals("app_launcher")
                || packageName.equals("notification_panel") || packageName.equals("koreader_history")
                || packageName.equals("calendar"))) {
            uninstallButton.setVisibility(View.GONE);
        } else {
            uninstallButton.setVisibility(View.VISIBLE);
            uninstallButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                intent.setData(Uri.parse("package:" + packageName));
                getContext().startActivity(intent);
                dismiss();
            });
        }

        if (removeCallback != null) {
            removeButton.setVisibility(View.VISIBLE);
            DialogEffectHelper.applyButtonTheme(removeButton, theme, getContext(), surfaceColor);
            removeButton.setOnClickListener(v -> {
                removeCallback.onRemove();
                dismiss();
            });
        } else {
            removeButton.setVisibility(View.GONE);
        }

        TextView[] allButtons = {renameButton, moreInfoButton, uninstallButton, removeButton};
        View lastVisible = null;
        for (int i = allButtons.length - 1; i >= 0; i--) {
            if (allButtons[i].getVisibility() == View.VISIBLE) {
                lastVisible = allButtons[i];
                break;
            }
        }

        int eightDp = (int) (8 * getContext().getResources().getDisplayMetrics().density);
        for (TextView btn : allButtons) {
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) btn.getLayoutParams();
            if (p != null) {
                p.bottomMargin = (btn == lastVisible) ? 0 : eightDp;
                btn.setLayoutParams(p);
            }
        }
    }
}
