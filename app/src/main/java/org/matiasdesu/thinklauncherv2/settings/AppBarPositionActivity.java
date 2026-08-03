package org.matiasdesu.thinklauncherv2.settings;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.views.AppBarPositionView;

public class AppBarPositionActivity extends BaseSettingsActivity {

    private static final String[] POSITION_LABELS = {
            "Top Left", "Top Right", "Bottom Left", "Bottom Right",
            "Center Left", "Center Right", "Top Center", "Bottom Center", "Center"
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_app_bar_position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        TextView selectedLabel = findViewById(R.id.selected_label);

        AppBarPositionView picker = findViewById(R.id.position_picker);
        int current = prefs.getInt("app_bar_position", 0);
        picker.setSelectedPosition(current);
        selectedLabel.setText(POSITION_LABELS[current]);

        picker.setOnPositionSelectedListener((position, label) -> {
            prefs.edit().putInt("app_bar_position", position).apply();
            selectedLabel.setText(label);
        });
    }
}