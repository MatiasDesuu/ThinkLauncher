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
import org.matiasdesu.thinklauncherv2.utils.RepeatListener;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.WallpaperHelper;
import org.matiasdesu.thinklauncherv2.views.WallpaperPositionView;

import java.io.InputStream;

public class WallpaperSettingsActivity extends AppCompatActivity {

    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;
    private boolean screenAnimations;
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

        screenAnimations = prefs.getInt("screen_animations", 0) == 1;

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
            overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
        });

        LinearLayout chooseWallpaperButton = findViewById(R.id.choose_wallpaper_button);
        chooseWallpaperButton.setOnClickListener(v -> openImagePicker());

        TextView selectButtonText = findViewById(R.id.select_button_text);
        ThemeUtils.applyButtonTheme(selectButtonText, theme, this);

        LinearLayout removeWallpaperButton = findViewById(R.id.remove_wallpaper_button);
        removeWallpaperButton.setOnClickListener(v -> removeWallpaper());

        TextView removeButtonText = findViewById(R.id.remove_button_text);
        ThemeUtils.applyButtonTheme(removeButtonText, theme, this);

        positionContainer = findViewById(R.id.position_container);
        wallpaperPositionView = findViewById(R.id.wallpaper_position_view);

        updateWallpaperStatus();

        float savedOffsetX = prefs.getFloat("wallpaper_offset_x", 0.5f);
        float savedOffsetY = prefs.getFloat("wallpaper_offset_y", 0.5f);
        float savedScale = prefs.getFloat("wallpaper_scale", 1f);
        wallpaperPositionView.setPosition(savedOffsetX, savedOffsetY);
        wallpaperPositionView.setScale(savedScale);

        // Zoom controls (must be declared before listener)
        View zoomContainer = findViewById(R.id.wallpaper_zoom_container);
        TextView zoomValueTv = zoomContainer.findViewById(R.id.value_text);
        zoomValueTv.setText(Math.round(savedScale * 100f) + "%");
        TextView minusZoomBtn = zoomContainer.findViewById(R.id.btn_minus);
        TextView plusZoomBtn = zoomContainer.findViewById(R.id.btn_plus);

        wallpaperPositionView.setOnPositionChangedListener(new WallpaperPositionView.OnPositionChangedListener() {
            @Override
            public void onPositionChanged(float offsetX, float offsetY) {
            }

            @Override
            public void onPositionChangeFinished(float offsetX, float offsetY) {
                prefs.edit()
                        .putFloat("wallpaper_offset_x", offsetX)
                        .putFloat("wallpaper_offset_y", offsetY)
                        .apply();
            }

            @Override
            public void onScaleChanged(float scale) {
                prefs.edit().putFloat("wallpaper_scale", scale).apply();
                zoomValueTv.setText(Math.round(scale * 100f) + "%");
            }
        });

        minusZoomBtn.setOnTouchListener(new RepeatListener(v -> {
            float current = prefs.getFloat("wallpaper_scale", 1f);
            float newScale = Math.max(1f, current - 0.1f);
            newScale = Math.round(newScale * 10f) / 10f;
            prefs.edit().putFloat("wallpaper_scale", newScale).apply();
            zoomValueTv.setText(Math.round(newScale * 100f) + "%");
            wallpaperPositionView.setScale(newScale);
        }));

        plusZoomBtn.setOnTouchListener(new RepeatListener(v -> {
            float current = prefs.getFloat("wallpaper_scale", 1f);
            float newScale = Math.min(3f, current + 0.1f);
            newScale = Math.round(newScale * 10f) / 10f;
            prefs.edit().putFloat("wallpaper_scale", newScale).apply();
            zoomValueTv.setText(Math.round(newScale * 100f) + "%");
            wallpaperPositionView.setScale(newScale);
        }));

        // D-Pad directional nudges
        float nudgeStep = 0.02f;
        View.OnTouchListener moveUp = new RepeatListener(v -> nudgeOffset(0f, -nudgeStep));
        View.OnTouchListener moveDown = new RepeatListener(v -> nudgeOffset(0f, nudgeStep));
        View.OnTouchListener moveLeft = new RepeatListener(v -> nudgeOffset(-nudgeStep, 0f));
        View.OnTouchListener moveRight = new RepeatListener(v -> nudgeOffset(nudgeStep, 0f));
        findViewById(R.id.btn_move_up).setOnTouchListener(moveUp);
        findViewById(R.id.btn_move_down).setOnTouchListener(moveDown);
        findViewById(R.id.btn_move_left).setOnTouchListener(moveLeft);
        findViewById(R.id.btn_move_right).setOnTouchListener(moveRight);

        // Reset position button
        LinearLayout resetButton = findViewById(R.id.reset_position_button);
        resetButton.setOnClickListener(v -> {
            prefs.edit()
                    .putFloat("wallpaper_offset_x", 0.5f)
                    .putFloat("wallpaper_offset_y", 0.5f)
                    .putFloat("wallpaper_scale", 1f)
                    .apply();
            wallpaperPositionView.setPosition(0.5f, 0.5f);
            wallpaperPositionView.setScale(1f);
            zoomValueTv.setText("100%");
            loadWallpaperPreview();
        });

        loadWallpaperPreview();

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
                });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchImagePicker();
                    } else {
                        Toast.makeText(this, "Permission required to select wallpaper", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            launchImagePicker();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            launchImagePicker();
        }
    }

    private void launchImagePicker() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Intent pickIntent = new Intent(Intent.ACTION_PICK);
                pickIntent.setType("image/*");
                imagePickerLauncher.launch(pickIntent);
            } catch (Exception e2) {
                e2.printStackTrace();
                Toast.makeText(this, "No app found to select images. Please install a gallery or file manager.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveWallpaper(Uri imageUri) {
        Toast.makeText(this, "Processing wallpaper...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    if (bitmap != null) {
                        WallpaperHelper.saveWallpaper(this, bitmap);

                        runOnUiThread(() -> {
                            loadWallpaperPreview();
                            updateWallpaperStatus();
                            Toast.makeText(this, "Wallpaper set successfully", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to set wallpaper", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void removeWallpaper() {
        WallpaperHelper.removeWallpaper(this);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit()
                .putFloat("wallpaper_offset_x", 0.5f)
                .putFloat("wallpaper_offset_y", 0.5f)
                .putFloat("wallpaper_scale", 1f)
                .apply();
        wallpaperPositionView.setPosition(0.5f, 0.5f);
        wallpaperPositionView.setScale(1f);
        wallpaperPositionView.setWallpaperBitmap(null);

        updateWallpaperStatus();

        Toast.makeText(this, "Wallpaper removed", Toast.LENGTH_SHORT).show();
    }

    private void nudgeOffset(float dx, float dy) {
        SharedPreferences p = getSharedPreferences("prefs", MODE_PRIVATE);
        float curX = p.getFloat("wallpaper_offset_x", 0.5f);
        float curY = p.getFloat("wallpaper_offset_y", 0.5f);
        float newX = Math.max(0f, Math.min(1f, curX + dx));
        float newY = Math.max(0f, Math.min(1f, curY + dy));
        if (newX != curX || newY != curY) {
            p.edit()
                    .putFloat("wallpaper_offset_x", newX)
                    .putFloat("wallpaper_offset_y", newY)
                    .apply();
            wallpaperPositionView.setPosition(newX, newY);
        }
    }

    private void updateWallpaperStatus() {
        boolean hasWallpaper = WallpaperHelper.hasWallpaper(this);
        positionContainer.setVisibility(hasWallpaper ? View.VISIBLE : View.GONE);
    }

    private void loadWallpaperPreview() {
        new Thread(() -> {
            Bitmap wallpaper = WallpaperHelper.loadWallpaper(this);
            if (wallpaper != null) {
                runOnUiThread(() -> {
                    wallpaperPositionView.setWallpaperBitmap(wallpaper);
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);
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
        finish();
        overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
    }
}
