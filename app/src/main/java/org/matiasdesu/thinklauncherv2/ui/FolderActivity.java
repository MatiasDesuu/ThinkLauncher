package org.matiasdesu.thinklauncherv2.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.AppListSizeHelper;
import org.matiasdesu.thinklauncherv2.utils.AppSearchHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderActivity extends AppCompatActivity {

    public static final String EXTRA_FOLDER_ID = "folder_id";
    public static final String EXTRA_FOLDER_NAME = "folder_name";
    public static final String EXTRA_UPDATED_FOLDER_NAME = "updated_folder_name";
    private static final int REQUEST_ADD_APP = 1001;

    private int textSize;
    private boolean boldText;
    private List<AppSearchHelper.AppItem> folderApps;
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private FolderAppAdapter folderAdapter;
    private LinearLayout rootLayout;
    private SharedPreferences prefs;
    private boolean scrollAppList;
    private String folderId;
    private String folderName;
    private int sortMode = 0; // 0 = added order, 1 = name, 2 = custom
    private boolean isReordering = false;
    private List<AppSearchHelper.AppItem> originalAppsBeforeReorder;
    private TextView folderNameText;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(FolderActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                    finish();
                }
            }
        }
    };

    private BroadcastReceiver packageRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
                String packageName = intent.getData().getSchemeSpecificPart();
                removeAppFromList(packageName);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        folderId = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        folderName = getIntent().getStringExtra(EXTRA_FOLDER_NAME);

        if (folderId == null || folderName == null) {
            finish();
            return;
        }

        int bgColor = ThemeUtils.getBgColor(theme, this);
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
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(bgColor);
        }

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), Context.RECEIVER_NOT_EXPORTED);
        IntentFilter packageFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        registerReceiver(packageRemovedReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, this);
        }

        folderNameText = findViewById(R.id.folder_name_text);
        folderNameText.setText(folderName);
        ThemeUtils.applyTextColor(folderNameText, theme, this);
        folderNameText.setOnLongClickListener(v -> {
            new FolderOptionsDialog(this, sortMode, 
                // On change folder name
                () -> {
                    new RenameDialog(this, folderName, newName -> {
                        folderName = newName;
                        folderNameText.setText(folderName);
                        saveFolderName(folderId, folderName);
                    }).show();
                },
                // On toggle sort
                () -> {
                    sortMode = (sortMode + 1) % 3;
                    prefs.edit().putInt(folderId + "_sort_mode", sortMode).apply();
                    sortFolderApps();
                    currentPage = 0;
                    folderAdapter.notifyDataSetChanged();
                    updatePageIndicator();
                },
                // On reorder
                () -> {
                    enterReorderMode();
                }
            ).show();
            return true;
        });

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            if (isReordering) {
                cancelReorderMode();
            } else {
                returnResult();
                finish();
                overridePendingTransition(0, 0);
            }
        });

        ImageView addButton = findViewById(R.id.add_button);
        addButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        addButton.setOnClickListener(v -> {
            if (isReordering) {
                exitReorderMode();
            } else {
                Intent intent = new Intent(FolderActivity.this, AppSelectorActivity.class);
                intent.putExtra(AppSelectorActivity.EXTRA_POSITION, -2); // Special value for folder
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent, REQUEST_ADD_APP);
            }
        });

        sortMode = prefs.getInt(folderId + "_sort_mode", prefs.getBoolean(folderId + "_sort_by_name", false) ? 1 : 0);
        // Clean up old pref if it exists
        if (prefs.contains(folderId + "_sort_by_name")) {
            prefs.edit().remove(folderId + "_sort_by_name").apply();
        }

        RecyclerView rv = findViewById(R.id.folder_app_list);
        ThemeUtils.applyBackgroundColor(rv, theme, this);

        textSize = prefs.getInt("text_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;

        itemsPerPage = AppListSizeHelper.calculateItemsPerPage(this, textSize);

        RecyclerView recyclerView = findViewById(R.id.folder_app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        View container = findViewById(R.id.app_list_container);
        SwipePageNavigator pageNavigator = null;

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
                        return (int) Math.ceil((double) folderApps.size() / itemsPerPage);
                    }

                    @Override
                    public void updatePageIndicator() {
                        FolderActivity.this.updatePageIndicator();
                    }
                }, theme);
        }

        loadFolderApps();

        folderAdapter = new FolderAppAdapter(folderApps, this, theme);
        recyclerView.setAdapter(folderAdapter);

        if (!scrollAppList && pageNavigator != null) {
            pageNavigator.setItemsPerPage(itemsPerPage);
            pageNavigator.setTotalItems(folderApps.size());
            pageNavigator.setCurrentPage(currentPage);
        }

        updatePageIndicator();
    }

    private void loadFolderApps() {
        folderApps = new ArrayList<>();
        String key = (sortMode == 2) ? folderId + "_apps_custom" : folderId + "_apps_ordered";
        String appsData = prefs.getString(key, "");
        
        if (appsData.isEmpty() && sortMode == 2) {
            // If custom is empty, fallback to added order
            appsData = prefs.getString(folderId + "_apps_ordered", "");
        }

        if (!appsData.isEmpty()) {
            String[] appsList = appsData.split(";;;;");
            for (String appData : appsList) {
                String[] parts = appData.split("\\|");
                if (parts.length == 2) {
                    folderApps.add(new AppSearchHelper.AppItem(parts[0], parts[1]));
                }
            }
        } else {
            // Fallback: try to load from old Set-based storage
            Set<String> appSet = prefs.getStringSet(folderId + "_apps", new HashSet<>());
            for (String appData : appSet) {
                String[] parts = appData.split("\\|");
                if (parts.length == 2) {
                    folderApps.add(new AppSearchHelper.AppItem(parts[0], parts[1]));
                }
            }
            // Migrate to new format
            if (!folderApps.isEmpty()) {
                saveFolderApps();
                prefs.edit().remove(folderId + "_apps").apply();
            }
        }
        sortFolderApps();
    }
    
    private void sortFolderApps() {
        if (sortMode == 1) { // Alphabetical
            folderApps.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        } else if (sortMode == 0) { // Added order
            // Reload from SharedPreferences to restore original added order
            folderApps.clear();
            String appsData = prefs.getString(folderId + "_apps_ordered", "");
            if (!appsData.isEmpty()) {
                String[] appsList = appsData.split(";;;;");
                for (String appData : appsList) {
                    String[] parts = appData.split("\\|");
                    if (parts.length == 2) {
                        folderApps.add(new AppSearchHelper.AppItem(parts[0], parts[1]));
                    }
                }
            }
        } else if (sortMode == 2) { // Custom order
            folderApps.clear();
            String appsData = prefs.getString(folderId + "_apps_custom", "");
            if (appsData.isEmpty()) {
                appsData = prefs.getString(folderId + "_apps_ordered", "");
            }
            if (!appsData.isEmpty()) {
                String[] appsList = appsData.split(";;;;");
                for (String appData : appsList) {
                    String[] parts = appData.split("\\|");
                    if (parts.length == 2) {
                        folderApps.add(new AppSearchHelper.AppItem(parts[0], parts[1]));
                    }
                }
            }
        }
    }

    private void saveFolderApps() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < folderApps.size(); i++) {
            AppSearchHelper.AppItem app = folderApps.get(i);
            sb.append(app.label).append("|").append(app.packageName);
            if (i < folderApps.size() - 1) {
                sb.append(";;;;");
            }
        }
        String key = (isReordering || sortMode == 2) ? folderId + "_apps_custom" : folderId + "_apps_ordered";
        prefs.edit().putString(key, sb.toString()).apply();
        
        // If we are adding/removing apps and NOT in custom mode, we should probably update custom too?
        // Actually, if we add an app in "Added order", it should be added to the end of custom order too.
        if (sortMode != 2 && !isReordering) {
            // Also update custom order if it exists, appending the new item if it's an add
            // For simplicity, let's just keep them in sync for additions/removals if custom order doesn't exist yet
            // Or just always update both for add/remove?
        }
    }

    private void saveFolderName(String folderId, String name) {
        prefs.edit().putString(folderId + "_name", name).apply();
    }

    public void launchApp(String label, String packageName) {
        if ("notification_panel".equals(packageName)) {
            try {
                Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(getSystemService("statusbar"));
            } catch (Exception e) {
                // Log or ignore
            }
            finish();
        } else if ("launcher_settings".equals(packageName)) {
            try {
                Class<?> clazz = Class.forName("org.matiasdesu.thinklauncherv2.settings.SettingsActivity");
                Intent intent = new Intent(this, clazz);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 100);
                return;
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        } else if ("app_launcher".equals(packageName)) {
            Intent intent = new Intent(this, AppLauncherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 100);
            return;
        } else if (packageName != null && packageName.startsWith("webapp_")) {
            // Launch web app in default browser
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            String url = prefs.getString(packageName + "_url", "");
            if (!url.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(url));
                try {
                    startActivity(intent);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 100);
                    return;
                } catch (Exception e) {
                    // Log or ignore if no browser available
                }
            }
        } else if (!packageName.isEmpty()) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 100);
                return;
            }
        }
        finish();
    }

    private void updatePageIndicator() {
        TextView pageIndicator = findViewById(R.id.page_indicator);
        View bottomDivider = findViewById(R.id.bottom_divider);
        View bottomBar = findViewById(R.id.bottom_bar);
        if (scrollAppList || isReordering) {
            pageIndicator.setVisibility(View.GONE);
            bottomDivider.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            return;
        }
        pageIndicator.setVisibility(View.VISIBLE);
        bottomDivider.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        int totalPages = (int) Math.ceil((double) folderApps.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private void removeAppFromList(String packageName) {
        folderApps.removeIf(app -> app.packageName.equals(packageName));
        saveFolderApps();
        folderAdapter.notifyDataSetChanged();
        updatePageIndicator();
    }

    private void removeAppFromFolder(int position) {
        if (position >= 0 && position < folderApps.size()) {
            folderApps.remove(position);
            saveFolderApps();
            folderAdapter.notifyDataSetChanged();
            updatePageIndicator();
        }
    }

    private void editWebApp(int position) {
        if (position >= 0 && position < folderApps.size()) {
            AppSearchHelper.AppItem app = folderApps.get(position);
            String url = prefs.getString(app.packageName + "_url", "");
            new WebAppDialog(this, app.label, url, (newName, newUrl) -> {
                app.label = newName;
                prefs.edit().putString(app.packageName + "_url", newUrl).apply();
                saveFolderApps();
                sortFolderApps();
                folderAdapter.notifyDataSetChanged();
            }).show();
        }
    }

    private void enterReorderMode() {
        isReordering = true;
        originalAppsBeforeReorder = new ArrayList<>(folderApps);
        
        ImageView addButton = findViewById(R.id.add_button);
        addButton.setImageResource(R.drawable.check);
        
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setImageResource(R.drawable.cancel);
        
        updatePageIndicator();
        folderAdapter.notifyDataSetChanged();
    }

    private void exitReorderMode() {
        isReordering = false;
        
        ImageView addButton = findViewById(R.id.add_button);
        addButton.setImageResource(R.drawable.add);
        
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setImageResource(R.drawable.back_arrow);
        
        saveFolderApps();
        updatePageIndicator();
        folderAdapter.notifyDataSetChanged();
    }

    private void cancelReorderMode() {
        isReordering = false;
        
        if (originalAppsBeforeReorder != null) {
            folderApps.clear();
            folderApps.addAll(originalAppsBeforeReorder);
            originalAppsBeforeReorder = null;
        }
        
        ImageView addButton = findViewById(R.id.add_button);
        addButton.setImageResource(R.drawable.add);
        
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setImageResource(R.drawable.back_arrow);
        
        updatePageIndicator();
        folderAdapter.notifyDataSetChanged();
    }

    public void moveAppUp(int position) {
        if (position > 0 && position < folderApps.size()) {
            AppSearchHelper.AppItem app = folderApps.remove(position);
            folderApps.add(position - 1, app);
            folderAdapter.notifyDataSetChanged();
        }
    }

    public void moveAppDown(int position) {
        if (position >= 0 && position < folderApps.size() - 1) {
            AppSearchHelper.AppItem app = folderApps.remove(position);
            folderApps.add(position + 1, app);
            folderAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_APP && resultCode == RESULT_OK && data != null) {
            String label = data.getStringExtra(AppSelectorActivity.EXTRA_LABEL);
            String pkg = data.getStringExtra(AppSelectorActivity.EXTRA_PACKAGE);
            
            if (label != null && pkg != null && !pkg.isEmpty() && !pkg.equals("")) {
                // Check if app is already in folder
                boolean exists = false;
                for (AppSearchHelper.AppItem app : folderApps) {
                    if (app.packageName.equals(pkg)) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    folderApps.add(new AppSearchHelper.AppItem(label, pkg));
                    saveFolderApps();
                    sortFolderApps();
                    currentPage = 0;
                    folderAdapter.notifyDataSetChanged();
                    updatePageIndicator();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(homeButtonReceiver);
            unregisterReceiver(packageRemovedReceiver);
        } catch (Exception e) {
            // Already unregistered
        }
    }

    @Override
    public void onBackPressed() {
        if (isReordering) {
            cancelReorderMode();
        } else {
            returnResult();
            super.onBackPressed();
            overridePendingTransition(0, 0);
        }
    }
    
    private void returnResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_FOLDER_ID, folderId);
        resultIntent.putExtra(EXTRA_UPDATED_FOLDER_NAME, folderName);
        setResult(RESULT_OK, resultIntent);
    }

    private class FolderAppAdapter extends RecyclerView.Adapter<FolderAppAdapter.ViewHolder> {

        private List<AppSearchHelper.AppItem> apps;
        private FolderActivity activity;
        private int theme;

        public FolderAppAdapter(List<AppSearchHelper.AppItem> apps, FolderActivity activity, int theme) {
            this.apps = apps;
            this.activity = activity;
            this.theme = theme;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            int globalPosition = (activity.scrollAppList || isReordering) ? position : currentPage * itemsPerPage + position;
            if (globalPosition >= apps.size()) return;
            
            AppSearchHelper.AppItem app = apps.get(globalPosition);
            holder.textView.setText(app.label);
            holder.textView.setTextSize(activity.textSize);
            holder.textView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);
            ThemeUtils.applyBackgroundColor(holder.itemView, theme, activity);
            ThemeUtils.applyTextColor(holder.textView, theme, activity);
            
            if (isReordering) {
                holder.reorderButtons.setVisibility(View.VISIBLE);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
                
                holder.moveUpButton.setColorFilter(ThemeUtils.getTextColor(theme, activity));
                holder.moveDownButton.setColorFilter(ThemeUtils.getTextColor(theme, activity));
                
                // Swapped as per user report that they were reversed
                holder.moveUpButton.setOnClickListener(v -> activity.moveAppDown(globalPosition));
                holder.moveDownButton.setOnClickListener(v -> activity.moveAppUp(globalPosition));
            } else {
                holder.reorderButtons.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> activity.launchApp(app.label, app.packageName));
                holder.itemView.setOnLongClickListener(v -> {
                    AppOptionsDialog.OnMoreInfoCallback moreInfo = null;
                    if (app.packageName != null && app.packageName.startsWith("webapp_")) {
                        moreInfo = () -> activity.editWebApp(globalPosition);
                    }
                    new AppOptionsDialog(activity, app.packageName, () -> {
                        activity.removeAppFromFolder(globalPosition);
                    }, moreInfo).show();
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            if (activity.scrollAppList || isReordering) {
                return apps.size();
            }
            int start = currentPage * itemsPerPage;
            int end = Math.min(start + itemsPerPage, apps.size());
            return end - start;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView textView;
            public View reorderButtons;
            public ImageView moveUpButton;
            public ImageView moveDownButton;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.app_name);
                reorderButtons = itemView.findViewById(R.id.reorder_buttons);
                moveUpButton = itemView.findViewById(R.id.move_up_button);
                moveDownButton = itemView.findViewById(R.id.move_down_button);
            }
        }
    }
}
