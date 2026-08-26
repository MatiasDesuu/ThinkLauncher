package org.matiasdesu.thinklauncherv2.ui;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.media.ExifInterface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.views.ZoomableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryViewerActivity extends AppCompatActivity {

    private int theme;
    private boolean appLauncherAnimations;
    private SharedPreferences prefs;
    private ZoomableImageView imageView;
    private TextView pageIndicator;

    private ArrayList<Long> imageIds;
    private ArrayList<String> imageNames;
    private int currentIndex;
    private long currentImageId;

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(GalleryViewerActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        appLauncherAnimations = prefs.getInt("screen_animations", 0) == 1;
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme,
                prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_viewer);

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme,
                prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1);

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        imageView = findViewById(R.id.gallery_image_view);
        pageIndicator = findViewById(R.id.page_indicator);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        ImageView deleteButton = findViewById(R.id.delete_button);
        deleteButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        deleteButton.setOnClickListener(v -> confirmDelete());

        long[] idsArray = getIntent().getLongArrayExtra("image_ids");
        String[] namesArray = getIntent().getStringArrayExtra("image_names");
        if (idsArray != null) {
            imageIds = new ArrayList<>();
            for (long id : idsArray) imageIds.add(id);
        }
        if (namesArray != null) {
            imageNames = new ArrayList<>();
            for (String n : namesArray) imageNames.add(n);
        }
        currentIndex = getIntent().getIntExtra("current_index", 0);

        if (imageIds == null || imageIds.isEmpty()) {
            Toast.makeText(this, "No image data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageView.setOnSwipeListener(new ZoomableImageView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                navigateNext();
            }

            @Override
            public void onSwipeRight() {
                navigatePrevious();
            }
        });

        findViewById(R.id.prev_page_button).setOnClickListener(v -> navigatePrevious());
        findViewById(R.id.next_page_button).setOnClickListener(v -> navigateNext());

        loadCurrentImage();
    }

    private void navigateNext() {
        if (currentIndex < imageIds.size() - 1) {
            currentIndex++;
            loadCurrentImage();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    private void navigatePrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            loadCurrentImage();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    private void loadCurrentImage() {
        currentImageId = imageIds.get(currentIndex);
        updateCounter();

        loadExecutor.execute(() -> {
            try {
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, currentImageId);
                Bitmap rawBitmap = BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(uri));
                final Bitmap bitmap = (rawBitmap != null) ? applyExifOrientation(uri, rawBitmap) : null;
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.resetZoom();
                    } else {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateCounter() {
        String text = (currentIndex + 1) + " / " + imageIds.size();
        pageIndicator.setText(text);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete image")
                .setMessage("Delete this image?")
                .setPositiveButton("Delete", (d, w) -> deleteCurrentImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCurrentImage() {
        try {
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, currentImageId);
            getContentResolver().delete(uri, null, null);

            imageIds.remove(currentIndex);
            if (imageNames != null && currentIndex < imageNames.size()) {
                imageNames.remove(currentIndex);
            }

            if (imageIds.isEmpty()) {
                Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (currentIndex >= imageIds.size()) {
                currentIndex = imageIds.size() - 1;
            }

            loadCurrentImage();
            Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap applyExifOrientation(Uri uri, Bitmap bitmap) {
        try {
            android.content.ContentResolver resolver = getContentResolver();
            android.database.Cursor cursor = resolver.query(uri,
                    new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            String filePath = null;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(0);
                }
                cursor.close();
            }
            if (filePath == null) return bitmap;

            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotation = 0;
            boolean flip = false;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    flip = true;
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    rotation = 180;
                    flip = true;
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    rotation = 90;
                    flip = true;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    rotation = 270;
                    flip = true;
                    break;
                default:
                    return bitmap;
            }
            Matrix matrix = new Matrix();
            if (rotation != 0) {
                matrix.postRotate(rotation);
            }
            if (flip) {
                matrix.postScale(-1, 1, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
            }
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (Exception e) {
            return bitmap;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontHelper.applyToViewTree(this, findViewById(android.R.id.content));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loadExecutor.shutdownNow();
        try {
            unregisterReceiver(homeButtonReceiver);
        } catch (Exception e) {
        }
    }
}
