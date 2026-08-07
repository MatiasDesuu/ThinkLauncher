package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class HomeScreenSettingsActivity extends BaseSettingsActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_home_screen_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        findViewById(R.id.home_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.time_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, TimeSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.date_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, DateSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.text_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, TextSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.icon_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, IconSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.wallpaper_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, WallpaperSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.home_screen_padding_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeScreenPaddingActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        initPagination(null);
    }
}
