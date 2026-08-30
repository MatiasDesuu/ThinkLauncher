package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class UpdateAvailableDialog extends GuardedDialog {

    public interface Callback {
        void onDownload();
        void onView();
    }

    public UpdateAvailableDialog(Context context, String current, String latestTag, String latestTitle, String changelog, Callback cb) {
        super(context, R.style.NoAnimationDialog);
        init(current, latestTag, latestTitle, changelog, cb);
    }

    private void init(String current, String latestTag, String latestTitle, String changelog, Callback cb) {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_update_available);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);

        TextView title = findViewById(R.id.title_text);
        TextView versionInfo = findViewById(R.id.version_info);
        TextView changelogView = findViewById(R.id.changelog_text);
        TextView btnView = findViewById(R.id.btn_view);
        TextView btnLater = findViewById(R.id.btn_later);
        TextView btnDownload = findViewById(R.id.btn_download);

        title.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        versionInfo.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
        changelogView.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        versionInfo.setText("Current: " + current + "  →  Latest: " + latestTag + (latestTitle != null && !latestTitle.isEmpty() && !latestTitle.equals(latestTag) ? " (" + latestTitle + ")" : ""));
        if (changelog == null || changelog.trim().isEmpty()) {
            changelogView.setText("No changelog available.");
        } else {
            String text = changelog.trim();
            if (text.length() > 2000) text = text.substring(0, 2000) + "...";
            changelogView.setText(text);
        }

        DialogEffectHelper.applyButtonTheme(btnView, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnLater, theme, getContext(), surfaceColor);
        DialogEffectHelper.applyButtonTheme(btnDownload, theme, getContext(), surfaceColor);

        btnView.setOnClickListener(v -> {
            dismiss();
            if (cb != null) cb.onView();
        });
        btnLater.setOnClickListener(v -> dismiss());
        btnDownload.setOnClickListener(v -> {
            dismiss();
            if (cb != null) cb.onDownload();
        });
    }
}
