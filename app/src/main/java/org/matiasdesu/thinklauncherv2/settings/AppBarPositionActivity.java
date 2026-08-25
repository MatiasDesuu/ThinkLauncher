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
    public static final String EXTRA_PICKER_MODE = "picker_mode";

    public static final int PICKER_MODE_GRID9 = AppBarPositionView.MODE_GRID9;
    public static final int PICKER_MODE_CROSS4 = AppBarPositionView.MODE_CROSS4;

    private static final String[] POSITION_LABELS = {
            "Top Left", "Top Right", "Bottom Left", "Bottom Right",
            "Center Left", "Center Right", "Top Center", "Bottom Center", "Center"
    };

    private static final String[] CROSS4_POSITION_LABELS = { "Top", "Right", "Bottom", "Left" };

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

        int mode = getIntent().getIntExtra(EXTRA_PICKER_MODE, PICKER_MODE_GRID9);
        String[] labels = mode == PICKER_MODE_CROSS4 ? CROSS4_POSITION_LABELS : POSITION_LABELS;

        AppBarPositionView picker = findViewById(R.id.position_picker);
        picker.setMode(mode);
        int current = prefs.getInt(positionPrefKey, defaultPosition);
        if (current < 0 || current >= labels.length) {
            current = (defaultPosition >= 0 && defaultPosition < labels.length)
                    ? defaultPosition : 0;
        }
        picker.setSelectedPosition(current);
        selectedLabel.setText(labels[current]);

        picker.setOnPositionSelectedListener((position, label) -> {
            prefs.edit().putInt(positionPrefKey, position).apply();
            selectedLabel.setText(label);
        });
    }
}
