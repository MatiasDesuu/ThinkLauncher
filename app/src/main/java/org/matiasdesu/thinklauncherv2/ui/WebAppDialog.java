package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
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
        int surfaceColor = DialogEffectHelper.setup(this, theme);

        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        EditText nameEditText = findViewById(R.id.webapp_name_edit_text);
        DialogEffectHelper.applyEditTextTheme(nameEditText, theme, getContext(), surfaceColor);

        EditText urlEditText = findViewById(R.id.webapp_url_edit_text);
        DialogEffectHelper.applyEditTextTheme(urlEditText, theme, getContext(), surfaceColor);

        TextView nameLabel = findViewById(R.id.webapp_name_label);
        ThemeUtils.applyTextColor(nameLabel, theme, getContext());

        TextView urlLabel = findViewById(R.id.webapp_url_label);
        ThemeUtils.applyTextColor(urlLabel, theme, getContext());

        TextView cancelButton = findViewById(R.id.cancel_button);
        DialogEffectHelper.applyButtonTheme(cancelButton, theme, getContext(), surfaceColor);

        TextView okButton = findViewById(R.id.ok_button);
        DialogEffectHelper.applyButtonTheme(okButton, theme, getContext(), surfaceColor);

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
