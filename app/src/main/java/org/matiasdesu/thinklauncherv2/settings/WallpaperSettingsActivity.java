package org.matiasdesu.thinklauncherv2.settings;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.WallpaperHelper;
import org.matiasdesu.thinklauncherv2.views.WallpaperPositionView;

import java.io.InputStream;

public class WallpaperSettingsActivity extends AppCompatActivity {

    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;
    private WallpaperPositionView wallpaperPositionView;
    private LinearLayout positionContainer;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(WallpaperSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!ThemeUtils.isDarkTheme(theme, this)) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(0);
            }
        }

        rootLayout = findViewById(R.id.root_layout);
        rootLayout.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(rootLayout, theme, this);

        setupActivityResultLaunchers();

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        // Choose Wallpaper button
        LinearLayout chooseWallpaperButton = findViewById(R.id.choose_wallpaper_button);
        chooseWallpaperButton.setOnClickListener(v -> openImagePicker());
        
        // Apply theme to Select button
        TextView selectButtonText = findViewById(R.id.select_button_text);
        ThemeUtils.applyButtonTheme(selectButtonText, theme, this);

        // Remove Wallpaper button
        LinearLayout removeWallpaperButton = findViewById(R.id.remove_wallpaper_button);
        removeWallpaperButton.setOnClickListener(v -> removeWallpaper());
        
        // Apply theme to Remove button
        TextView removeButtonText = findViewById(R.id.remove_button_text);
        ThemeUtils.applyButtonTheme(removeButtonText, theme, this);

        // Position selector container (must be initialized before updateWallpaperStatus)
        positionContainer = findViewById(R.id.position_container);
        wallpaperPositionView = findViewById(R.id.wallpaper_position_view);
        
        // Update status after positionContainer is initialized
        updateWallpaperStatus();
        
        // Load saved position
        float savedOffsetX = prefs.getFloat("wallpaper_offset_x", 0.5f);
        float savedOffsetY = prefs.getFloat("wallpaper_offset_y", 0.5f);
        wallpaperPositionView.setPosition(savedOffsetX, savedOffsetY);
        
        // Save position when changed
        wallpaperPositionView.setOnPositionChangedListener(new WallpaperPositionView.OnPositionChangedListener() {
            @Override
            public void onPositionChanged(float offsetX, float offsetY) {
                // Just update local variables if needed, or do nothing here for performance
            }

            @Override
            public void onPositionChangeFinished(float offsetX, float offsetY) {
                prefs.edit()
                    .putFloat("wallpaper_offset_x", offsetX)
                    .putFloat("wallpaper_offset_y", offsetY)
                    .apply();
            }
        });

        // Load wallpaper preview if exists
        loadWallpaperPreview();

        // Initialize pagination
        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::updateWallpaperStatus);
    }

    private void setupActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        saveWallpaper(imageUri);
                    }
                }
            }
        );

        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchImagePicker();
                } else {
                    Toast.makeText(this, "Permission required to select wallpaper", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses photo picker, no permission needed
            launchImagePicker();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            launchImagePicker();
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveWallpaper(Uri imageUri) {
        try {
            // Copy the image to internal storage
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap != null) {
                    WallpaperHelper.saveWallpaper(this, bitmap);
                    
                    // Update UI
                    loadWallpaperPreview();
                    updateWallpaperStatus();
                    
                    Toast.makeText(this, "Wallpaper set successfully", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to set wallpaper", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void removeWallpaper() {
        WallpaperHelper.removeWallpaper(this);
        
        // Reset position
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit()
            .putFloat("wallpaper_offset_x", 0.5f)
            .putFloat("wallpaper_offset_y", 0.5f)
            .apply();
        wallpaperPositionView.setPosition(0.5f, 0.5f);
        wallpaperPositionView.setWallpaperBitmap(null);
        
        // Update UI
        updateWallpaperStatus();
        
        Toast.makeText(this, "Wallpaper removed", Toast.LENGTH_SHORT).show();
    }

    private void updateWallpaperStatus() {
        boolean hasWallpaper = WallpaperHelper.hasWallpaper(this);
        positionContainer.setVisibility(hasWallpaper ? View.VISIBLE : View.GONE);
    }

    private void loadWallpaperPreview() {
        Bitmap wallpaper = WallpaperHelper.loadWallpaper(this);
        if (wallpaper != null) {
            wallpaperPositionView.setWallpaperBitmap(wallpaper);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), Context.RECEIVER_NOT_EXPORTED);
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }
}
