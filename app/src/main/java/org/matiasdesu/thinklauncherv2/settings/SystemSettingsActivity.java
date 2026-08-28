package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.ResetAllConfigDialog;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsBackupHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.WallpaperHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SystemSettingsActivity extends BaseSettingsActivity {

    private static final int REQUEST_EXPORT = 5001;
    private static final int REQUEST_IMPORT = 5002;

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

        findViewById(R.id.notification_permission_button).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                if (!screenAnimations) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                if (!screenAnimations) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }
                startActivity(intent);
            }
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.exact_alarm_permission_button).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                if (!screenAnimations) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                if (!screenAnimations) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }
                startActivity(intent);
            }
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        findViewById(R.id.files_access_button).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
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

        findViewById(R.id.export_config_button).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "thinklauncher_config.json");
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivityForResult(intent, REQUEST_EXPORT);
        });

        findViewById(R.id.import_config_button).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivityForResult(intent, REQUEST_IMPORT);
        });

        findViewById(R.id.reset_all_config_button).setOnClickListener(v -> {
            new ResetAllConfigDialog(this, () -> {
                prefs.edit().clear().apply();
                SettingsBackupHelper.applyInitialDefaults(this);
                FontHelper.removeFont(this);
                WallpaperHelper.removeWallpaper(this);
                new java.io.File(getFilesDir(), "custom_gestures").delete();
                Toast.makeText(this, "Configuration reset", Toast.LENGTH_SHORT).show();
                finish();
            }).show();
        });

        initPagination(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT) {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os == null) {
                    Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                os.write(SettingsBackupHelper.exportToJson(this).getBytes("UTF-8"));
                os.close();
                Toast.makeText(this, "Configuration exported", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_IMPORT) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) {
                    Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int read;
                while ((read = is.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                is.close();
                SettingsBackupHelper.importFromJson(this, buffer.toString("UTF-8"));
                Toast.makeText(this, "Configuration imported", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Import failed: invalid file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
