package org.matiasdesu.thinklauncherv2.ui;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryViewerActivity extends AppCompatActivity {

    private int theme;
    private boolean appLauncherAnimations;
    private boolean galleryAnimation;
    private SharedPreferences prefs;
    private ViewPager2 viewPager;
    private GalleryPagerAdapter adapter;
    private TextView pageIndicator;
    private TextView imageNameView;

    private ArrayList<Long> imageIds;
    private ArrayList<String> imageNames;
    private int currentIndex;
    private long currentImageId;

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private boolean imageDeleted = false;

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
        galleryAnimation = prefs.getBoolean("gallery_animation", false);
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

        viewPager = findViewById(R.id.gallery_view_pager);
        pageIndicator = findViewById(R.id.page_indicator);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);

        imageNameView = findViewById(R.id.image_name);
        ThemeUtils.applyTextColor(imageNameView, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> onBackPressed());

        ImageView deleteButton = findViewById(R.id.delete_button);
        deleteButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        deleteButton.setOnClickListener(v -> confirmDelete());

        ImageView shareButton = findViewById(R.id.share_button);
        shareButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        shareButton.setOnClickListener(v -> shareCurrentImage());

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

        // Set up ViewPager2 with adapter
        adapter = new GalleryPagerAdapter(imageIds, loadExecutor);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentIndex, false);

        viewPager.setUserInputEnabled(galleryAnimation);

        updateCounter();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                currentImageId = imageIds.get(currentIndex);
                updateCounter();
            }
        });

        findViewById(R.id.prev_page_button).setOnClickListener(v -> navigatePrevious());
        findViewById(R.id.next_page_button).setOnClickListener(v -> navigateNext());
    }

    private void navigateNext() {
        if (currentIndex < imageIds.size() - 1) {
            viewPager.setCurrentItem(currentIndex + 1, galleryAnimation);
        }
    }

    private void navigatePrevious() {
        if (currentIndex > 0) {
            viewPager.setCurrentItem(currentIndex - 1, galleryAnimation);
        }
    }

    private void shareCurrentImage() {
        try {
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, currentImageId);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCounter() {
        if (imageIds.isEmpty()) return;
        String text = (currentIndex + 1) + " / " + imageIds.size();
        pageIndicator.setText(text);

        if (imageNames != null && currentIndex < imageNames.size()) {
            imageNameView.setText(imageNames.get(currentIndex));
        } else {
            imageNameView.setText("");
        }
    }

    private void confirmDelete() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestManageStoragePermission();
            return;
        }
        new DeleteImageDialog(this, this::deleteCurrentImage).show();
    }

    private void requestManageStoragePermission() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
        Toast.makeText(this, "Grant file access permission to delete images", Toast.LENGTH_LONG).show();
    }

    private void deleteCurrentImage() {
        try {
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, currentImageId);
            getContentResolver().delete(uri, null, null);
            imageDeleted = true;

            int deletedIndex = currentIndex;
            imageIds.remove(deletedIndex);
            if (imageNames != null && deletedIndex < imageNames.size()) {
                imageNames.remove(deletedIndex);
            }

            if (imageIds.isEmpty()) {
                setResult(RESULT_OK);
                Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (currentIndex >= imageIds.size()) {
                currentIndex = imageIds.size() - 1;
            }

            adapter.notifyItemRemoved(deletedIndex);
            adapter.notifyItemRangeChanged(currentIndex, imageIds.size() - currentIndex);

            // Post to ensure ViewPager2 updates after adapter notification
            viewPager.post(() -> {
                viewPager.setCurrentItem(currentIndex, false);
                updateCounter();
            });

            Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (imageDeleted) {
            setResult(RESULT_OK);
        }
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
