package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class AppOptionsDialog extends Dialog {

    public interface OnRemoveCallback {
        void onRemove();
    }

    public interface OnMoreInfoCallback {
        void onMoreInfo();
    }

    private String packageName;
    private OnRemoveCallback removeCallback;
    private OnMoreInfoCallback moreInfoCallback;

    public AppOptionsDialog(Context context, String packageName) {
        super(context, R.style.NoAnimationDialog);
        this.packageName = packageName;
        this.removeCallback = null;
        this.moreInfoCallback = null;
        init();
    }

    public AppOptionsDialog(Context context, String packageName, OnRemoveCallback removeCallback) {
        super(context, R.style.NoAnimationDialog);
        this.packageName = packageName;
        this.removeCallback = removeCallback;
        this.moreInfoCallback = null;
        init();
    }

    public AppOptionsDialog(Context context, String packageName, OnRemoveCallback removeCallback, OnMoreInfoCallback moreInfoCallback) {
        super(context, R.style.NoAnimationDialog);
        this.packageName = packageName;
        this.removeCallback = removeCallback;
        this.moreInfoCallback = moreInfoCallback;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_app_options);
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Aplicar colores
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, getContext());
            GradientDrawable drawable = (GradientDrawable) root.getBackground();
            drawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));
        }

        TextView moreInfoButton = findViewById(R.id.more_info_button);
        ThemeUtils.applyButtonTheme(moreInfoButton, theme, getContext());

        if (packageName != null && packageName.startsWith("webapp_")) {
            moreInfoButton.setText("Edit Web App");
        }

        TextView uninstallButton = findViewById(R.id.uninstall_button);
        ThemeUtils.applyButtonTheme(uninstallButton, theme, getContext());
        
        if (packageName != null && (packageName.startsWith("webapp_") || packageName.startsWith("folder_") || packageName.equals("launcher_settings") || packageName.equals("app_launcher") || packageName.equals("notification_panel"))) {
            uninstallButton.setVisibility(View.GONE);
        }

        TextView removeButton = findViewById(R.id.remove_button);

        // Adjust margin for uninstall button based on remove button visibility
        int marginBottom = (removeCallback != null) ? (int)(8 * getContext().getResources().getDisplayMetrics().density) : 0;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) uninstallButton.getLayoutParams();
        if (params != null) {
            params.bottomMargin = marginBottom;
            uninstallButton.setLayoutParams(params);
        }

        if (removeCallback != null) {
            // In folder context - show remove button
            if (removeButton != null) {
                removeButton.setVisibility(View.VISIBLE);
                ThemeUtils.applyButtonTheme(removeButton, theme, getContext());
                removeButton.setOnClickListener(v -> {
                    removeCallback.onRemove();
                    dismiss();
                });
            }
        } else {
            // Normal context
            if (removeButton != null) removeButton.setVisibility(View.GONE);
        }

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

        uninstallButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageName));
            getContext().startActivity(intent);
            dismiss();
        });
    }
}