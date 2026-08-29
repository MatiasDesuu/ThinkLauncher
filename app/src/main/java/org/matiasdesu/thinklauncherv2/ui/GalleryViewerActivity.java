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
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryFavoritesHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryHiddenHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryTrashHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
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
    private ArrayList<Integer> mediaTypes;
    private ArrayList<Long> imageDates;
    private int currentIndex;
    private long currentImageId;
    private int currentMediaType;
    private boolean isTrashMode;
    private boolean isFavoritesMode;
    private boolean isHiddenMode;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private boolean imageDeleted = false;
    private boolean favoritesChanged = false;
    private ImageView favoriteButton;
    private static final int REQ_VIDEO_FULLSCREEN = 9001;

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
        imageNameView.setOnClickListener(v -> {
            String n = (imageNames != null && currentIndex < imageNames.size()) ? imageNames.get(currentIndex) : "";
            long d = (imageDates != null && currentIndex < imageDates.size()) ? imageDates.get(currentIndex) : 0;
            new GalleryDetailsDialog(this, currentImageId, currentMediaType, n, d, 0, null).show();
        });

        isTrashMode = getIntent().getBooleanExtra("trash_mode", false);
        isFavoritesMode = getIntent().getBooleanExtra("favorites_mode", false);
        isHiddenMode = getIntent().getBooleanExtra("hidden_mode", false);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> onBackPressed());

        ImageView deleteButton = findViewById(R.id.delete_button);
        deleteButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        ImageView restoreButton = findViewById(R.id.restore_button);
        restoreButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        deleteButton.setVisibility(View.GONE);
        restoreButton.setVisibility(View.GONE);
        favoriteButton = findViewById(R.id.favorite_button);
        if (favoriteButton != null) favoriteButton.setVisibility(View.GONE);

        ImageView moreButton = findViewById(R.id.more_button);
        if (moreButton != null) {
            moreButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
            moreButton.setOnClickListener(v -> showOptionsDialog());
        }

        ImageView shareButton = findViewById(R.id.share_button);
        shareButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        shareButton.setOnClickListener(v -> shareCurrentImage());

        long[] idsArray = getIntent().getLongArrayExtra("image_ids");
        String[] namesArray = getIntent().getStringArrayExtra("image_names");
        int[] typesArray = getIntent().getIntArrayExtra("media_types");
        long[] datesArray = getIntent().getLongArrayExtra("image_dates");
        if (idsArray != null) {
            imageIds = new ArrayList<>();
            for (long id : idsArray) imageIds.add(id);
        }
        if (namesArray != null) {
            imageNames = new ArrayList<>();
            for (String n : namesArray) imageNames.add(n);
        }
        mediaTypes = new ArrayList<>();
        if (typesArray != null) {
            for (int t : typesArray) mediaTypes.add(t);
        } else if (imageIds != null) {
            for (int i = 0; i < imageIds.size(); i++) mediaTypes.add(GalleryTrashHelper.TYPE_IMAGE);
        }
        imageDates = new ArrayList<>();
        if (datesArray != null) {
            for (long d : datesArray) imageDates.add(d);
        } else if (imageIds != null) {
            for (int i = 0; i < imageIds.size(); i++) imageDates.add(0L);
        }
        currentIndex = getIntent().getIntExtra("current_index", 0);

        if (imageIds == null || imageIds.isEmpty()) {
            Toast.makeText(this, "No image data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (currentIndex >= 0 && currentIndex < imageIds.size()) {
            currentImageId = imageIds.get(currentIndex);
            currentMediaType = currentIndex < mediaTypes.size() ? mediaTypes.get(currentIndex) : GalleryTrashHelper.TYPE_IMAGE;
        }

        adapter = new GalleryPagerAdapter(imageIds, mediaTypes, loadExecutor, (vid, pos, playing) -> {
            Intent intent = new Intent(this, VideoFullscreenActivity.class);
            intent.putExtra("video_id", vid);
            intent.putExtra("position", pos);
            intent.putExtra("is_playing", playing);
            startActivityForResult(intent, REQ_VIDEO_FULLSCREEN);
            overridePendingTransition(0, 0);
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentIndex, false);

        viewPager.setUserInputEnabled(galleryAnimation);

        updateCounter();
        updateFavoriteIcon();
        viewPager.post(() -> updateFavoriteIcon());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                currentImageId = imageIds.get(currentIndex);
                currentMediaType = position < mediaTypes.size() ? mediaTypes.get(position) : GalleryTrashHelper.TYPE_IMAGE;
                updateCounter();
                updateFavoriteIcon();
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
            Uri baseUri = currentMediaType == GalleryTrashHelper.TYPE_VIDEO
                    ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri uri = ContentUris.withAppendedId(baseUri, currentImageId);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(currentMediaType == GalleryTrashHelper.TYPE_VIDEO ? "video/*" : "image/*");
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
        if (imageDates != null && currentIndex < imageDates.size()) {
            long d = imageDates.get(currentIndex);
            if (d != 0) imageNameView.setText(dateFormat.format(new Date(d * 1000)));
            else if (imageNames != null && currentIndex < imageNames.size()) imageNameView.setText(imageNames.get(currentIndex));
            else imageNameView.setText("");
        } else if (imageNames != null && currentIndex < imageNames.size()) {
            imageNameView.setText(imageNames.get(currentIndex));
        } else {
            imageNameView.setText("");
        }
    }

    private void updateFavoriteIcon() {
        if (favoriteButton == null) return;
        SharedPreferences fp = getSharedPreferences("prefs", MODE_PRIVATE);
        java.util.Set<String> s = fp.getStringSet("gallery_favorite_ids", null);
        boolean fav = s != null && (s.contains("image:" + currentImageId) || s.contains("video:" + currentImageId) || s.contains(String.valueOf(currentImageId)));
        if (isFavoritesMode) fav = true;
        favoriteButton.setImageResource(fav ? R.drawable.star_filled : R.drawable.star_outline);
        favoriteButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        favoriteButton.setContentDescription(fav ? "Unfavorite" : "Favorite");
    }

    private void toggleFavorite() {
        SharedPreferences fp = getSharedPreferences("prefs", MODE_PRIVATE);
        java.util.Set<String> s = fp.getStringSet("gallery_favorite_ids", null);
        boolean wasFav = s != null && (s.contains("image:" + currentImageId) || s.contains("video:" + currentImageId) || s.contains(String.valueOf(currentImageId)));
        java.util.Set<String> cur = fp.getStringSet("gallery_favorite_ids", null);
        java.util.Set<String> upd = cur == null ? new java.util.HashSet<>() : new java.util.HashSet<>(cur);
        if (wasFav) {
            upd.remove("image:" + currentImageId);
            upd.remove("video:" + currentImageId);
            upd.remove(String.valueOf(currentImageId));
        } else {
            upd.add(currentMediaType == GalleryTrashHelper.TYPE_VIDEO ? "video:" + currentImageId : "image:" + currentImageId);
        }
        fp.edit().putStringSet("gallery_favorite_ids", upd).apply();
        favoritesChanged = true;
        updateFavoriteIcon();
        if (isFavoritesMode && wasFav) {
            imageDeleted = true;
            int removedIndex = currentIndex;
            imageIds.remove(removedIndex);
            if (imageNames != null && removedIndex < imageNames.size()) imageNames.remove(removedIndex);
            if (mediaTypes != null && removedIndex < mediaTypes.size()) mediaTypes.remove(removedIndex);
            if (imageDates != null && removedIndex < imageDates.size()) imageDates.remove(removedIndex);
            if (imageIds.isEmpty()) {
                setResult(RESULT_OK);
                finish();
                return;
            }
            if (currentIndex >= imageIds.size()) currentIndex = imageIds.size() - 1;
            if (currentIndex >= 0 && currentIndex < imageIds.size()) {
                currentImageId = imageIds.get(currentIndex);
                currentMediaType = currentIndex < mediaTypes.size() ? mediaTypes.get(currentIndex) : GalleryTrashHelper.TYPE_IMAGE;
            }
            adapter.notifyItemRemoved(removedIndex);
            adapter.notifyItemRangeChanged(currentIndex, imageIds.size() - currentIndex);
            viewPager.post(() -> {
                viewPager.setCurrentItem(currentIndex, false);
                updateCounter();
                updateFavoriteIcon();
            });
        }
    }

    private void toggleHidden() {
        boolean wasHidden = GalleryHiddenHelper.isHidden(this, currentImageId, currentMediaType);
        if (wasHidden) GalleryHiddenHelper.unhide(this, currentImageId, currentMediaType);
        else {
            GalleryHiddenHelper.hide(this, currentImageId, currentMediaType);
            GalleryFavoritesHelper.removeFavorite(this, currentImageId, currentMediaType);
        }
        favoritesChanged = true;
        if (wasHidden && isHiddenMode) {
            imageDeleted = true;
            int removedIndex = currentIndex;
            imageIds.remove(removedIndex);
            if (imageNames != null && removedIndex < imageNames.size()) imageNames.remove(removedIndex);
            if (mediaTypes != null && removedIndex < mediaTypes.size()) mediaTypes.remove(removedIndex);
            if (imageDates != null && removedIndex < imageDates.size()) imageDates.remove(removedIndex);
            if (imageIds.isEmpty()) {
                setResult(RESULT_OK);
                finish();
                return;
            }
            if (currentIndex >= imageIds.size()) currentIndex = imageIds.size() - 1;
            if (currentIndex >= 0 && currentIndex < imageIds.size()) {
                currentImageId = imageIds.get(currentIndex);
                currentMediaType = currentIndex < mediaTypes.size() ? mediaTypes.get(currentIndex) : GalleryTrashHelper.TYPE_IMAGE;
            }
            adapter.notifyItemRemoved(removedIndex);
            adapter.notifyItemRangeChanged(currentIndex, imageIds.size() - currentIndex);
            viewPager.post(() -> {
                viewPager.setCurrentItem(currentIndex, false);
                updateCounter();
            });
        } else if (!wasHidden && !isTrashMode && !isHiddenMode) {
            imageDeleted = true;
            int removedIndex = currentIndex;
            imageIds.remove(removedIndex);
            if (imageNames != null && removedIndex < imageNames.size()) imageNames.remove(removedIndex);
            if (mediaTypes != null && removedIndex < mediaTypes.size()) mediaTypes.remove(removedIndex);
            if (imageDates != null && removedIndex < imageDates.size()) imageDates.remove(removedIndex);
            if (imageIds.isEmpty()) {
                setResult(RESULT_OK);
                finish();
                return;
            }
            if (currentIndex >= imageIds.size()) currentIndex = imageIds.size() - 1;
            if (currentIndex >= 0 && currentIndex < imageIds.size()) {
                currentImageId = imageIds.get(currentIndex);
                currentMediaType = currentIndex < mediaTypes.size() ? mediaTypes.get(currentIndex) : GalleryTrashHelper.TYPE_IMAGE;
            }
            adapter.notifyItemRemoved(removedIndex);
            adapter.notifyItemRangeChanged(currentIndex, imageIds.size() - currentIndex);
            viewPager.post(() -> {
                viewPager.setCurrentItem(currentIndex, false);
                updateCounter();
            });
        }
    }

    private void showOptionsDialog() {
        new GalleryViewerOptionsDialog(this, currentImageId, currentMediaType, isTrashMode, isHiddenMode,
                this::toggleFavorite,
                this::toggleHidden,
                () -> {
                    if (isTrashMode) confirmPermanentDelete();
                    else moveToTrash();
                }).show();
    }

    private void confirmPermanentDelete() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestManageStoragePermission();
            return;
        }
        new DeleteImageDialog(this, "Delete permanently? This cannot be undone.", "Delete", this::deletePermanently).show();
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

    private void moveToTrash() {
        try {
            GalleryTrashHelper.moveToTrash(this, currentImageId, currentMediaType);
            GalleryHiddenHelper.unhide(this, currentImageId, currentMediaType);
            GalleryFavoritesHelper.removeFavorite(this, currentImageId, currentMediaType);
            imageDeleted = true;
            int removedIndex = currentIndex;
            imageIds.remove(removedIndex);
            if (imageNames != null && removedIndex < imageNames.size()) imageNames.remove(removedIndex);
            if (mediaTypes != null && removedIndex < mediaTypes.size()) mediaTypes.remove(removedIndex);
            if (imageDates != null && removedIndex < imageDates.size()) imageDates.remove(removedIndex);
            if (imageIds.isEmpty()) {
                setResult(RESULT_OK);
                finish();
                return;
            }
            if (currentIndex >= imageIds.size()) currentIndex = imageIds.size() - 1;
            if (currentIndex >= 0 && currentIndex < imageIds.size()) {
                currentImageId = imageIds.get(currentIndex);
                currentMediaType = currentIndex < mediaTypes.size() ? mediaTypes.get(currentIndex) : GalleryTrashHelper.TYPE_IMAGE;
            }
            adapter.notifyItemRemoved(removedIndex);
            adapter.notifyItemRangeChanged(currentIndex, imageIds.size() - currentIndex);
            viewPager.post(() -> {
                viewPager.setCurrentItem(currentIndex, false);
                updateCounter();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to move to trash", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreCurrentImage() {
        try {
            GalleryTrashHelper.restore(this, currentImageId, currentMediaType);
            imageDeleted = true;
            int removedIndex = currentIndex;
            imageIds.remove(removedIndex);
            if (imageNames != null && removedIndex < imageNames.size()) imageNames.remove(removedIndex);
            if (mediaTypes != null && removedIndex < mediaTypes.size()) mediaTypes.remove(removedIndex);
            if (imageDates != null && removedIndex < imageDates.size()) imageDates.remove(removedIndex);
            if (imageIds.isEmpty()) {
                setResult(RESULT_OK);
                finish();
                return;
            }
            if (currentIndex >= imageIds.size()) currentIndex = imageIds.size() - 1;
            if (currentIndex >= 0 && currentIndex < imageIds.size()) {
                currentImageId = imageIds.get(currentIndex);
                currentMediaType = currentIndex < mediaTypes.size() ? mediaTypes.get(currentIndex) : GalleryTrashHelper.TYPE_IMAGE;
            }
            adapter.notifyItemRemoved(removedIndex);
            adapter.notifyItemRangeChanged(currentIndex, imageIds.size() - currentIndex);
            viewPager.post(() -> {
                viewPager.setCurrentItem(currentIndex, false);
                updateCounter();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to restore image", Toast.LENGTH_SHORT).show();
        }
    }

    private void deletePermanently() {
        try {
            Uri baseUri = currentMediaType == GalleryTrashHelper.TYPE_VIDEO
                    ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri uri = ContentUris.withAppendedId(baseUri, currentImageId);
            getContentResolver().delete(uri, null, null);
            GalleryTrashHelper.removeFromTrash(this, currentImageId, currentMediaType);
            imageDeleted = true;
            int deletedIndex = currentIndex;
            imageIds.remove(deletedIndex);
            if (imageNames != null && deletedIndex < imageNames.size()) imageNames.remove(deletedIndex);
            if (mediaTypes != null && deletedIndex < mediaTypes.size()) mediaTypes.remove(deletedIndex);
            if (imageDates != null && deletedIndex < imageDates.size()) imageDates.remove(deletedIndex);
            if (imageIds.isEmpty()) {
                setResult(RESULT_OK);
                finish();
                return;
            }
            if (currentIndex >= imageIds.size()) currentIndex = imageIds.size() - 1;
            if (currentIndex >= 0 && currentIndex < imageIds.size()) {
                currentImageId = imageIds.get(currentIndex);
                currentMediaType = currentIndex < mediaTypes.size() ? mediaTypes.get(currentIndex) : GalleryTrashHelper.TYPE_IMAGE;
            }
            adapter.notifyItemRemoved(deletedIndex);
            adapter.notifyItemRangeChanged(currentIndex, imageIds.size() - currentIndex);
            viewPager.post(() -> {
                viewPager.setCurrentItem(currentIndex, false);
                updateCounter();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (imageDeleted || favoritesChanged) {
            setResult(RESULT_OK);
        }
        finish();
        overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VIDEO_FULLSCREEN && resultCode == RESULT_OK && data != null) {
            long vid = data.getLongExtra("video_id", -1);
            int pos = data.getIntExtra("position", 0);
            boolean playing = data.getBooleanExtra("is_playing", false);
            if (vid != -1) {
                int targetIdx = -1;
                if (imageIds != null) {
                    for (int i = 0; i < imageIds.size(); i++) {
                        if (imageIds.get(i) == vid) { targetIdx = i; break; }
                    }
                }
                final int idx = targetIdx != -1 ? targetIdx : currentIndex;
                viewPager.post(() -> {
                    try {
                        RecyclerView rv = (RecyclerView) viewPager.getChildAt(0);
                        RecyclerView.ViewHolder vh = null;
                        if (rv != null) vh = rv.findViewHolderForAdapterPosition(idx);
                        if (vh instanceof GalleryPagerAdapter.PageViewHolder) {
                            ((GalleryPagerAdapter.PageViewHolder) vh).restoreFromFullscreen(pos, playing);
                        } else {
                            if (adapter != null) {
                                adapter.setPendingFullscreenResult(vid, pos, playing);
                                adapter.notifyItemChanged(idx);
                            }
                        }
                    } catch (Exception ignored) {}
                });
            }
        }
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
