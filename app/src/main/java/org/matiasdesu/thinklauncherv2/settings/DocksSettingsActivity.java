package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class DocksSettingsActivity extends BaseSettingsActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_docks_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        LinearLayout floatingDockButton = findViewById(R.id.floating_dock_button);
        floatingDockButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppBarSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout bottomDockButton = findViewById(R.id.bottom_dock_button);
        bottomDockButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DockSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        LinearLayout musicDockButton = findViewById(R.id.music_dock_button);
        musicDockButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MusicDockSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        initPagination(null);
    }
}