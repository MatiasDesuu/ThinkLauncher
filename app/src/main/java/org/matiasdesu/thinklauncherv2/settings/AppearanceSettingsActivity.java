package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class AppearanceSettingsActivity extends BaseSettingsActivity {

    private int customBgColor;
    private int customAccentColor;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_appearance_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        customAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        findViewById(R.id.theme_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemeSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.display_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, DisplaySettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.fonts_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, FontsSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        initPagination(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int newTheme = prefs.getInt("theme", 0);
        int newCustomBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        int newCustomAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);

        if (newTheme != theme || (newTheme == ThemeUtils.THEME_CUSTOM &&
                (newCustomBgColor != customBgColor || newCustomAccentColor != customAccentColor))) {
            restartActivity();
        }
    }

    private void restartActivity() {
        Intent intent = new Intent(this, AppearanceSettingsActivity.class);
        if (!screenAnimations) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
    }
}
