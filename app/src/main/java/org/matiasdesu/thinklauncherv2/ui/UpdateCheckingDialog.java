package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class UpdateCheckingDialog extends GuardedDialog {

    public UpdateCheckingDialog(Context context) {
        super(context, R.style.NoAnimationDialog);
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        setContentView(R.layout.dialog_update_checking);
        setCancelable(false);
        FontHelper.applyToViewTree(getContext(), findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(this, theme);
        View root = findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, getContext(), surfaceColor);
        TextView tv = findViewById(R.id.progress_text);
        if (tv != null) tv.setTextColor(ThemeUtils.getTextColor(theme, getContext()));
    }
}
