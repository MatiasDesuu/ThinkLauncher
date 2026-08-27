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
    private boolean isFolderView;
    private String selectedFolder;
    private boolean galleryModified;
    private SwipePageNavigator pageNavigator;

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

        titleView.setOnClickListener(v -> {
            int curCols = isFolderView ? folderGridColumns : gridColumns;
            int curRows = isFolderView ? folderGridRows : gridRows;
            boolean curShowTitles = isFolderView ? folderShowGridTitles : showGridTitles;
            boolean curIsGrid = isFolderView ? isFolderGridView : isGridView;
            new GalleryOptionsDialog(this, curCols, curRows, curShowTitles, curIsGrid, !scrollAppList, isTrashMode, galleryGroupMode,
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
                        if (pageNavigator != null) {
                            pageNavigator.setCurrentPage(0);
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
                            return (int) Math.ceil((double) displayItems.size() / itemsPerPage);
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

        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(0);
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
                    int globalPosition = scrollAppList ? position : currentPage * itemsPerPage + position;
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
            List<GalleryImage> filtered = new ArrayList<>();
            for (GalleryImage img : loaded) {
                boolean trashed = GalleryTrashHelper.isTrashed(GalleryActivity.this, img.id, img.mediaType);
                if (isTrashMode ? trashed : !trashed) filtered.add(img);
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
                Collections.sort(folders, (a, b) -> a.folderName.compareToIgnoreCase(b.folderName));
                updateDisplayItems();
                itemsPerPage = calculateItemsPerPage();
                applyGridLayoutManager();
                int totalPages = (int) Math.ceil((double) displayItems.size() / itemsPerPage);
                if (restorePage >= 0) {
                    currentPage = Math.min(restorePage, Math.max(0, totalPages - 1));
                    restorePage = -1;
                } else {
                    currentPage = 0;
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

    private void updateDisplayItems() {
        displayItems.clear();
        if (isTrashMode) {
            if (galleryGroupMode == GROUP_NONE) {
                displayItems.addAll(allMedia);
            } else {
                addGroupedItems(allMedia);
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
            for (GalleryImage img : allMedia) if (selectedFolder.equals(img.folderName)) source.add(img);
        } else {
            source = allMedia;
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

    private void updateTitle() {
        if (titleView == null) return;
        if (isTrashMode) titleView.setText("Trash");
        else if (isFolderView) titleView.setText("Folders");
        else if (selectedFolder != null) titleView.setText(selectedFolder);
        else titleView.setText("Gallery");
    }

    private void updateFolderButtonIcon() {
        ImageView folderButton = findViewById(R.id.folder_button);
        if (folderButton == null) return;
        if (isTrashMode || selectedFolder != null) {
            folderButton.setVisibility(View.GONE);
            return;
        }
        folderButton.setVisibility(View.VISIBLE);
        boolean showGallery = isFolderView;
        folderButton.setImageResource(showGallery ? R.drawable.gallery_view : R.drawable.folder_view);
        folderButton.setContentDescription(showGallery ? "Gallery" : "Folders");
    }

        private void toggleFolderView() {
        if (isTrashMode) return;
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
        int totalPages = (int) Math.ceil((double) displayItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
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
            int globalPosition = scrollAppList ? position : currentPage * itemsPerPage + position;
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
            int globalPosition = scrollAppList ? position : currentPage * itemsPerPage + position;
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
                    gvh.itemView.setOnClickListener(v -> openFolder(f.folderName));
                } else {
                    gvh.itemView.setOnClickListener(v -> openImage(globalPosition));
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
                    lvh.itemView.setOnClickListener(v -> openFolder(f.folderName));
                } else {
                    lvh.itemView.setOnClickListener(v -> openImage(globalPosition));
                }
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
            ImageView videoIndicator;

            GridViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.gallery_thumbnail);
                filename = itemView.findViewById(R.id.gallery_filename);
                videoIndicator = itemView.findViewById(R.id.video_indicator);
            }
        }

        class ListViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;
            TextView filename;
            TextView date;
            ImageView videoIndicator;

            ListViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.gallery_thumbnail);
                filename = itemView.findViewById(R.id.gallery_filename);
                date = itemView.findViewById(R.id.gallery_date);
                videoIndicator = itemView.findViewById(R.id.video_indicator);
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
