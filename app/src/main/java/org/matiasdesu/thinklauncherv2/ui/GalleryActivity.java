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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryTrashHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 4001;
    private static final int REQUEST_VIEWER = 4002;
    private static final int REQUEST_TRASH = 4003;
    private int theme;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private boolean appLauncherAnimations;
    private boolean showWallpaperBackdrop;
    private int gallerySurfaceColor;
    private SharedPreferences prefs;

    private List<GalleryImage> images;
    private GalleryAdapter adapter;
    private int itemsPerPage;
    private int currentPage = 0;
    private int restorePage = -1;
    private RecyclerView recyclerView;
    private boolean isGridView;
    private int gridColumns;
    private int gridRows;
    private boolean showGridTitles;
    private boolean isTrashMode;
    private boolean galleryModified;
    private SwipePageNavigator pageNavigator;

    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(2);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

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
        isTrashMode = getIntent().getBooleanExtra("trash_mode", false);
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
        if (isTrashMode) titleView.setText("Trash");

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            if (galleryModified) setResult(RESULT_OK);
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        ImageView toggleViewButton = findViewById(R.id.toggle_view_button);
        toggleViewButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        toggleViewButton.setOnClickListener(v -> toggleView());

        titleView.setOnLongClickListener(v -> {
            new GalleryOptionsDialog(this, gridColumns, gridRows, showGridTitles, isGridView, !scrollAppList, isTrashMode,
                    (columns, rows) -> {
                        gridColumns = columns;
                        gridRows = rows;
                        prefs.edit()
                                .putInt("gallery_grid_columns", columns)
                                .putInt("gallery_grid_rows", rows)
                                .apply();
                        currentPage = 0;
                        itemsPerPage = calculateItemsPerPage();
                        if (isGridView) {
                            recyclerView.setLayoutManager(new GridLayoutManager(this, gridColumns));
                        }
                        if (pageNavigator != null) {
                            pageNavigator.setCurrentPage(0);
                        }
                        adapter.notifyDataSetChanged();
                        updatePageIndicator();
                    },
                    show -> {
                        showGridTitles = show;
                        prefs.edit().putBoolean("gallery_grid_show_titles", show).apply();
                        adapter.notifyDataSetChanged();
                    },
                    () -> {
                        if (isTrashMode) {
                            if (galleryModified) setResult(RESULT_OK);
                            finish();
                            overridePendingTransition(0, 0);
                        } else {
                            Intent intent = new Intent(GalleryActivity.this, GalleryActivity.class);
                            intent.putExtra("trash_mode", true);
                            startActivityForResult(intent, REQUEST_TRASH);
                            overridePendingTransition(0, 0);
                        }
                    }).show();
            return true;
        });

        isGridView = prefs.getBoolean("gallery_grid_view", true);
        gridColumns = prefs.getInt("gallery_grid_columns", 3);
        gridRows = prefs.getInt("gallery_grid_rows", 3);
        showGridTitles = prefs.getBoolean("gallery_grid_show_titles", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;
        toggleViewButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);

        recyclerView = findViewById(R.id.gallery_grid);
        View topLayout = findViewById(R.id.top_layout);
        View container = findViewById(R.id.app_list_container);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, gallerySurfaceColor,
                topLayout, recyclerView, container);

        images = new ArrayList<>();
        adapter = new GalleryAdapter(images);
        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, gridColumns));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setAdapter(adapter);

        setupPageNavigator(container);
        updatePageIndicator();
        requestPermissionAndLoad();
    }

    private void setupPageNavigator(View container) {
        if (!scrollAppList) {
            pageNavigator = new SwipePageNavigator(this, recyclerView, container,
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
        } else {
            pageNavigator = null;
        }
    }

    private void toggleView() {
        isGridView = !isGridView;
        prefs.edit().putBoolean("gallery_grid_view", isGridView).apply();
        ImageView toggleButton = findViewById(R.id.toggle_view_button);
        toggleButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);

        currentPage = 0;
        itemsPerPage = calculateItemsPerPage();

        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, gridColumns));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(0);
        }

        adapter.notifyDataSetChanged();
        updatePageIndicator();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
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

            Set<Long> allIds = new HashSet<>();
            for (GalleryImage img : loaded) allIds.add(img.id);
            GalleryTrashHelper.pruneInvalidIds(GalleryActivity.this, allIds);
            List<GalleryImage> filtered = new ArrayList<>();
            for (GalleryImage img : loaded) {
                boolean trashed = GalleryTrashHelper.isTrashed(GalleryActivity.this, img.id);
                if (isTrashMode ? trashed : !trashed) filtered.add(img);
            }

            runOnUiThread(() -> {
                images.clear();
                images.addAll(filtered);
                itemsPerPage = calculateItemsPerPage();
                int totalPages = (int) Math.ceil((double) images.size() / itemsPerPage);
                if (restorePage >= 0) {
                    currentPage = Math.min(restorePage, Math.max(0, totalPages - 1));
                    restorePage = -1;
                } else {
                    currentPage = 0;
                }
                if (pageNavigator != null) {
                    pageNavigator.setCurrentPage(currentPage);
                    pageNavigator.setTotalItems(images.size());
                }
                adapter.notifyDataSetChanged();
                updatePageIndicator();
            });
        });
    }

    private int getGridItemSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return screenWidth / gridColumns;
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

        if (isGridView) {
            int itemSize = getGridItemSize();
            float density = getResources().getDisplayMetrics().density;
            int textAreaHeight = showGridTitles ? (int) (28 * density) : 0;
            int itemHeightPx = itemSize + textAreaHeight;
            int recyclerHeightPx = getResources().getDisplayMetrics().heightPixels
                    - (int) (topHeightDp * density)
                    - (int) (dividerDp * density)
                    - (int) (bottomHeightDp * density)
                    - navBarHeightPx;
            int fittingRows = Math.max(1, recyclerHeightPx / itemHeightPx);
            int rows = Math.min(gridRows, fittingRows);
            return gridColumns * rows;
        } else {
            float itemHeightDp = 64 + 20;
            return Math.max(1, (int) (recyclerHeightDp / itemHeightDp));
        }
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
        intent.putExtra("trash_mode", isTrashMode);

        startActivityForResult(intent, REQUEST_VIEWER);
        overridePendingTransition(0, appLauncherAnimations ? 0 : 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIEWER && resultCode == RESULT_OK) {
            restorePage = currentPage;
            loadImages();
            if (isTrashMode) {
                galleryModified = true;
                setResult(RESULT_OK);
            }
        } else if (requestCode == REQUEST_TRASH && resultCode == RESULT_OK) {
            restorePage = currentPage;
            loadImages();
        }
    }

    @Override
    public void onBackPressed() {
        if (galleryModified) setResult(RESULT_OK);
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

    private class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_GRID = 0;
        private static final int VIEW_TYPE_LIST = 1;

        private final List<GalleryImage> items;

        GalleryAdapter(List<GalleryImage> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return isGridView ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_GRID) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_gallery_grid, parent, false);
                return new GridViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_gallery_list, parent, false);
                return new ListViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int globalPosition = scrollAppList ? position : currentPage * itemsPerPage + position;
            if (globalPosition >= items.size()) return;

            GalleryImage image = items.get(globalPosition);

            if (holder instanceof GridViewHolder) {
                GridViewHolder gvh = (GridViewHolder) holder;
                int itemSize = getGridItemSize();
                float density = getResources().getDisplayMetrics().density;
                int textAreaHeight = showGridTitles ? (int) (28 * density) : 0;
                ViewGroup.LayoutParams lp = gvh.itemView.getLayoutParams();
                if (lp == null) {
                    lp = new ViewGroup.LayoutParams(itemSize, itemSize + textAreaHeight);
                } else {
                    lp.width = itemSize;
                    lp.height = itemSize + textAreaHeight;
                }
                gvh.itemView.setLayoutParams(lp);
                ViewGroup.LayoutParams imageLp = gvh.thumbnail.getLayoutParams();
                imageLp.height = itemSize;
                gvh.thumbnail.setLayoutParams(imageLp);
                gvh.filename.setVisibility(showGridTitles ? View.VISIBLE : View.GONE);
                gvh.filename.setText(image.name);
                ThemeUtils.applyTextColor(gvh.filename, theme, GalleryActivity.this);
                FontHelper.applyToViewTree(GalleryActivity.this, gvh.itemView);
                gvh.thumbnail.setImageBitmap(null);
                gvh.thumbnail.setTag(globalPosition);

                thumbnailExecutor.execute(() -> {
                    try {
                        Uri thumbUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image.id);
                        Bitmap thumb = getContentResolver().loadThumbnail(thumbUri,
                                new android.util.Size(200, 200), null);
                        if (thumb != null) {
                            gvh.thumbnail.post(() -> {
                                if (Integer.valueOf(globalPosition).equals(gvh.thumbnail.getTag())) {
                                    gvh.thumbnail.setImageBitmap(thumb);
                                }
                            });
                        }
                    } catch (Exception e) {
                    }
                });

                gvh.itemView.setOnClickListener(v -> openImage(globalPosition));
            } else if (holder instanceof ListViewHolder) {
                ListViewHolder lvh = (ListViewHolder) holder;
                lvh.filename.setText(image.name);
                lvh.date.setText(dateFormat.format(new Date(image.dateAdded * 1000)));
                ThemeUtils.applyTextColor(lvh.filename, theme, GalleryActivity.this);
                ThemeUtils.applyTextColor(lvh.date, theme, GalleryActivity.this);
                FontHelper.applyToViewTree(GalleryActivity.this, lvh.itemView);

                lvh.thumbnail.setImageBitmap(null);
                lvh.thumbnail.setTag(globalPosition);

                thumbnailExecutor.execute(() -> {
                    try {
                        Uri thumbUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image.id);
                        Bitmap thumb = getContentResolver().loadThumbnail(thumbUri,
                                new android.util.Size(200, 200), null);
                        if (thumb != null) {
                            lvh.thumbnail.post(() -> {
                                if (Integer.valueOf(globalPosition).equals(lvh.thumbnail.getTag())) {
                                    lvh.thumbnail.setImageBitmap(thumb);
                                }
                            });
                        }
                    } catch (Exception e) {
                    }
                });

                lvh.itemView.setOnClickListener(v -> openImage(globalPosition));
            }
        }

        @Override
        public int getItemCount() {
            if (scrollAppList) {
                return items.size();
            }
            int start = currentPage * itemsPerPage;
            return Math.min(itemsPerPage, items.size() - start);
        }

        class GridViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView filename;

            GridViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.gallery_thumbnail);
                filename = itemView.findViewById(R.id.gallery_filename);
            }
        }

        class ListViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView filename;
            TextView date;

            ListViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.gallery_thumbnail);
                filename = itemView.findViewById(R.id.gallery_filename);
                date = itemView.findViewById(R.id.gallery_date);
            }
        }
    }
}
