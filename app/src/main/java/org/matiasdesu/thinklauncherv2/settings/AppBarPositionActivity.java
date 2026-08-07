package org.matiasdesu.thinklauncherv2.settings;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.views.AppBarPositionView;

public class AppBarPositionActivity extends BaseSettingsActivity {

    public static final String EXTRA_PREF_KEY = "pref_key";
    public static final String EXTRA_DEFAULT_POSITION = "default_position";
    public static final String EXTRA_TITLE = "title";

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

        String prefKey = getIntent().getStringExtra(EXTRA_PREF_KEY);
        if (prefKey == null || prefKey.isEmpty()) {
            prefKey = "app_bar_position";
        }
        final String positionPrefKey = prefKey;
        int defaultPosition = getIntent().getIntExtra(EXTRA_DEFAULT_POSITION, 0);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null && !title.isEmpty()) {
            TextView titleTv = findViewById(R.id.title_text);
            titleTv.setText(title);
        }

        TextView selectedLabel = findViewById(R.id.selected_label);

        AppBarPositionView picker = findViewById(R.id.position_picker);
        int current = prefs.getInt(positionPrefKey, defaultPosition);
        if (current < 0 || current > 8) {
            current = defaultPosition;
        }
        picker.setSelectedPosition(current);
        selectedLabel.setText(POSITION_LABELS[current]);

        picker.setOnPositionSelectedListener((position, label) -> {
            prefs.edit().putInt(positionPrefKey, position).apply();
            selectedLabel.setText(label);
        });
    }
}
