package org.matiasdesu.thinklauncherv2.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 4001;
    private static final int REQUEST_VIEWER = 4002;
    private int theme;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private boolean appLauncherAnimations;
    private boolean showWallpaperBackdrop;
    private int gallerySurfaceColor;
    private SharedPreferences prefs;

    private List<GalleryImage> images;
    private GalleryGridAdapter adapter;
    private int itemsPerPage;
    private int currentPage = 0;
    private RecyclerView recyclerView;

    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(2);

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(GalleryActivity.this, MainActivity.class);
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
        opacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1;
        appLauncherAnimations = prefs.getInt("screen_animations", 0) == 1;
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
        gallerySurfaceColor = backdrop.surfaceColor;
        showWallpaperBackdrop = backdrop.showWallpaperBackdrop;

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        TextView titleView = findViewById(R.id.gallery_title);
        ThemeUtils.applyTextColor(titleView, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;

        recyclerView = findViewById(R.id.gallery_grid);
        View topLayout = findViewById(R.id.top_layout);
        View container = findViewById(R.id.app_list_container);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, gallerySurfaceColor,
                topLayout, recyclerView, container);

        images = new ArrayList<>();
        adapter = new GalleryGridAdapter(images);

        int spanCount = calculateSpanCount();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setAdapter(adapter);

        if (!scrollAppList) {
            new SwipePageNavigator(this, recyclerView, container,
                    new SwipePageNavigator.PageChangeCallback() {
                        @Override
                        public void onPageChanged(int newPage) {
                            currentPage = newPage;
                            recyclerView.getAdapter().notifyDataSetChanged();
                            updatePageIndicator();
                            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                        }

                        @Override
                        public int getTotalPages() {
                            return (int) Math.ceil((double) images.size() / itemsPerPage);
                        }

                        @Override
                        public void updatePageIndicator() {
                            GalleryActivity.this.updatePageIndicator();
                        }
                    }, theme);
        }

        updatePageIndicator();
        requestPermissionAndLoad();
    }

    private int calculateSpanCount() {
        float screenWidthDp = getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density;
        if (screenWidthDp < 400) return 2;
        return 3;
    }

    private void requestPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
                return;
            }
        }
        loadImages();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages();
            } else {
                Toast.makeText(this, "Gallery permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void loadImages() {
        thumbnailExecutor.execute(() -> {
            List<GalleryImage> loaded = new ArrayList<>();
            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.SIZE
            };
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        String name = cursor.getString(1);
                        long dateAdded = cursor.getLong(2);
                        long size = cursor.getLong(3);
                        if (name == null || name.isEmpty()) name = "image_" + id;
                        loaded.add(new GalleryImage(id, name, dateAdded, size));
                    }
                }
            } catch (SecurityException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Gallery permission is required", Toast.LENGTH_SHORT).show());
                return;
            }

            runOnUiThread(() -> {
                images.clear();
                images.addAll(loaded);
                itemsPerPage = calculateItemsPerPage();
                currentPage = 0;
                adapter.notifyDataSetChanged();
                updatePageIndicator();
            });
        });
    }

    private int calculateItemsPerPage() {
        float screenHeightDp = getResources().getDisplayMetrics().heightPixels /
                getResources().getDisplayMetrics().density;
        int navBarHeightPx = 0;
        try {
            navBarHeightPx = getResources().getDimensionPixelSize(
                    getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        } catch (Exception e) {
        }
        screenHeightDp -= navBarHeightPx / getResources().getDisplayMetrics().density;

        float topHeightDp = 48;
        float dividerDp = 4;
        float bottomHeightDp = 48;
        float recyclerHeightDp = screenHeightDp - topHeightDp - dividerDp - bottomHeightDp;

        float itemHeightDp = 120 + 20 + 12;
        int spanCount = calculateSpanCount();
        int rowsPerPage = Math.max(1, (int) (recyclerHeightDp / itemHeightDp));
        return rowsPerPage * spanCount;
    }

    private void updatePageIndicator() {
        TextView pageIndicator = findViewById(R.id.page_indicator);
        View bottomDivider = findViewById(R.id.bottom_divider);
        View bottomBar = findViewById(R.id.bottom_bar);
        if (scrollAppList) {
            pageIndicator.setVisibility(View.GONE);
            bottomDivider.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            return;
        }
        pageIndicator.setVisibility(View.VISIBLE);
        bottomDivider.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        int totalPages = (int) Math.ceil((double) images.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private void openImage(int position) {
        if (position < 0 || position >= images.size()) return;

        long[] idsArray = new long[images.size()];
        String[] namesArray = new String[images.size()];
        for (int i = 0; i < images.size(); i++) {
            idsArray[i] = images.get(i).id;
            namesArray[i] = images.get(i).name;
        }

        Intent intent = new Intent(this, GalleryViewerActivity.class);
        intent.putExtra("current_index", position);
        intent.putExtra("image_ids", idsArray);
        intent.putExtra("image_names", namesArray);

        startActivityForResult(intent, REQUEST_VIEWER);
        overridePendingTransition(0, appLauncherAnimations ? 0 : 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIEWER && resultCode == RESULT_OK) {
            loadImages();
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
        thumbnailExecutor.shutdownNow();
        try {
            unregisterReceiver(homeButtonReceiver);
        } catch (Exception e) {
        }
    }

    private static class GalleryImage {
        long id;
        String name;
        long dateAdded;
        long size;

        GalleryImage(long id, String name, long dateAdded, long size) {
            this.id = id;
            this.name = name;
            this.dateAdded = dateAdded;
            this.size = size;
        }
    }

    private class GalleryGridAdapter extends RecyclerView.Adapter<GalleryGridAdapter.ViewHolder> {

        private final List<GalleryImage> items;

        GalleryGridAdapter(List<GalleryImage> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gallery_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            int globalPosition = scrollAppList ? position : currentPage * itemsPerPage + position;
            if (globalPosition >= items.size()) return;

            GalleryImage image = items.get(globalPosition);
            holder.filename.setText(image.name);
            ThemeUtils.applyTextColor(holder.filename, theme, GalleryActivity.this);
            FontHelper.applyToViewTree(GalleryActivity.this, holder.itemView);

            holder.thumbnail.setTag(globalPosition);

            thumbnailExecutor.execute(() -> {
                try {
                    Uri thumbUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image.id);
                    Bitmap thumb = getContentResolver().loadThumbnail(thumbUri,
                            new android.util.Size(200, 200), null);
                    if (thumb != null) {
                        holder.thumbnail.post(() -> {
                            if (Integer.valueOf(globalPosition).equals(holder.thumbnail.getTag())) {
                                holder.thumbnail.setImageBitmap(thumb);
                            }
                        });
                    }
                } catch (Exception e) {
                }
            });

            holder.itemView.setOnClickListener(v -> openImage(globalPosition));
        }

        @Override
        public int getItemCount() {
            if (scrollAppList) {
                return items.size();
            }
            int start = currentPage * itemsPerPage;
            return Math.min(itemsPerPage, items.size() - start);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView filename;

            ViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.gallery_thumbnail);
                filename = itemView.findViewById(R.id.gallery_filename);
            }
        }
    }
}
