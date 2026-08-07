package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class ControlsSettingsActivity extends BaseSettingsActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_controls_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        findViewById(R.id.gesture_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, GestureSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.hardware_keys_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, HardwareKeysSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.settings_button_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsButtonSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.search_button_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchButtonSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        initPagination(null);
    }
}
