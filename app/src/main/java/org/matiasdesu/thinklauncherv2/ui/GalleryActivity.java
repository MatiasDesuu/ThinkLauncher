package org.matiasdesu.thinklauncherv2.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.EditText;
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
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryFavoritesHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryHiddenHelper;
import org.matiasdesu.thinklauncherv2.utils.GalleryTrashHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 4001;
    private static final int REQUEST_VIEWER = 4002;
    private static final int REQUEST_TRASH = 4003;
    private static final int REQUEST_HIDDEN = 4005;
    private int theme;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private boolean appLauncherAnimations;
    private boolean showWallpaperBackdrop;
    private int gallerySurfaceColor;
    private SharedPreferences prefs;

    private List<GalleryImage> images;
    private List<GalleryImage> allMedia;
    private List<GalleryFolder> folders;
    private List<Object> displayItems;
    private GalleryAdapter adapter;
    private int itemsPerPage;
    private int currentPage = 0;
    private int restorePage = -1;
    private RecyclerView recyclerView;
    private TextView titleView;
    private boolean isGridView;
    private boolean isFolderGridView;
    private int gridColumns;
    private int gridRows;
    private boolean showGridTitles;
    private int folderGridColumns;
    private int folderGridRows;
    private boolean folderShowGridTitles;
    private boolean isTrashMode;
    private boolean isFavoritesMode;
    private boolean isHiddenMode;
    private boolean isFolderView;
    private String selectedFolder;
    private boolean galleryModified;
    private SwipePageNavigator pageNavigator;
    private final List<Integer> pageStartIndices = new ArrayList<>();
    private static final int REQUEST_FAVORITES = 4004;
    private boolean isSelectionMode;
    private final Set<String> selectedKeys = new HashSet<>();

    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(2);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat daySeparatorFormat = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat monthSeparatorFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat yearSeparatorFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    private static final int GROUP_NONE = 0;
    private static final int GROUP_DAY = 1;
    private static final int GROUP_MONTH = 2;
    private static final int GROUP_YEAR = 3;
    private int galleryGroupMode;
    private static final int FILTER_ALL = 0;
    private static final int FILTER_IMAGES = 1;
    private static final int FILTER_VIDEOS = 2;
    private int galleryFilterMode;

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
        isFavoritesMode = getIntent().getBooleanExtra("favorites_mode", false);
        isHiddenMode = getIntent().getBooleanExtra("hidden_mode", false);
        if (isTrashMode && isFavoritesMode) isFavoritesMode = false;
        if (isHiddenMode && (isTrashMode || isFavoritesMode)) { isTrashMode = false; isFavoritesMode = false; }
        if (isTrashMode && isHiddenMode) isHiddenMode = false;
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

        titleView = findViewById(R.id.gallery_title);
        ThemeUtils.applyTextColor(titleView, theme, this);
        updateTitle();

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> handleBackPressed());

        ImageView toggleViewButton = findViewById(R.id.toggle_view_button);
        toggleViewButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        toggleViewButton.setOnClickListener(v -> toggleView());

        ImageView folderButton = findViewById(R.id.folder_button);
        folderButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        updateFolderButtonIcon();
        folderButton.setOnClickListener(v -> toggleFolderView());

        ImageView selMore = findViewById(R.id.selection_more_button);
        if (selMore != null) {
            selMore.setColorFilter(ThemeUtils.getTextColor(theme, this));
            selMore.setOnClickListener(v -> showSelectionOptionsDialog());
        }

        titleView.setOnLongClickListener(v -> {
            if (isSelectionMode) {
                exitSelectionMode();
                return true;
            }
            if (scrollAppList) {
                if (recyclerView != null) {
                    recyclerView.scrollToPosition(0);
                    EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                }
                return true;
            }
            return false;
        });
        titleView.setOnClickListener(v -> {
            if (isSelectionMode) {
                exitSelectionMode();
                return;
            }
            int curCols = isFolderView ? folderGridColumns : gridColumns;
            int curRows = isFolderView ? folderGridRows : gridRows;
            boolean curShowTitles = isFolderView ? folderShowGridTitles : showGridTitles;
            boolean curIsGrid = isFolderView ? isFolderGridView : isGridView;
            new GalleryOptionsDialog(this, curCols, curRows, curShowTitles, curIsGrid, !scrollAppList, isTrashMode, isFavoritesMode, isHiddenMode, galleryGroupMode,
                    (columns, rows) -> {
                        if (isFolderView) {
                            folderGridColumns = columns;
                            folderGridRows = rows;
                            gridColumns = columns;
                            gridRows = rows;
                            prefs.edit().putInt("gallery_folder_grid_columns", columns).putInt("gallery_folder_grid_rows", rows).apply();
                        } else {
                            gridColumns = columns;
                            gridRows = rows;
                            prefs.edit().putInt("gallery_grid_columns", columns).putInt("gallery_grid_rows", rows).apply();
                        }
                        currentPage = 0;
                        itemsPerPage = calculateItemsPerPage();
                        applyGridLayoutManager();
                        recomputePagination();
                        if (pageNavigator != null) {
                            pageNavigator.setCurrentPage(0);
                            pageNavigator.setTotalItems(displayItems.size());
                        }
                        adapter.notifyDataSetChanged();
                        updatePageIndicator();
                    },
                    show -> {
                        if (isFolderView) {
                            folderShowGridTitles = show;
                            showGridTitles = show;
                            prefs.edit().putBoolean("gallery_folder_grid_show_titles", show).apply();
                        } else {
                            showGridTitles = show;
                            prefs.edit().putBoolean("gallery_grid_show_titles", show).apply();
                        }
                        adapter.notifyDataSetChanged();
                    },
                    mode -> {
                        galleryGroupMode = mode;
                        updateDisplayItems();
                        currentPage = 0;
                        itemsPerPage = calculateItemsPerPage();
                        applyGridLayoutManager();
                        recomputePagination();
                        if (pageNavigator != null) {
                            pageNavigator.setCurrentPage(0);
                            pageNavigator.setTotalItems(displayItems.size());
                        }
                        adapter.notifyDataSetChanged();
                        updatePageIndicator();
                        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
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
                    },
                    () -> {
                        if (isFavoritesMode) {
                            if (galleryModified) setResult(RESULT_OK);
                            finish();
                            overridePendingTransition(0, 0);
                        } else {
                            Intent intent = new Intent(GalleryActivity.this, GalleryActivity.class);
                            intent.putExtra("favorites_mode", true);
                            startActivityForResult(intent, REQUEST_FAVORITES);
                            overridePendingTransition(0, 0);
                        }
                    },
                    () -> {
                        if (isHiddenMode) {
                            if (galleryModified) setResult(RESULT_OK);
                            finish();
                            overridePendingTransition(0, 0);
                        } else {
                            Intent intent = new Intent(GalleryActivity.this, GalleryActivity.class);
                            intent.putExtra("hidden_mode", true);
                            startActivityForResult(intent, REQUEST_HIDDEN);
                            overridePendingTransition(0, 0);
                        }
                    },
                    () -> confirmEmptyTrash(),
                    galleryFilterMode,
                    filter -> {
                        galleryFilterMode = filter;
                        if (isSelectionMode) exitSelectionMode();
                        updateDisplayItems();
                        currentPage = 0;
                        itemsPerPage = calculateItemsPerPage();
                        applyGridLayoutManager();
                        recomputePagination();
                        if (pageNavigator != null) {
                            pageNavigator.setCurrentPage(0);
                            pageNavigator.setTotalItems(displayItems.size());
                        }
                        adapter.notifyDataSetChanged();
                        updatePageIndicator();
                        updateTitle();
                        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                    }).show();
        });

        isGridView = prefs.getBoolean("gallery_grid_view", true);
        isFolderGridView = prefs.getBoolean("gallery_folder_grid_view", true);
        gridColumns = prefs.getInt("gallery_grid_columns", 3);
        gridRows = prefs.getInt("gallery_grid_rows", 3);
        showGridTitles = prefs.getBoolean("gallery_grid_show_titles", true);
        folderGridColumns = prefs.getInt("gallery_folder_grid_columns", 3);
        folderGridRows = prefs.getInt("gallery_folder_grid_rows", 3);
        folderShowGridTitles = prefs.getBoolean("gallery_folder_grid_show_titles", true);
        galleryGroupMode = prefs.getInt("gallery_group_by", GROUP_NONE);
        if (galleryGroupMode < GROUP_NONE || galleryGroupMode > GROUP_YEAR) galleryGroupMode = GROUP_NONE;
        galleryFilterMode = prefs.getInt("gallery_filter_by", FILTER_ALL);
        if (galleryFilterMode < FILTER_ALL || galleryFilterMode > FILTER_VIDEOS) galleryFilterMode = FILTER_ALL;
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;
        toggleViewButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);
        isFolderView = false;
        selectedFolder = null;
        allMedia = new ArrayList<>();
        folders = new ArrayList<>();
        displayItems = new ArrayList<>();
        images = allMedia;

        recyclerView = findViewById(R.id.gallery_grid);
        View topLayout = findViewById(R.id.top_layout);
        View container = findViewById(R.id.app_list_container);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, gallerySurfaceColor,
                topLayout, recyclerView, container);

        adapter = new GalleryAdapter(displayItems);
        applyGridLayoutManager();
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setAdapter(adapter);

        setupPageNavigator(container);
        updatePageIndicator();
        TextView pi = findViewById(R.id.page_indicator);
        if (pi != null) {
            pi.setOnClickListener(v -> {
                if (scrollAppList || currentPage == 0) return;
                currentPage = 0;
                if (pageNavigator != null) pageNavigator.setCurrentPage(0);
                adapter.notifyDataSetChanged();
                updatePageIndicator();
                EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            });
        }
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
                            if (pageStartIndices.isEmpty()) return 1;
                            return pageStartIndices.size();
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
        if (isSelectionMode) {
            exitSelectionMode();
            return;
        }
        if (isFolderView) {
            isFolderGridView = !isFolderGridView;
            isGridView = isFolderGridView;
            prefs.edit().putBoolean("gallery_folder_grid_view", isFolderGridView).apply();
        } else {
            isGridView = !isGridView;
            prefs.edit().putBoolean("gallery_grid_view", isGridView).apply();
        }
        ImageView toggleButton = findViewById(R.id.toggle_view_button);
        toggleButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);

        currentPage = 0;
        itemsPerPage = calculateItemsPerPage();
        applyGridLayoutManager();
        recomputePagination();

        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(0);
            pageNavigator.setTotalItems(displayItems.size());
        }

        adapter.notifyDataSetChanged();
        updatePageIndicator();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

    private void applyGridLayoutManager() {
        if (recyclerView == null) return;
        if (isGridView) {
            GridLayoutManager glm = new GridLayoutManager(this, gridColumns);
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int globalPosition = getGlobalPosition(currentPage, position);
                    if (globalPosition >= displayItems.size()) return 1;
                    Object obj = displayItems.get(globalPosition);
                    if (obj instanceof GallerySeparator) return gridColumns;
                    return 1;
                }
            });
            recyclerView.setLayoutManager(glm);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }



    private void requestPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean needImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED;
            boolean needVideo = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED;
            if (needImages || needVideo) {
                ArrayList<String> perms = new ArrayList<>();
                if (needImages) perms.add(Manifest.permission.READ_MEDIA_IMAGES);
                if (needVideo) perms.add(Manifest.permission.READ_MEDIA_VIDEO);
                ActivityCompat.requestPermissions(this,
                        perms.toArray(new String[0]), REQUEST_PERMISSION);
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
            boolean granted = false;
            for (int r : grantResults) if (r == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) {
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
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATA
            };
            String[] videoProjection = {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.DATA
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
                        String bucket = cursor.getString(4);
                        String data = cursor.getString(5);
                        if (name == null || name.isEmpty()) name = "image_" + id;
                        String folderName = bucket != null && !bucket.isEmpty() ? bucket : "Unknown";
                        String folderPath = data != null ? new File(data).getParent() : folderName;
                        if (folderPath == null) folderPath = folderName;
                        loaded.add(new GalleryImage(id, name, dateAdded, size, GalleryTrashHelper.TYPE_IMAGE, folderName, folderPath));
                    }
                    cursor.close();
                }
            } catch (SecurityException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Gallery permission is required", Toast.LENGTH_SHORT).show());
                return;
            }
            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoProjection, null, null, sortOrder)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        String name = cursor.getString(1);
                        long dateAdded = cursor.getLong(2);
                        long size = cursor.getLong(3);
                        String bucket = cursor.getString(4);
                        String data = cursor.getString(5);
                        if (name == null || name.isEmpty()) name = "video_" + id;
                        String folderName = bucket != null && !bucket.isEmpty() ? bucket : "Unknown";
                        String folderPath = data != null ? new File(data).getParent() : folderName;
                        if (folderPath == null) folderPath = folderName;
                        loaded.add(new GalleryImage(id, name, dateAdded, size, GalleryTrashHelper.TYPE_VIDEO, folderName, folderPath));
                    }
                    cursor.close();
                }
            } catch (SecurityException e) {
            }
            loaded.sort((a, b) -> Long.compare(b.dateAdded, a.dateAdded));
            Set<Long> allImageIds = new HashSet<>();
            Set<Long> allVideoIds = new HashSet<>();
            for (GalleryImage img : loaded) {
                if (img.mediaType == GalleryTrashHelper.TYPE_VIDEO) allVideoIds.add(img.id);
                else allImageIds.add(img.id);
            }
            GalleryTrashHelper.pruneInvalidIds(GalleryActivity.this, allImageIds, allVideoIds);
            GalleryFavoritesHelper.pruneInvalidIds(GalleryActivity.this, allImageIds, allVideoIds);
            org.matiasdesu.thinklauncherv2.utils.GalleryHiddenHelper.pruneInvalidIds(GalleryActivity.this, allImageIds, allVideoIds);
            List<GalleryImage> filtered = new ArrayList<>();
            for (GalleryImage img : loaded) {
                boolean trashed = GalleryTrashHelper.isTrashed(GalleryActivity.this, img.id, img.mediaType);
                boolean fav = GalleryFavoritesHelper.isFavorite(GalleryActivity.this, img.id, img.mediaType);
                boolean hidden = org.matiasdesu.thinklauncherv2.utils.GalleryHiddenHelper.isHidden(GalleryActivity.this, img.id, img.mediaType);
                if (isTrashMode) {
                    if (trashed) filtered.add(img);
                } else if (isFavoritesMode) {
                    if (!trashed && !hidden && fav) filtered.add(img);
                } else if (isHiddenMode) {
                    if (!trashed && hidden) filtered.add(img);
                } else {
                    if (!trashed && !hidden) filtered.add(img);
                }
            }
            runOnUiThread(() -> {
                allMedia.clear();
                allMedia.addAll(filtered);
                images.clear();
                images.addAll(filtered);
                folders.clear();
                Map<String, List<GalleryImage>> map = new HashMap<>();
                for (GalleryImage img : allMedia) {
                    String key = img.folderName != null ? img.folderName : "Unknown";
                    map.computeIfAbsent(key, k -> new ArrayList<>()).add(img);
                }
                for (Map.Entry<String, List<GalleryImage>> e : map.entrySet()) {
                    List<GalleryImage> list = e.getValue();
                    Collections.sort(list, (a, b) -> Long.compare(b.dateAdded, a.dateAdded));
                    GalleryImage thumb = list.get(0);
                    folders.add(new GalleryFolder(e.getKey(), thumb.folderPath, list.size(), thumb.id, thumb.mediaType));
                }
                sortFolders();
                updateDisplayItems();
                itemsPerPage = calculateItemsPerPage();
                applyGridLayoutManager();
                if (restorePage >= 0) {
                    int saved = restorePage;
                    restorePage = -1;
                    recomputePagination();
                    int totalPages = pageStartIndices.isEmpty() ? 1 : pageStartIndices.size();
                    currentPage = Math.min(saved, Math.max(0, totalPages - 1));
                } else {
                    currentPage = 0;
                    recomputePagination();
                }
                if (pageNavigator != null) {
                    pageNavigator.setCurrentPage(currentPage);
                    pageNavigator.setTotalItems(displayItems.size());
                }
                adapter.notifyDataSetChanged();
                updatePageIndicator();
                updateTitle();
            });
        });
    }

    private boolean passesFilter(GalleryImage img) {
        if (galleryFilterMode == FILTER_IMAGES) return img.mediaType == GalleryTrashHelper.TYPE_IMAGE;
        if (galleryFilterMode == FILTER_VIDEOS) return img.mediaType == GalleryTrashHelper.TYPE_VIDEO;
        return true;
    }

    private List<GalleryImage> getFiltered(List<GalleryImage> src) {
        if (galleryFilterMode == FILTER_ALL) return src;
        List<GalleryImage> out = new ArrayList<>();
        for (GalleryImage img : src) if (passesFilter(img)) out.add(img);
        return out;
    }

    private void updateDisplayItems() {
        displayItems.clear();
        if (isTrashMode) {
            List<GalleryImage> filtered = getFiltered(allMedia);
            if (galleryGroupMode == GROUP_NONE) {
                displayItems.addAll(filtered);
            } else {
                addGroupedItems(filtered);
            }
            return;
        }
        if (isFavoritesMode) {
            List<GalleryImage> filtered = getFiltered(allMedia);
            if (galleryGroupMode == GROUP_NONE) {
                displayItems.addAll(filtered);
            } else {
                addGroupedItems(filtered);
            }
            return;
        }
        if (isHiddenMode) {
            List<GalleryImage> filtered = getFiltered(allMedia);
            if (galleryGroupMode == GROUP_NONE) {
                displayItems.addAll(filtered);
            } else {
                addGroupedItems(filtered);
            }
            return;
        }
        if (isFolderView) {
            displayItems.addAll(folders);
            return;
        }
        List<GalleryImage> source;
        if (selectedFolder != null) {
            source = new ArrayList<>();
            for (GalleryImage img : allMedia) if (selectedFolder.equals(img.folderName) && passesFilter(img)) source.add(img);
        } else {
            source = getFiltered(allMedia);
        }
        if (galleryGroupMode == GROUP_NONE) {
            displayItems.addAll(source);
        } else {
            addGroupedItems(source);
        }
    }

    private void addGroupedItems(List<GalleryImage> source) {
        String lastKey = null;
        for (GalleryImage img : source) {
            String key = getGroupKey(img);
            if (!key.equals(lastKey)) {
                displayItems.add(new GallerySeparator(formatSeparator(img), key));
                lastKey = key;
            }
            displayItems.add(img);
        }
    }

    private String getGroupKey(GalleryImage img) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(img.dateAdded * 1000);
        if (galleryGroupMode == GROUP_DAY) {
            return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH);
        } else if (galleryGroupMode == GROUP_MONTH) {
            return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH);
        } else if (galleryGroupMode == GROUP_YEAR) {
            return String.valueOf(cal.get(Calendar.YEAR));
        }
        return "";
    }

    private String formatSeparator(GalleryImage img) {
        Date d = new Date(img.dateAdded * 1000);
        if (galleryGroupMode == GROUP_DAY) return daySeparatorFormat.format(d);
        if (galleryGroupMode == GROUP_MONTH) return monthSeparatorFormat.format(d);
        if (galleryGroupMode == GROUP_YEAR) return yearSeparatorFormat.format(d);
        return "";
    }

    private void sortFolders() {
        Set<String> pinned = prefs.getStringSet("gallery_pinned_folders", new HashSet<>());
        if (pinned == null) pinned = new HashSet<>();
        final Set<String> pinnedFinal = pinned;
        Collections.sort(folders, (a, b) -> {
            boolean ap = pinnedFinal.contains(a.folderName);
            boolean bp = pinnedFinal.contains(b.folderName);
            if (ap != bp) return ap ? -1 : 1;
            return a.folderName.compareToIgnoreCase(b.folderName);
        });
    }

    private boolean isFolderPinned(String folderName) {
        Set<String> pinned = prefs.getStringSet("gallery_pinned_folders", new HashSet<>());
        return pinned != null && pinned.contains(folderName);
    }

    private void toggleFolderPin(String folderName) {
        Set<String> pinned = prefs.getStringSet("gallery_pinned_folders", new HashSet<>());
        if (pinned == null) pinned = new HashSet<>();
        Set<String> updated = new HashSet<>(pinned);
        if (updated.contains(folderName)) updated.remove(folderName);
        else updated.add(folderName);
        prefs.edit().putStringSet("gallery_pinned_folders", updated).apply();
        sortFolders();
        updateDisplayItems();
        currentPage = 0;
        itemsPerPage = calculateItemsPerPage();
        applyGridLayoutManager();
        recomputePagination();
        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(0);
            pageNavigator.setTotalItems(displayItems.size());
        }
        adapter.notifyDataSetChanged();
        updatePageIndicator();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

    private void showFolderOptions(String folderName) {
        boolean pinned = isFolderPinned(folderName);
        new GalleryFolderOptionsDialog(this, folderName, pinned, () -> toggleFolderPin(folderName), () -> openFolder(folderName), () -> renameFolder(folderName)).show();
    }

    private void showImageDetails(GalleryImage img) {
        String fullPath = null;
        if (img.folderPath != null && img.name != null) {
            fullPath = new File(img.folderPath, img.name).getAbsolutePath();
        }
        new GalleryDetailsDialog(this, img.id, img.mediaType, img.name, img.dateAdded, img.size, fullPath).show();
    }

    private void renameFolder(String oldName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
            Toast.makeText(this, "Grant file access permission to rename folder", Toast.LENGTH_LONG).show();
            return;
        }
        new RenameDialog(this, oldName, newName -> {
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newName.equals(oldName)) return;
            if (newName.contains("/") || newName.contains("\\") || newName.contains(":")) {
                Toast.makeText(this, "Invalid name", Toast.LENGTH_SHORT).show();
                return;
            }
            String oldPath = null;
            for (GalleryFolder f : folders) if (f.folderName.equals(oldName)) { oldPath = f.folderPath; break; }
            if (oldPath == null) {
                for (GalleryImage img : allMedia) if (oldName.equals(img.folderName) && img.folderPath != null) { oldPath = new File(img.folderPath).getParent(); if (oldPath == null) oldPath = img.folderPath; break; }
                if (oldPath != null) oldPath = oldPath + File.separator + oldName;
            }
            if (oldPath == null) {
                Toast.makeText(this, "Folder path not found", Toast.LENGTH_SHORT).show();
                return;
            }
            File oldDir = new File(oldPath);
            if (!oldDir.exists() || !oldDir.isDirectory()) {
                File fallback = null;
                for (GalleryImage img : allMedia) if (oldName.equals(img.folderName)) {
                    String p = img.folderPath;
                    if (p != null) { File pf = new File(p); File parent = pf.getParentFile(); if (parent != null && parent.getName().equals(oldName)) { fallback = parent; break; } }
                }
                if (fallback != null) oldDir = fallback;
            }
            if (!oldDir.exists()) {
                Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show();
                return;
            }
            File newDir = new File(oldDir.getParent(), newName);
            if (newDir.exists()) {
                Toast.makeText(this, "Folder already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            final File finalOldDir = oldDir;
            final File finalNewDir = newDir;
            final String finalOldName = oldName;
            final String finalNewName = newName;
            thumbnailExecutor.execute(() -> {
                boolean success = finalOldDir.renameTo(finalNewDir);
                if (!success) {
                    boolean allMoved = true;
                    File[] files = finalOldDir.listFiles();
                    if (files != null) {
                        finalNewDir.mkdirs();
                        for (File f : files) {
                            File dest = new File(finalNewDir, f.getName());
                            if (!f.renameTo(dest)) allMoved = false;
                        }
                        if (allMoved) finalOldDir.delete();
                        success = allMoved && finalNewDir.exists();
                    }
                }
                boolean finalSuccess = success;
                runOnUiThread(() -> {
                    if (!finalSuccess) {
                        Toast.makeText(this, "Failed to rename folder", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Set<String> pinned = prefs.getStringSet("gallery_pinned_folders", new HashSet<>());
                    if (pinned != null && pinned.contains(finalOldName)) {
                        Set<String> updated = new HashSet<>(pinned);
                        updated.remove(finalOldName);
                        updated.add(finalNewName);
                        prefs.edit().putStringSet("gallery_pinned_folders", updated).apply();
                    }
                    try {
                        getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + " LIKE ?", new String[]{finalOldDir.getAbsolutePath() + "/%"});
                        getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + " LIKE ?", new String[]{finalOldDir.getAbsolutePath() + "/%"});
                    } catch (Exception ignored) {}
                    File[] toScan = finalNewDir.listFiles();
                    if (toScan != null && toScan.length > 0) {
                        String[] paths = new String[toScan.length];
                        for (int i = 0; i < toScan.length; i++) paths[i] = toScan[i].getAbsolutePath();
                        MediaScannerConnection.scanFile(this, paths, null, null);
                    } else {
                        MediaScannerConnection.scanFile(this, new String[]{finalNewDir.getAbsolutePath()}, null, null);
                    }
                    if (selectedFolder != null && selectedFolder.equals(finalOldName)) selectedFolder = finalNewName;
                    loadImages();
                    Toast.makeText(this, "Folder renamed", Toast.LENGTH_SHORT).show();
                });
            });
        }).show();
    }

    private void updateTitle() {
        if (titleView == null) return;
        if (isTrashMode) titleView.setText("Trash");
        else if (isFavoritesMode) titleView.setText("Favorites");
        else if (isHiddenMode) titleView.setText("Hidden");
        else if (isFolderView) titleView.setText("Folders");
        else if (selectedFolder != null) titleView.setText(selectedFolder);
        else titleView.setText("Gallery");
    }

    private void updateFolderButtonIcon() {
        ImageView folderButton = findViewById(R.id.folder_button);
        if (folderButton == null) return;
        if (isSelectionMode || isTrashMode || isFavoritesMode || isHiddenMode || selectedFolder != null) {
            folderButton.setVisibility(View.GONE);
            return;
        }
        folderButton.setVisibility(View.VISIBLE);
        boolean showGallery = isFolderView;
        folderButton.setImageResource(showGallery ? R.drawable.gallery_view : R.drawable.folder_view);
        folderButton.setContentDescription(showGallery ? "Gallery" : "Folders");
    }

    private String getSelectionKey(GalleryImage img) {
        return (img.mediaType == GalleryTrashHelper.TYPE_VIDEO ? "video:" : "image:") + img.id;
    }

    private boolean isSelected(GalleryImage img) {
        return selectedKeys.contains(getSelectionKey(img));
    }

    private void enterSelectionMode(GalleryImage img) {
        if (isFolderView) return;
        isSelectionMode = true;
        selectedKeys.clear();
        selectedKeys.add(getSelectionKey(img));
        updateSelectionUI();
        refreshVisibleSelectionOverlays();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedKeys.clear();
        updateSelectionUI();
        refreshVisibleSelectionOverlays();
    }

    private void toggleSelection(GalleryImage img, int globalPosition) {
        String key = getSelectionKey(img);
        if (selectedKeys.contains(key)) selectedKeys.remove(key);
        else selectedKeys.add(key);
        if (selectedKeys.isEmpty()) {
            exitSelectionMode();
            return;
        }
        updateSelectionUI();
        refreshSelectionForGlobalPosition(globalPosition);
    }

    private void refreshVisibleSelectionOverlays() {
        if (recyclerView == null) return;
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) continue;
            int globalPos = getGlobalPosition(currentPage, pos);
            if (globalPos < 0 || globalPos >= displayItems.size()) continue;
            Object obj = displayItems.get(globalPos);
            if (!(obj instanceof GalleryImage)) continue;
            GalleryImage gi = (GalleryImage) obj;
            boolean isSel = isSelected(gi);
            if (holder instanceof GalleryAdapter.GridViewHolder) {
                GalleryAdapter.GridViewHolder gvh = (GalleryAdapter.GridViewHolder) holder;
                if (gvh.selectionOverlay != null) gvh.selectionOverlay.setVisibility(View.GONE);
                if (gvh.selectionCheck != null) {
                    gvh.selectionCheck.setVisibility(isSel ? View.VISIBLE : View.GONE);
                    if (isSel) {
                        int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                        int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                        ThemeUtils.applyButtonBorder(gvh.selectionCheck, txt, bg, GalleryActivity.this);
                        gvh.selectionCheck.setColorFilter(txt);
                    }
                }
            } else if (holder instanceof GalleryAdapter.ListViewHolder) {
                GalleryAdapter.ListViewHolder lvh = (GalleryAdapter.ListViewHolder) holder;
                if (lvh.selectionOverlay != null) lvh.selectionOverlay.setVisibility(View.GONE);
                if (lvh.selectionCheck != null) {
                    lvh.selectionCheck.setVisibility(isSel ? View.VISIBLE : View.GONE);
                    if (isSel) {
                        int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                        int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                        ThemeUtils.applyButtonBorder(lvh.selectionCheck, txt, bg, GalleryActivity.this);
                        lvh.selectionCheck.setColorFilter(txt);
                    }
                }
            }
        }
    }

    private void refreshSelectionForGlobalPosition(int globalPosition) {
        if (recyclerView == null) return;
        Object target = globalPosition >= 0 && globalPosition < displayItems.size() ? displayItems.get(globalPosition) : null;
        if (!(target instanceof GalleryImage)) {
            refreshVisibleSelectionOverlays();
            return;
        }
        GalleryImage gi = (GalleryImage) target;
        boolean isSel = isSelected(gi);
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) continue;
            int gp = getGlobalPosition(currentPage, pos);
            if (gp != globalPosition) continue;
            if (holder instanceof GalleryAdapter.GridViewHolder) {
                GalleryAdapter.GridViewHolder gvh = (GalleryAdapter.GridViewHolder) holder;
                if (gvh.selectionOverlay != null) gvh.selectionOverlay.setVisibility(View.GONE);
                if (gvh.selectionCheck != null) {
                    gvh.selectionCheck.setVisibility(isSel ? View.VISIBLE : View.GONE);
                    if (isSel) {
                        int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                        int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                        ThemeUtils.applyButtonBorder(gvh.selectionCheck, txt, bg, GalleryActivity.this);
                        gvh.selectionCheck.setColorFilter(txt);
                    }
                }
            } else if (holder instanceof GalleryAdapter.ListViewHolder) {
                GalleryAdapter.ListViewHolder lvh = (GalleryAdapter.ListViewHolder) holder;
                if (lvh.selectionOverlay != null) lvh.selectionOverlay.setVisibility(View.GONE);
                if (lvh.selectionCheck != null) {
                    lvh.selectionCheck.setVisibility(isSel ? View.VISIBLE : View.GONE);
                    if (isSel) {
                        int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                        int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                        ThemeUtils.applyButtonBorder(lvh.selectionCheck, txt, bg, GalleryActivity.this);
                        lvh.selectionCheck.setColorFilter(txt);
                    }
                }
            }
            return;
        }
        refreshVisibleSelectionOverlays();
    }

    private void refreshVisibleFavorites() {
        if (recyclerView == null) return;
        java.util.Set<String> favSet = prefs.getStringSet("gallery_favorite_ids", null);
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) continue;
            int globalPos = getGlobalPosition(currentPage, pos);
            if (globalPos < 0 || globalPos >= displayItems.size()) continue;
            Object obj = displayItems.get(globalPos);
            if (!(obj instanceof GalleryImage)) continue;
            GalleryImage gi = (GalleryImage) obj;
            boolean isFav = favSet != null && (favSet.contains("image:" + gi.id) || favSet.contains("video:" + gi.id) || favSet.contains(String.valueOf(gi.id)));
            if (holder instanceof GalleryAdapter.GridViewHolder) {
                GalleryAdapter.GridViewHolder gvh = (GalleryAdapter.GridViewHolder) holder;
                if (gvh.favoriteIndicator != null) {
                    if (isFav) {
                        gvh.favoriteIndicator.setVisibility(View.VISIBLE);
                        gvh.favoriteIndicator.setBackground(null);
                        gvh.favoriteIndicator.setColorFilter(ThemeUtils.getTextColor(theme, GalleryActivity.this));
                    } else {
                        gvh.favoriteIndicator.setVisibility(View.GONE);
                    }
                }
            } else if (holder instanceof GalleryAdapter.ListViewHolder) {
                GalleryAdapter.ListViewHolder lvh = (GalleryAdapter.ListViewHolder) holder;
                if (lvh.favoriteIndicator != null) {
                    if (isFav) {
                        lvh.favoriteIndicator.setVisibility(View.VISIBLE);
                        lvh.favoriteIndicator.setBackground(null);
                        lvh.favoriteIndicator.setColorFilter(ThemeUtils.getTextColor(theme, GalleryActivity.this));
                    } else {
                        lvh.favoriteIndicator.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private boolean areAllSelectedFavorites() {
        if (selectedKeys.isEmpty()) return false;
        for (String key : selectedKeys) {
            try {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryFavoritesHelper.TYPE_VIDEO : GalleryFavoritesHelper.TYPE_IMAGE;
                if (!GalleryFavoritesHelper.isFavorite(this, id, type)) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private boolean areAllSelectedHidden() {
        if (selectedKeys.isEmpty()) return false;
        for (String key : selectedKeys) {
            try {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryHiddenHelper.TYPE_VIDEO : GalleryHiddenHelper.TYPE_IMAGE;
                if (!GalleryHiddenHelper.isHidden(this, id, type)) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void updateSelectionUI() {
        if (titleView == null) return;
        ImageView backButton = findViewById(R.id.back_button);
        ImageView toggleButton = findViewById(R.id.toggle_view_button);
        ImageView selMore = findViewById(R.id.selection_more_button);
        View bottomBar = findViewById(R.id.bottom_bar);
        View bottomDivider = findViewById(R.id.bottom_divider);
        ImageView selTrash = findViewById(R.id.selection_trash_button);
        ImageView selFav = findViewById(R.id.selection_favorite_button);
        ImageView selHidden = findViewById(R.id.selection_hidden_button);
        if (isSelectionMode) {
            titleView.setText(selectedKeys.size() + " selected");
            if (backButton != null) {
                backButton.setImageResource(R.drawable.cancel);
                backButton.setContentDescription("Close selection");
                backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
            }
            if (toggleButton != null) toggleButton.setVisibility(View.GONE);
            if (selTrash != null) selTrash.setVisibility(View.GONE);
            if (selFav != null) selFav.setVisibility(View.GONE);
            if (selHidden != null) selHidden.setVisibility(View.GONE);
            if (selMore != null) {
                selMore.setVisibility(View.VISIBLE);
                selMore.setColorFilter(ThemeUtils.getTextColor(theme, this));
            }
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
            if (bottomDivider != null) bottomDivider.setVisibility(View.GONE);
            updateFolderButtonIcon();
        } else {
            updateTitle();
            updateFolderButtonIcon();
            if (backButton != null) {
                backButton.setImageResource(R.drawable.back_arrow);
                backButton.setContentDescription("Back");
                backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
            }
            if (toggleButton != null) {
                toggleButton.setVisibility(View.VISIBLE);
                toggleButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);
                toggleButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
            }
            if (selTrash != null) selTrash.setVisibility(View.GONE);
            if (selFav != null) selFav.setVisibility(View.GONE);
            if (selHidden != null) selHidden.setVisibility(View.GONE);
            if (selMore != null) selMore.setVisibility(View.GONE);
            updatePageIndicator();
        }
    }

    private void showSelectionOptionsDialog() {
        if (!isSelectionMode || selectedKeys.isEmpty()) return;
        boolean allFav = areAllSelectedFavorites();
        boolean allHidden = areAllSelectedHidden();
        String favText = isTrashMode ? "Restore" : (allFav ? "Unfavorite" : "Favorite");
        String hiddenText = isHiddenMode ? "Unhide" : (allHidden ? "Unhide" : "Hide");
        String trashText = isTrashMode ? "Delete permanently" : "Move to trash";
        android.app.Dialog d = new android.app.Dialog(this, R.style.NoAnimationDialog);
        d.setContentView(R.layout.dialog_gallery_viewer_options);
        FontHelper.applyToViewTree(this, d.findViewById(android.R.id.content));
        int surfaceColor = DialogEffectHelper.setup(d, theme);
        View root = d.findViewById(android.R.id.content);
        DialogEffectHelper.applySurface(root, theme, this, surfaceColor);
        TextView favBtn = d.findViewById(R.id.option_favorite);
        TextView hiddenBtn = d.findViewById(R.id.option_hidden);
        TextView trashBtn = d.findViewById(R.id.option_trash);
        if (favBtn != null) {
            DialogEffectHelper.applyButtonTheme(favBtn, theme, this, surfaceColor);
            favBtn.setText(favText);
            favBtn.setOnClickListener(v -> { d.dismiss(); handleBatchFavorite(); });
        }
        if (hiddenBtn != null) {
            DialogEffectHelper.applyButtonTheme(hiddenBtn, theme, this, surfaceColor);
            hiddenBtn.setText(hiddenText);
            if (isTrashMode) hiddenBtn.setVisibility(View.GONE);
            else hiddenBtn.setOnClickListener(v -> { d.dismiss(); handleBatchHidden(); });
        }
        if (trashBtn != null) {
            DialogEffectHelper.applyButtonTheme(trashBtn, theme, this, surfaceColor);
            trashBtn.setText(trashText);
            trashBtn.setOnClickListener(v -> {
                d.dismiss();
                if (isTrashMode) confirmPermanentDeleteForSelection();
                else handleBatchTrash();
            });
        }
        d.show();
    }

    private void confirmPermanentDeleteForSelection() {
        if (!isTrashMode || selectedKeys.isEmpty()) return;
        new DeleteImageDialog(this, "Delete permanently? This cannot be undone.", "Delete", () -> {
            handleBatchTrash();
        }).show();
    }

    private void handleBatchTrash() {
        if (selectedKeys.isEmpty()) return;
        if (isTrashMode) {
            for (String key : new HashSet<>(selectedKeys)) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryTrashHelper.TYPE_VIDEO : GalleryTrashHelper.TYPE_IMAGE;
                try {
                    Uri baseUri = type == GalleryTrashHelper.TYPE_VIDEO ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    Uri uri = ContentUris.withAppendedId(baseUri, id);
                    getContentResolver().delete(uri, null, null);
                    GalleryTrashHelper.removeFromTrash(this, id, type);
                } catch (Exception ignored) {}
            }
        } else {
            for (String key : selectedKeys) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryTrashHelper.TYPE_VIDEO : GalleryTrashHelper.TYPE_IMAGE;
                GalleryTrashHelper.moveToTrash(this, id, type);
                GalleryFavoritesHelper.removeFavorite(this, id, type);
                GalleryHiddenHelper.unhide(this, id, type);
            }
        }
        galleryModified = true;
        setResult(RESULT_OK);
        exitSelectionMode();
        loadImages();
    }

    private void handleBatchFavorite() {
        if (selectedKeys.isEmpty()) return;
        if (isTrashMode) {
            for (String key : new HashSet<>(selectedKeys)) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryTrashHelper.TYPE_VIDEO : GalleryTrashHelper.TYPE_IMAGE;
                GalleryTrashHelper.restore(this, id, type);
            }
            galleryModified = true;
            setResult(RESULT_OK);
            exitSelectionMode();
            loadImages();
            return;
        }
        boolean allFav = areAllSelectedFavorites();
        Set<String> favKeys = new HashSet<>(selectedKeys);
        if (allFav) {
            for (String key : favKeys) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryTrashHelper.TYPE_VIDEO : GalleryTrashHelper.TYPE_IMAGE;
                GalleryFavoritesHelper.removeFavorite(this, id, type);
            }
        } else {
            for (String key : favKeys) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryTrashHelper.TYPE_VIDEO : GalleryTrashHelper.TYPE_IMAGE;
                GalleryFavoritesHelper.addFavorite(this, id, type);
            }
        }
        if (isFavoritesMode) {
            galleryModified = true;
            setResult(RESULT_OK);
            exitSelectionMode();
            loadImages();
            return;
        }
        exitSelectionMode();
        refreshVisibleFavorites();
    }

    private void handleBatchHidden() {
        if (selectedKeys.isEmpty()) return;
        if (isHiddenMode) {
            for (String key : new HashSet<>(selectedKeys)) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryHiddenHelper.TYPE_VIDEO : GalleryHiddenHelper.TYPE_IMAGE;
                GalleryHiddenHelper.unhide(this, id, type);
            }
            galleryModified = true;
            setResult(RESULT_OK);
            exitSelectionMode();
            loadImages();
            return;
        }
        boolean allHidden = areAllSelectedHidden();
        Set<String> keys = new HashSet<>(selectedKeys);
        if (allHidden) {
            for (String key : keys) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryHiddenHelper.TYPE_VIDEO : GalleryHiddenHelper.TYPE_IMAGE;
                GalleryHiddenHelper.unhide(this, id, type);
            }
        } else {
            for (String key : keys) {
                long id = Long.parseLong(key.substring(key.indexOf(":") + 1));
                int type = key.startsWith("video:") ? GalleryHiddenHelper.TYPE_VIDEO : GalleryHiddenHelper.TYPE_IMAGE;
                GalleryHiddenHelper.hide(this, id, type);
                GalleryFavoritesHelper.removeFavorite(this, id, type);
            }
        }
        galleryModified = true;
        setResult(RESULT_OK);
        exitSelectionMode();
        loadImages();
    }

    private void confirmEmptyTrash() {
        if (!isTrashMode) return;
        int count = 0;
        for (Object obj : displayItems) if (obj instanceof GalleryImage) count++;
        if (count == 0) return;
        new GalleryEmptyTrashConfirmDialog(this, count, this::emptyTrash).show();
    }

    private void emptyTrash() {
        if (!isTrashMode) return;
        java.util.List<GalleryImage> toDelete = new java.util.ArrayList<>();
        for (Object obj : displayItems) if (obj instanceof GalleryImage) toDelete.add((GalleryImage) obj);
        if (toDelete.isEmpty()) return;
        for (GalleryImage img : toDelete) {
            try {
                Uri baseUri = img.mediaType == GalleryTrashHelper.TYPE_VIDEO ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Uri uri = ContentUris.withAppendedId(baseUri, img.id);
                getContentResolver().delete(uri, null, null);
                GalleryTrashHelper.removeFromTrash(this, img.id, img.mediaType);
            } catch (Exception ignored) {}
        }
        if (isSelectionMode) exitSelectionMode();
        galleryModified = true;
        setResult(RESULT_OK);
        loadImages();
    }

        private void toggleFolderView() {
        if (isSelectionMode) {
            exitSelectionMode();
            return;
        }
        if (isTrashMode || isFavoritesMode || isHiddenMode) return;
        if (selectedFolder != null) {
            selectedFolder = null;
            isFolderView = true;
            isGridView = prefs.getBoolean("gallery_folder_grid_view", true);
            isFolderGridView = isGridView;
            gridColumns = folderGridColumns;
            gridRows = folderGridRows;
            showGridTitles = folderShowGridTitles;
        } else if (isFolderView) {
            isFolderView = false;
            isGridView = prefs.getBoolean("gallery_grid_view", true);
            gridColumns = prefs.getInt("gallery_grid_columns", 3);
            gridRows = prefs.getInt("gallery_grid_rows", 3);
            showGridTitles = prefs.getBoolean("gallery_grid_show_titles", true);
        } else {
            isFolderView = true;
            isGridView = prefs.getBoolean("gallery_folder_grid_view", true);
            isFolderGridView = isGridView;
            gridColumns = folderGridColumns;
            gridRows = folderGridRows;
            showGridTitles = folderShowGridTitles;
        }
        ImageView toggleButton = findViewById(R.id.toggle_view_button);
        if (toggleButton != null) toggleButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);
        currentPage = 0;
        updateDisplayItems();
        itemsPerPage = calculateItemsPerPage();
        applyGridLayoutManager();
        recomputePagination();
        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(0);
            pageNavigator.setTotalItems(displayItems.size());
        }
        adapter.notifyDataSetChanged();
        updatePageIndicator();
        updateTitle();
        updateFolderButtonIcon();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

        private void openFolder(String folderName) {
        selectedFolder = folderName;
        isFolderView = false;
        isGridView = prefs.getBoolean("gallery_grid_view", true);
        gridColumns = prefs.getInt("gallery_grid_columns", 3);
        gridRows = prefs.getInt("gallery_grid_rows", 3);
        showGridTitles = prefs.getBoolean("gallery_grid_show_titles", true);
        ImageView toggleButton = findViewById(R.id.toggle_view_button);
        if (toggleButton != null) toggleButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);
        currentPage = 0;
        updateDisplayItems();
        itemsPerPage = calculateItemsPerPage();
        applyGridLayoutManager();
        recomputePagination();
        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(0);
            pageNavigator.setTotalItems(displayItems.size());
        }
        adapter.notifyDataSetChanged();
        updatePageIndicator();
        updateTitle();
        updateFolderButtonIcon();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

        private void handleBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode();
            return;
        }
        if (selectedFolder != null) {
            selectedFolder = null;
            isFolderView = true;
            isGridView = prefs.getBoolean("gallery_folder_grid_view", true);
            isFolderGridView = isGridView;
            gridColumns = folderGridColumns;
            gridRows = folderGridRows;
            showGridTitles = folderShowGridTitles;
            ImageView toggleButton = findViewById(R.id.toggle_view_button);
            if (toggleButton != null) toggleButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);
            currentPage = 0;
            updateDisplayItems();
            itemsPerPage = calculateItemsPerPage();
            applyGridLayoutManager();
            recomputePagination();
            if (pageNavigator != null) {
                pageNavigator.setCurrentPage(0);
                pageNavigator.setTotalItems(displayItems.size());
            }
            adapter.notifyDataSetChanged();
            updatePageIndicator();
            updateTitle();
            updateFolderButtonIcon();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            return;
        }
        if (isFolderView) {
            isFolderView = false;
            isGridView = prefs.getBoolean("gallery_grid_view", true);
            gridColumns = prefs.getInt("gallery_grid_columns", 3);
            gridRows = prefs.getInt("gallery_grid_rows", 3);
            showGridTitles = prefs.getBoolean("gallery_grid_show_titles", true);
            ImageView toggleButton = findViewById(R.id.toggle_view_button);
            if (toggleButton != null) toggleButton.setImageResource(isGridView ? R.drawable.view_list : R.drawable.view_grid);
            currentPage = 0;
            updateDisplayItems();
            itemsPerPage = calculateItemsPerPage();
            applyGridLayoutManager();
            recomputePagination();
            if (pageNavigator != null) {
                pageNavigator.setCurrentPage(0);
                pageNavigator.setTotalItems(displayItems.size());
            }
            adapter.notifyDataSetChanged();
            updatePageIndicator();
            updateTitle();
            updateFolderButtonIcon();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            return;
        }
        if (galleryModified) setResult(RESULT_OK);
        finish();
        overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
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

    private void recomputePagination() {
        pageStartIndices.clear();
        if (scrollAppList) {
            return;
        }
        if (displayItems.isEmpty()) {
            pageStartIndices.add(0);
            return;
        }
        if (galleryGroupMode == GROUP_NONE || isFolderView) {
            for (int i = 0; i < displayItems.size(); i += itemsPerPage) {
                pageStartIndices.add(i);
            }
            if (pageStartIndices.isEmpty()) pageStartIndices.add(0);
            if (currentPage >= pageStartIndices.size()) currentPage = pageStartIndices.size() - 1;
            if (currentPage < 0) currentPage = 0;
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        int navBarHeightPx = 0;
        try {
            navBarHeightPx = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        } catch (Exception e) {
        }
        int recyclerHeightPx = getResources().getDisplayMetrics().heightPixels
                - (int) (48 * density) - (int) (4 * density) - (int) (48 * density) - navBarHeightPx;
        if (recyclerHeightPx <= 0) {
            pageStartIndices.add(0);
            return;
        }
        int separatorHeightPx = (int) (36 * density);
        if (isGridView) {
            int itemSize = getGridItemSize();
            int textAreaHeight = showGridTitles ? (int) (28 * density) : 0;
            int rowHeightPx = itemSize + textAreaHeight;
            pageStartIndices.add(0);
            int pageHeightUsed = 0;
            int rowSlotsUsed = 0;
            for (int i = 0; i < displayItems.size(); i++) {
                Object obj = displayItems.get(i);
                if (obj instanceof GallerySeparator) {
                    if (rowSlotsUsed != 0) {
                        rowSlotsUsed = 0;
                    }
                    if (pageHeightUsed + separatorHeightPx > recyclerHeightPx) {
                        pageHeightUsed = 0;
                        rowSlotsUsed = 0;
                        pageStartIndices.add(i);
                    }
                    pageHeightUsed += separatorHeightPx;
                } else {
                    if (rowSlotsUsed == 0) {
                        if (pageHeightUsed + rowHeightPx > recyclerHeightPx) {
                            pageHeightUsed = 0;
                            pageStartIndices.add(i);
                        }
                        pageHeightUsed += rowHeightPx;
                        rowSlotsUsed = 1;
                        if (rowSlotsUsed == gridColumns) rowSlotsUsed = 0;
                    } else {
                        rowSlotsUsed++;
                        if (rowSlotsUsed == gridColumns) rowSlotsUsed = 0;
                    }
                }
            }
        } else {
            int imageHeightPx = (int) ((64 + 20) * density);
            pageStartIndices.add(0);
            int pageHeightUsed = 0;
            for (int i = 0; i < displayItems.size(); i++) {
                Object obj = displayItems.get(i);
                int needed = (obj instanceof GallerySeparator) ? separatorHeightPx : imageHeightPx;
                if (pageHeightUsed + needed > recyclerHeightPx) {
                    pageHeightUsed = 0;
                    pageStartIndices.add(i);
                }
                pageHeightUsed += needed;
            }
        }
        if (pageStartIndices.isEmpty()) pageStartIndices.add(0);
        if (currentPage >= pageStartIndices.size()) currentPage = pageStartIndices.size() - 1;
        if (currentPage < 0) currentPage = 0;
    }

    private int getPageItemCount(int page) {
        if (scrollAppList) return displayItems.size();
        if (page < 0 || page >= pageStartIndices.size()) return 0;
        int start = pageStartIndices.get(page);
        int end = (page + 1 < pageStartIndices.size()) ? pageStartIndices.get(page + 1) : displayItems.size();
        return end - start;
    }

    private int getGlobalPosition(int page, int position) {
        if (scrollAppList) return position;
        if (page < 0 || page >= pageStartIndices.size()) return position;
        return pageStartIndices.get(page) + position;
    }

    private int findPageForGlobalPosition(int globalPos) {
        if (scrollAppList) return 0;
        for (int p = 0; p < pageStartIndices.size(); p++) {
            int start = pageStartIndices.get(p);
            int end = (p + 1 < pageStartIndices.size()) ? pageStartIndices.get(p + 1) : displayItems.size();
            if (globalPos >= start && globalPos < end) return p;
        }
        return 0;
    }

    private void updatePageIndicator() {
        TextView pageIndicator = findViewById(R.id.page_indicator);
        View bottomDivider = findViewById(R.id.bottom_divider);
        View bottomBar = findViewById(R.id.bottom_bar);
        if (isSelectionMode) {
            if (pageIndicator != null) pageIndicator.setVisibility(View.GONE);
            if (bottomDivider != null) bottomDivider.setVisibility(View.GONE);
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
            return;
        }
        if (scrollAppList) {
            pageIndicator.setVisibility(View.GONE);
            bottomDivider.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            return;
        }
        pageIndicator.setVisibility(View.VISIBLE);
        bottomDivider.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        int totalPages = pageStartIndices.isEmpty() ? 1 : pageStartIndices.size();
        if (totalPages == 0) totalPages = 1;
        int displayPage = Math.min(currentPage, totalPages - 1);
        pageIndicator.setText((displayPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private void openImage(int position) {
        if (position < 0 || position >= displayItems.size()) return;
        Object obj = displayItems.get(position);
        if (!(obj instanceof GalleryImage)) return;
        List<GalleryImage> currentImages = new ArrayList<>();
        for (Object o : displayItems) if (o instanceof GalleryImage) currentImages.add((GalleryImage) o);
        GalleryImage clicked = (GalleryImage) obj;
        int viewerIndex = currentImages.indexOf(clicked);
        if (viewerIndex < 0) viewerIndex = 0;
        long[] idsArray = new long[currentImages.size()];
        String[] namesArray = new String[currentImages.size()];
        int[] typesArray = new int[currentImages.size()];
        long[] datesArray = new long[currentImages.size()];
        for (int i = 0; i < currentImages.size(); i++) {
            idsArray[i] = currentImages.get(i).id;
            namesArray[i] = currentImages.get(i).name;
            typesArray[i] = currentImages.get(i).mediaType;
            datesArray[i] = currentImages.get(i).dateAdded;
        }
        Intent intent = new Intent(this, GalleryViewerActivity.class);
        intent.putExtra("current_index", viewerIndex);
        intent.putExtra("image_ids", idsArray);
        intent.putExtra("image_names", namesArray);
        intent.putExtra("media_types", typesArray);
        intent.putExtra("image_dates", datesArray);
        intent.putExtra("trash_mode", isTrashMode);
        intent.putExtra("favorites_mode", isFavoritesMode);
        intent.putExtra("hidden_mode", isHiddenMode);
        startActivityForResult(intent, REQUEST_VIEWER);
        overridePendingTransition(0, appLauncherAnimations ? 0 : 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIEWER && resultCode == RESULT_OK) {
            restorePage = currentPage;
            loadImages();
            if (isTrashMode || isFavoritesMode || isHiddenMode) {
                galleryModified = true;
                setResult(RESULT_OK);
            }
        } else if ((requestCode == REQUEST_TRASH || requestCode == REQUEST_FAVORITES || requestCode == REQUEST_HIDDEN) && resultCode == RESULT_OK) {
            restorePage = currentPage;
            loadImages();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
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
        int mediaType;
        String folderName;
        String folderPath;

        GalleryImage(long id, String name, long dateAdded, long size, int mediaType, String folderName, String folderPath) {
            this.id = id;
            this.name = name;
            this.dateAdded = dateAdded;
            this.size = size;
            this.mediaType = mediaType;
            this.folderName = folderName;
            this.folderPath = folderPath;
        }
    }

    private static class GalleryFolder {
        String folderName;
        String folderPath;
        int count;
        long thumbId;
        int thumbType;

        GalleryFolder(String folderName, String folderPath, int count, long thumbId, int thumbType) {
            this.folderName = folderName;
            this.folderPath = folderPath;
            this.count = count;
            this.thumbId = thumbId;
            this.thumbType = thumbType;
        }
    }

    private static class GallerySeparator {
        String label;
        String key;

        GallerySeparator(String label, String key) {
            this.label = label;
            this.key = key;
        }
    }

    private class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_GRID = 0;
        private static final int VIEW_TYPE_LIST = 1;
        private static final int VIEW_TYPE_SEPARATOR = 2;

        private final List<Object> items;

        GalleryAdapter(List<Object> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            int globalPosition = getGlobalPosition(currentPage, position);
            if (globalPosition < items.size() && items.get(globalPosition) instanceof GallerySeparator) {
                return VIEW_TYPE_SEPARATOR;
            }
            return isGridView ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_SEPARATOR) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_gallery_separator, parent, false);
                return new SeparatorViewHolder(view);
            } else if (viewType == VIEW_TYPE_GRID) {
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
            int globalPosition = getGlobalPosition(currentPage, position);
            if (globalPosition >= items.size()) return;

            Object obj = items.get(globalPosition);
            if (obj instanceof GallerySeparator) {
                GallerySeparator sep = (GallerySeparator) obj;
                SeparatorViewHolder svh = (SeparatorViewHolder) holder;
                svh.label.setText(sep.label);
                ThemeUtils.applyTextColor(svh.label, theme, GalleryActivity.this);
                FontHelper.applyToViewTree(GalleryActivity.this, svh.itemView);
                if (isGridView) {
                    ViewGroup.LayoutParams lp = svh.itemView.getLayoutParams();
                    if (lp instanceof GridLayoutManager.LayoutParams) {
                        ((GridLayoutManager.LayoutParams) lp).height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
                svh.itemView.setOnClickListener(null);
                return;
            }
            boolean isFolder = obj instanceof GalleryFolder;
            GalleryImage image = null;
            GalleryFolder folder = null;
            long thumbId = 0;
            int thumbType = GalleryTrashHelper.TYPE_IMAGE;
            String displayName = "";
            String displayDate = "";
            boolean showVideoIcon = false;
            boolean showFolderIcon = false;
            if (isFolder) {
                folder = (GalleryFolder) obj;
                thumbId = folder.thumbId;
                thumbType = folder.thumbType;
                displayName = folder.folderName + " (" + folder.count + ")";
                displayDate = folder.count + " items";
                showFolderIcon = true;
            } else {
                image = (GalleryImage) obj;
                thumbId = image.id;
                thumbType = image.mediaType;
                displayName = image.name;
                displayDate = dateFormat.format(new Date(image.dateAdded * 1000));
                showVideoIcon = image.mediaType == GalleryTrashHelper.TYPE_VIDEO;
            }
            Uri baseUri = thumbType == GalleryTrashHelper.TYPE_VIDEO ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

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
                gvh.filename.setVisibility(showGridTitles ? View.VISIBLE : View.GONE);
                gvh.filename.setText(displayName);
                ThemeUtils.applyTextColor(gvh.filename, theme, GalleryActivity.this);
                FontHelper.applyToViewTree(GalleryActivity.this, gvh.itemView);
                if (showFolderIcon) {
                    gvh.videoIndicator.setVisibility(View.VISIBLE);
                    gvh.videoIndicator.setImageResource(R.drawable.folder);
                    int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                    int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                    ThemeUtils.applyButtonBorder(gvh.videoIndicator, txt, bg, GalleryActivity.this);
                    gvh.videoIndicator.setColorFilter(txt);
                } else if (showVideoIcon) {
                    gvh.videoIndicator.setVisibility(View.VISIBLE);
                    gvh.videoIndicator.setImageResource(R.drawable.ic_media_play);
                    int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                    int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                    ThemeUtils.applyButtonBorder(gvh.videoIndicator, txt, bg, GalleryActivity.this);
                    gvh.videoIndicator.setColorFilter(txt);
                } else {
                    gvh.videoIndicator.setVisibility(View.GONE);
                }
                if (isFolder && isFolderPinned(folder.folderName)) {
                    gvh.pinnedIndicator.setVisibility(View.VISIBLE);
                    int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                    int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                    ThemeUtils.applyButtonBorder(gvh.pinnedIndicator, txt, bg, GalleryActivity.this);
                    gvh.pinnedIndicator.setColorFilter(txt);
                } else {
                    gvh.pinnedIndicator.setVisibility(View.GONE);
                }
                java.util.Set<String> favSetG = prefs.getStringSet("gallery_favorite_ids", null);
                boolean gIsFav = !isFolder && image != null && favSetG != null && (favSetG.contains("image:" + image.id) || favSetG.contains("video:" + image.id) || favSetG.contains(String.valueOf(image.id)));
                if (gIsFav) {
                    gvh.favoriteIndicator.setVisibility(View.VISIBLE);
                    gvh.favoriteIndicator.setBackground(null);
                    gvh.favoriteIndicator.setColorFilter(ThemeUtils.getTextColor(theme, GalleryActivity.this));
                } else {
                    gvh.favoriteIndicator.setVisibility(View.GONE);
                }
                boolean isSelG = !isFolder && image != null && isSelected(image);
                if (gvh.selectionOverlay != null) gvh.selectionOverlay.setVisibility(View.GONE);
                if (gvh.selectionCheck != null) {
                    gvh.selectionCheck.setVisibility(isSelG ? View.VISIBLE : View.GONE);
                    if (isSelG) {
                        int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                        int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                        ThemeUtils.applyButtonBorder(gvh.selectionCheck, txt, bg, GalleryActivity.this);
                        gvh.selectionCheck.setColorFilter(txt);
                    }
                }
                gvh.thumbnail.setImageBitmap(null);
                gvh.thumbnail.setTag(globalPosition);
                final long fThumbId = thumbId;
                final Uri fBaseUri = baseUri;
                final int fPos = globalPosition;
                thumbnailExecutor.execute(() -> {
                    try {
                        Uri thumbUri = ContentUris.withAppendedId(fBaseUri, fThumbId);
                        Bitmap thumb = getContentResolver().loadThumbnail(thumbUri,
                                new android.util.Size(200, 200), null);
                        if (thumb != null) {
                            gvh.thumbnail.post(() -> {
                                if (Integer.valueOf(fPos).equals(gvh.thumbnail.getTag())) {
                                    gvh.thumbnail.setImageBitmap(thumb);
                                }
                            });
                        }
                    } catch (Exception e) {
                    }
                });

                if (isFolder) {
                    GalleryFolder f = folder;
                    gvh.itemView.setOnLongClickListener(null);
                    gvh.itemView.setOnClickListener(null);
                    final Handler h = new Handler(Looper.getMainLooper());
                    final float slop = ViewConfiguration.get(gvh.itemView.getContext()).getScaledTouchSlop();
                    final float[] down = new float[2];
                    final boolean[] longFired = new boolean[1];
                    final Runnable runnableLp = () -> {
                        longFired[0] = true;
                        showFolderOptions(f.folderName);
                        android.view.ViewParent p = gvh.itemView.getParent();
                        if (p != null) p.requestDisallowInterceptTouchEvent(true);
                        if (recyclerView != null && recyclerView.getParent() != null) recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
                        gvh.itemView.getParent().requestDisallowInterceptTouchEvent(true);
                    };
                    gvh.itemView.setOnTouchListener((v, e) -> {
                        switch (e.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                down[0] = e.getX(); down[1] = e.getY();
                                longFired[0] = false;
                                h.postDelayed(runnableLp, ViewConfiguration.getLongPressTimeout());
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (!longFired[0] && (Math.abs(e.getX() - down[0]) > slop || Math.abs(e.getY() - down[1]) > slop)) {
                                    h.removeCallbacks(runnableLp);
                                }
                                if (longFired[0]) return true;
                                break;
                            case MotionEvent.ACTION_UP:
                                h.removeCallbacks(runnableLp);
                                if (longFired[0]) { longFired[0] = false; return true; }
                                if (Math.abs(e.getX() - down[0]) < slop && Math.abs(e.getY() - down[1]) < slop) {
                                    openFolder(f.folderName);
                                    return true;
                                }
                                break;
                            case MotionEvent.ACTION_CANCEL:
                                h.removeCallbacks(runnableLp);
                                longFired[0] = false;
                                break;
                        }
                        return false;
                    });
                } else {
                    final GalleryImage fImgG = image;
                    gvh.itemView.setOnTouchListener(null);
                    gvh.itemView.setOnClickListener(v -> {
                        if (isSelectionMode) toggleSelection(fImgG, globalPosition);
                        else openImage(globalPosition);
                    });
                    gvh.itemView.setOnLongClickListener(v -> {
                        if (isSelectionMode) {
                            toggleSelection(fImgG, globalPosition);
                            return true;
                        } else {
                            enterSelectionMode(fImgG);
                            return true;
                        }
                    });
                }
            } else if (holder instanceof ListViewHolder) {
                ListViewHolder lvh = (ListViewHolder) holder;
                lvh.filename.setText(displayName);
                lvh.date.setText(displayDate);
                ThemeUtils.applyTextColor(lvh.filename, theme, GalleryActivity.this);
                ThemeUtils.applyTextColor(lvh.date, theme, GalleryActivity.this);
                FontHelper.applyToViewTree(GalleryActivity.this, lvh.itemView);
                if (showFolderIcon) {
                    lvh.videoIndicator.setVisibility(View.VISIBLE);
                    lvh.videoIndicator.setImageResource(R.drawable.folder);
                    int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                    int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                    ThemeUtils.applyButtonBorder(lvh.videoIndicator, txt, bg, GalleryActivity.this);
                    lvh.videoIndicator.setColorFilter(txt);
                } else if (showVideoIcon) {
                    lvh.videoIndicator.setVisibility(View.VISIBLE);
                    lvh.videoIndicator.setImageResource(R.drawable.ic_media_play);
                    int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                    int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                    ThemeUtils.applyButtonBorder(lvh.videoIndicator, txt, bg, GalleryActivity.this);
                    lvh.videoIndicator.setColorFilter(txt);
                } else {
                    lvh.videoIndicator.setVisibility(View.GONE);
                }
                if (isFolder && isFolderPinned(folder.folderName)) {
                    lvh.pinnedIndicator.setVisibility(View.VISIBLE);
                    int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                    int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                    ThemeUtils.applyButtonBorder(lvh.pinnedIndicator, txt, bg, GalleryActivity.this);
                    lvh.pinnedIndicator.setColorFilter(txt);
                } else {
                    lvh.pinnedIndicator.setVisibility(View.GONE);
                }
                java.util.Set<String> favSetL = prefs.getStringSet("gallery_favorite_ids", null);
                boolean lIsFav = !isFolder && image != null && favSetL != null && (favSetL.contains("image:" + image.id) || favSetL.contains("video:" + image.id) || favSetL.contains(String.valueOf(image.id)));
                if (lIsFav) {
                    lvh.favoriteIndicator.setVisibility(View.VISIBLE);
                    lvh.favoriteIndicator.setBackground(null);
                    lvh.favoriteIndicator.setColorFilter(ThemeUtils.getTextColor(theme, GalleryActivity.this));
                } else {
                    lvh.favoriteIndicator.setVisibility(View.GONE);
                }
                boolean isSelL = !isFolder && image != null && isSelected(image);
                if (lvh.selectionOverlay != null) lvh.selectionOverlay.setVisibility(View.GONE);
                if (lvh.selectionCheck != null) {
                    lvh.selectionCheck.setVisibility(isSelL ? View.VISIBLE : View.GONE);
                    if (isSelL) {
                        int bg = ThemeUtils.getBgColor(theme, GalleryActivity.this);
                        int txt = ThemeUtils.getTextColor(theme, GalleryActivity.this);
                        ThemeUtils.applyButtonBorder(lvh.selectionCheck, txt, bg, GalleryActivity.this);
                        lvh.selectionCheck.setColorFilter(txt);
                    }
                }

                lvh.thumbnail.setImageBitmap(null);
                lvh.thumbnail.setTag(globalPosition);
                final long fThumbId2 = thumbId;
                final Uri fBaseUri2 = baseUri;
                final int fPos2 = globalPosition;
                thumbnailExecutor.execute(() -> {
                    try {
                        Uri thumbUri = ContentUris.withAppendedId(fBaseUri2, fThumbId2);
                        Bitmap thumb = getContentResolver().loadThumbnail(thumbUri,
                                new android.util.Size(200, 200), null);
                        if (thumb != null) {
                            lvh.thumbnail.post(() -> {
                                if (Integer.valueOf(fPos2).equals(lvh.thumbnail.getTag())) {
                                    lvh.thumbnail.setImageBitmap(thumb);
                                }
                            });
                        }
                    } catch (Exception e) {
                    }
                });

                if (isFolder) {
                    GalleryFolder f = folder;
                    lvh.itemView.setOnClickListener(null);
                    lvh.itemView.setOnLongClickListener(null);
                    final Handler h2 = new Handler(Looper.getMainLooper());
                    final float slop2 = ViewConfiguration.get(lvh.itemView.getContext()).getScaledTouchSlop();
                    final float[] down2 = new float[2];
                    final boolean[] longFired2 = new boolean[1];
                    final Runnable lp2 = () -> {
                        longFired2[0] = true;
                        showFolderOptions(f.folderName);
                        android.view.ViewParent p2 = lvh.itemView.getParent();
                        if (p2 != null) p2.requestDisallowInterceptTouchEvent(true);
                        if (recyclerView != null && recyclerView.getParent() != null) recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
                        lvh.itemView.getParent().requestDisallowInterceptTouchEvent(true);
                    };
                    lvh.itemView.setOnTouchListener((v, e) -> {
                        switch (e.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                down2[0] = e.getX(); down2[1] = e.getY();
                                longFired2[0] = false;
                                h2.postDelayed(lp2, ViewConfiguration.getLongPressTimeout());
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (!longFired2[0] && (Math.abs(e.getX() - down2[0]) > slop2 || Math.abs(e.getY() - down2[1]) > slop2)) {
                                    h2.removeCallbacks(lp2);
                                }
                                if (longFired2[0]) return true;
                                break;
                            case MotionEvent.ACTION_UP:
                                h2.removeCallbacks(lp2);
                                if (longFired2[0]) { longFired2[0] = false; return true; }
                                if (Math.abs(e.getX() - down2[0]) < slop2 && Math.abs(e.getY() - down2[1]) < slop2) {
                                    openFolder(f.folderName);
                                    return true;
                                }
                                break;
                            case MotionEvent.ACTION_CANCEL:
                                h2.removeCallbacks(lp2);
                                longFired2[0] = false;
                                break;
                        }
                        return false;
                    });
                } else {
                    final GalleryImage fImgL = image;
                    lvh.itemView.setOnTouchListener(null);
                    lvh.itemView.setOnClickListener(v -> {
                        if (isSelectionMode) toggleSelection(fImgL, globalPosition);
                        else openImage(globalPosition);
                    });
                    lvh.itemView.setOnLongClickListener(v -> {
                        if (isSelectionMode) {
                            toggleSelection(fImgL, globalPosition);
                            return true;
                        } else {
                            enterSelectionMode(fImgL);
                            return true;
                        }
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            if (scrollAppList) {
                return items.size();
            }
            return getPageItemCount(currentPage);
        }

        class GridViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView filename;
            ImageView videoIndicator;
            ImageView pinnedIndicator;
            ImageView favoriteIndicator;
            View selectionOverlay;
            ImageView selectionCheck;

            GridViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.gallery_thumbnail);
                filename = itemView.findViewById(R.id.gallery_filename);
                videoIndicator = itemView.findViewById(R.id.video_indicator);
                pinnedIndicator = itemView.findViewById(R.id.pinned_indicator);
                favoriteIndicator = itemView.findViewById(R.id.favorite_indicator);
                selectionOverlay = itemView.findViewById(R.id.selection_overlay);
                selectionCheck = itemView.findViewById(R.id.selection_check);
            }
        }

        class ListViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView filename;
            TextView date;
            ImageView videoIndicator;
            ImageView pinnedIndicator;
            ImageView favoriteIndicator;
            View selectionOverlay;
            ImageView selectionCheck;

            ListViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.gallery_thumbnail);
                filename = itemView.findViewById(R.id.gallery_filename);
                date = itemView.findViewById(R.id.gallery_date);
                videoIndicator = itemView.findViewById(R.id.video_indicator);
                pinnedIndicator = itemView.findViewById(R.id.pinned_indicator);
                favoriteIndicator = itemView.findViewById(R.id.favorite_indicator);
                selectionOverlay = itemView.findViewById(R.id.selection_overlay);
                selectionCheck = itemView.findViewById(R.id.selection_check);
            }
        }

        class SeparatorViewHolder extends RecyclerView.ViewHolder {
            TextView label;

            SeparatorViewHolder(View itemView) {
                super(itemView);
                label = itemView.findViewById(R.id.gallery_separator);
            }
        }
    }
}
