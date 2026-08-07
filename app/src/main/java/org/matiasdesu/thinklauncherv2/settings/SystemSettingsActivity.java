package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class SystemSettingsActivity extends BaseSettingsActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_system_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        findViewById(R.id.default_launcher_button).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.accessibility_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.device_admin_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.DeviceAdminSettings");
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.app_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.github_repo_button).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/MatiasDesuu/ThinkLauncher"));
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        initPagination(null);
    }
}
