package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class WebAppDialog extends Dialog {

    public interface WebAppCallback {
        void onWebAppAccepted(String name, String url);
    }

    private WebAppCallback callback;

    public WebAppDialog(Context context, String initialName, String initialUrl, WebAppCallback callback) {
        super(context, R.style.NoAnimationDialog);
        this.callback = callback;
        init(initialName, initialUrl);
    }

    private void init(String initialName, String initialUrl) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_webapp);
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Apply colors
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, getContext());
            GradientDrawable drawable = (GradientDrawable) root.getBackground();
            drawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));
        }

        EditText nameEditText = findViewById(R.id.webapp_name_edit_text);
        ThemeUtils.applyEditTextTheme(nameEditText, theme, getContext());
        GradientDrawable nameDrawable = (GradientDrawable) nameEditText.getBackground();
        nameDrawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));

        EditText urlEditText = findViewById(R.id.webapp_url_edit_text);
        ThemeUtils.applyEditTextTheme(urlEditText, theme, getContext());
        GradientDrawable urlDrawable = (GradientDrawable) urlEditText.getBackground();
        urlDrawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));

        TextView nameLabel = findViewById(R.id.webapp_name_label);
        ThemeUtils.applyTextColor(nameLabel, theme, getContext());

        TextView urlLabel = findViewById(R.id.webapp_url_label);
        ThemeUtils.applyTextColor(urlLabel, theme, getContext());

        TextView cancelButton = findViewById(R.id.cancel_button);
        ThemeUtils.applyButtonTheme(cancelButton, theme, getContext());

        TextView okButton = findViewById(R.id.ok_button);
        ThemeUtils.applyButtonTheme(okButton, theme, getContext());

        nameEditText.setText(initialName);
        urlEditText.setText(initialUrl);

        cancelButton.setOnClickListener(v -> dismiss());

        okButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String url = urlEditText.getText().toString().trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                // Add protocol if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                callback.onWebAppAccepted(name, url);
            }
            dismiss();
        });
    }
}
