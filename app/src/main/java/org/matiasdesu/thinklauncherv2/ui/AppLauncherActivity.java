package org.matiasdesu.thinklauncherv2.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.SwipePageNavigator;
import org.matiasdesu.thinklauncherv2.utils.AppListSizeHelper;
import org.matiasdesu.thinklauncherv2.utils.AppSearchHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.SystemAppHelper;
import org.matiasdesu.thinklauncherv2.utils.OnyxHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppLauncherActivity extends AppCompatActivity {

    private int textSize;
    private boolean boldText;
    private List<AppSearchHelper.AppItem> originalApps;
    private List<AppSearchHelper.AppItem> filteredApps;
    private AppSearchHelper.IndexedApps appIndex;
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private AppLauncherAdapter launcherAdapter;
    private boolean keyboardShown = false;
    private SharedPreferences prefs;
    private List<String> installedAppLabels;
    private List<String> installedAppPackages;
    private Set<String> hiddenApps;
    private List<AppSearchHelper.AppItem> searchApps;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private int appIndexSidebar;
    private int appIndexAnimation;
    private LinearLayout indexSidebar;
    private LinearLayout indexSidebarHorizontal;
    private SwipePageNavigator pageNavigator;
    private boolean appLauncherAnimations;
    private String[] sidebarLetters;
    private int highlightedLetterIndex = -1;
    private int sidebarLetterTextSize = 16;
    private boolean showWallpaperBackdrop;
    private int launcherSurfaceColor;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(AppLauncherActivity.this, MainActivity.class);
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
        hiddenApps = new HashSet<>(prefs.getStringSet("hidden_apps", new HashSet<>()));
        theme = prefs.getInt("theme", 0);
        opacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1;
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_app_selector);

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
        launcherSurfaceColor = backdrop.surfaceColor;
        showWallpaperBackdrop = backdrop.showWallpaperBackdrop;

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);
        IntentFilter packageFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        registerReceiver(packageRemovedReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        View topLayout = findViewById(R.id.top_layout);
        EditText searchEditText = findViewById(R.id.search_edit_text);
        ThemeUtils.applyEditTextTheme(searchEditText, theme, this);
        RecyclerView rv = findViewById(R.id.app_selector_list);
        View container = findViewById(R.id.app_list_container);
        View listWrapper = findViewById(R.id.list_wrapper);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, launcherSurfaceColor,
                topLayout, searchEditText, rv, container, listWrapper);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        textSize = prefs.getInt("app_launcher_font_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;
        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        appIndexAnimation = prefs.getInt("app_index_animation", 0);
        appLauncherAnimations = prefs.getInt("screen_animations", 0) == 1;

        itemsPerPage = AppListSizeHelper.calculateItemsPerPage(this, textSize);

        RecyclerView recyclerView = findViewById(R.id.app_selector_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setClipChildren(true);
        recyclerView.setClipToPadding(true);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }
        });

        pageNavigator = null;

        if (!scrollAppList) {
            pageNavigator = new SwipePageNavigator(this, recyclerView, container,
                    new SwipePageNavigator.PageChangeCallback() {
                        @Override
                        public void onPageChanged(int newPage) {
                            currentPage = newPage;
                            highlightedLetterIndex = -1;
                            if (indexSidebarHorizontal != null && !scrollAppList) {
                                int textColor = ThemeUtils.getTextColor(theme, AppLauncherActivity.this);
                                int bgColor = ThemeUtils.getBgColor(theme, AppLauncherActivity.this);
                                updateHorizontalSidebarHighlight(textColor, bgColor);
                            }
                            recyclerView.getAdapter().notifyDataSetChanged();
                            updatePageIndicator();
                            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                        }

                        @Override
                        public int getTotalPages() {
                            return (int) Math.ceil((double) filteredApps.size() / itemsPerPage);
                        }

                        @Override
                        public void updatePageIndicator() {
                            AppLauncherActivity.this.updatePageIndicator();
                        }
                    }, theme);
        }

        List<String> installedAppLabels = new ArrayList<>();
        List<String> installedAppPackages = new ArrayList<>();

        originalApps = new ArrayList<>();
        filteredApps = new ArrayList<>();

        loadAppsAsync(installedAppLabels, installedAppPackages);

        indexSidebar = findViewById(R.id.index_sidebar);
        indexSidebarHorizontal = findViewById(R.id.index_sidebar_horizontal);
        buildIndexSidebar();

        launcherAdapter = new AppLauncherAdapter(filteredApps, this, theme);
        recyclerView.setAdapter(launcherAdapter);

        if (!scrollAppList && pageNavigator != null) {
            pageNavigator.setItemsPerPage(itemsPerPage);
            pageNavigator.setTotalItems(filteredApps.size());
            pageNavigator.setCurrentPage(currentPage);
        }

        updatePageIndicator();

        EditText et = findViewById(R.id.search_edit_text);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        et.setSingleLine(true);
        et.setCursorVisible(false);
        et.setMovementMethod(null);

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                filteredApps.clear();
                if (query.isEmpty() || appIndex == null) {
                    if (originalApps != null) {
                        filteredApps.addAll(originalApps);
                    }
                } else {
                    List<AppSearchHelper.AppItem> filtered = appIndex.filter(query);
                    filteredApps.addAll(filtered);
                }
                buildIndexSidebar();
                currentPage = 0;
                launcherAdapter.notifyDataSetChanged();
                updatePageIndicator();

                if (!query.isEmpty() && filteredApps.size() == 1) {
                    AppSearchHelper.AppItem app = filteredApps.get(0);
                    launchApp(app.label, app.packageName);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (!filteredApps.isEmpty()) {
                    AppSearchHelper.AppItem firstApp = filteredApps.get(0);
                    launchApp(firstApp.label, firstApp.packageName);
                }
                return true;
            }
            return false;
        });
    }

    public void launchApp(String label, String packageName) {
        if (OnyxHelper.isOnyxDevice() && OnyxHelper.isAppFrozen(this, packageName)) {
            OnyxHelper.showFrozenAppDialog(this, packageName, theme);
            return;
        }
        if (SystemAppHelper.isSystemApp(packageName)) {
            if (SystemAppHelper.launch(this, packageName)) {
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 100);
                return;
            }
        }
        if (!packageName.isEmpty()) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                boolean animate = prefs.getInt("app_launch_animation", 0) == 1;
                if (!animate) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }
                try {
                    startActivity(intent);
                    if (animate) {
                        overridePendingTransition(R.anim.dialog_fade_in, 0);
                    } else {
                        overridePendingTransition(0, 0);
                    }
                } catch (Exception e) {
                    if (OnyxHelper.isOnyxDevice() && OnyxHelper.isAppFrozen(this, packageName)) {
                        OnyxHelper.showFrozenAppDialog(this, packageName, theme);
                        return;
                    }
                    finish();
                    return;
                }
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 100);
                return;
            } else {
                if (OnyxHelper.isOnyxDevice() && OnyxHelper.isAppFrozen(this, packageName)) {
                    OnyxHelper.showFrozenAppDialog(this, packageName, theme);
                    return;
                }
            }
        }
        finish();
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
        int totalPages = (int) Math.ceil((double) filteredApps.size() / itemsPerPage);
        if (totalPages == 0)
            totalPages = 1;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private void removeAppFromList(String packageName) {
        originalApps.removeIf(app -> app.packageName.equals(packageName));
        filteredApps.removeIf(app -> app.packageName.equals(packageName));
        if (searchApps != null) {
            searchApps.removeIf(app -> app.packageName.equals(packageName));
            appIndex = new AppSearchHelper.IndexedApps(searchApps);
        }
        launcherAdapter.notifyDataSetChanged();
        updatePageIndicator();
    }

    private void loadApps(List<String> labels, List<String> packages) {
        installedAppLabels = labels;
        installedAppPackages = packages;
        originalApps = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String pkg = packages.get(i);
            String customLabel = prefs.getString("custom_label_" + pkg, "");
            String label = customLabel.isEmpty() ? labels.get(i) : customLabel;

            if (!customLabel.isEmpty()) {
                labels.set(i, customLabel);
            }

            originalApps.add(new AppSearchHelper.AppItem(label, pkg));
        }
        searchApps = new ArrayList<>(originalApps);
        appIndex = new AppSearchHelper.IndexedApps(searchApps);
        originalApps.removeIf(app -> hiddenApps.contains(app.packageName));
        filteredApps.clear();
        filteredApps.addAll(originalApps);
    }

    /**
     * Load the installed-app list off the main thread. Label resolution
     * ({@link ResolveInfo#loadLabel}) is a slow binder call, so each label is
     * fetched exactly once and sorting happens on the resolved strings.
     */
    private void loadAppsAsync(List<String> targetLabels, List<String> targetPackages) {
        final List<String> labels = new ArrayList<>();
        final List<String> packages = new ArrayList<>();

        Thread loader = new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            int queryFlags = 0;
            try {
                if (OnyxHelper.isOnyxDevice()) {
                    queryFlags = PackageManager.MATCH_DISABLED_COMPONENTS;
                }
            } catch (Exception ignored) {}
            List<ResolveInfo> apps = pm.queryIntentActivities(intent, queryFlags);
            List<String[]> labeled = new ArrayList<>(apps.size());
            for (ResolveInfo ri : apps) {
                labeled.add(new String[]{ri.loadLabel(pm).toString(), ri.activityInfo.packageName});
            }
            labeled.sort((a, b) -> a[0].compareToIgnoreCase(b[0]));
            for (String[] item : labeled) {
                labels.add(item[0]);
                packages.add(item[1]);
            }

            SystemAppHelper.insertCoreAppsForLauncher(labels, packages, 0);

            try {
                java.util.List<AppSearchHelper.AppItem> onyxApps = OnyxHelper.getAvailableOnyxApps(AppLauncherActivity.this);
                for (AppSearchHelper.AppItem item : onyxApps) {
                    if (!packages.contains(item.packageName)) {
                        labels.add(item.label);
                        packages.add(item.packageName);
                    }
                }
            } catch (Exception ignored) {}

            runOnUiThread(() -> {
                targetLabels.clear();
                targetLabels.addAll(labels);
                targetPackages.clear();
                targetPackages.addAll(packages);
                loadApps(targetLabels, targetPackages);
                buildIndexSidebar();
                currentPage = 0;
                launcherAdapter.notifyDataSetChanged();
                if (pageNavigator != null) {
                    pageNavigator.setTotalItems(filteredApps.size());
                }
                updatePageIndicator();
            });
        }, "TL-AppLoader");
        loader.setPriority(Thread.NORM_PRIORITY);
        loader.start();
    }

    private void renameApp(int position) {
        int globalPosition = scrollAppList ? position : currentPage * itemsPerPage + position;
        if (globalPosition >= 0 && globalPosition < filteredApps.size()) {
            AppSearchHelper.AppItem app = filteredApps.get(globalPosition);
            new RenameDialog(this, app.label, newName -> {
                app.label = newName;
                prefs.edit().putString("custom_label_" + app.packageName, newName).apply();

                if (originalApps != null) {
                    for (AppSearchHelper.AppItem originalApp : originalApps) {
                        if (originalApp.packageName.equals(app.packageName)) {
                            originalApp.label = newName;
                            break;
                        }
                    }
                }

                if (installedAppLabels != null && installedAppPackages != null) {
                    for (int i = 0; i < installedAppLabels.size(); i++) {
                        if (installedAppPackages.get(i).equals(app.packageName)) {
                            installedAppLabels.set(i, newName);
                            break;
                        }
                    }
                }

                if (searchApps != null) {
                    for (AppSearchHelper.AppItem searchApp : searchApps) {
                        if (searchApp.packageName.equals(app.packageName)) {
                            searchApp.label = newName;
                            break;
                        }
                    }
                    appIndex = new AppSearchHelper.IndexedApps(searchApps);
                }
                launcherAdapter.notifyDataSetChanged();
            }).show();
        }
    }

    private void hideApp(int position) {
        int globalPosition = scrollAppList ? position : currentPage * itemsPerPage + position;
        if (globalPosition >= 0 && globalPosition < filteredApps.size()) {
            AppSearchHelper.AppItem app = filteredApps.get(globalPosition);
            hiddenApps.add(app.packageName);
            prefs.edit().putStringSet("hidden_apps", hiddenApps).apply();
            originalApps.removeIf(a -> a.packageName.equals(app.packageName));
            filteredApps.removeIf(a -> a.packageName.equals(app.packageName));
            buildIndexSidebar();
            if (pageNavigator != null) {
                pageNavigator.setTotalItems(filteredApps.size());
            }
            launcherAdapter.notifyDataSetChanged();
            updatePageIndicator();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    public void refreshApps() {
        loadAppsAsync(installedAppLabels, installedAppPackages);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontHelper.applyToViewTree(this, findViewById(android.R.id.content));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));

            if (!keyboardShown && prefs.getBoolean("auto_focus_search", true)) {
                keyboardShown = true;
                EditText et = findViewById(R.id.search_edit_text);
                et.requestFocus();
                et.performClick();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
                }
            }
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

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            String action = prefs.getString("hardware_key_volume_up", "");
            if (action != null && !action.isEmpty()) {
                launchApp("", action);
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            String action = prefs.getString("hardware_key_volume_down", "");
            if (action != null && !action.isEmpty()) {
                launchApp("", action);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {
        finish();
        overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
    }

    private void buildIndexSidebar() {
        if (indexSidebar == null || indexSidebarHorizontal == null) return;

        if (appIndexSidebar == 0 || filteredApps == null || filteredApps.isEmpty()) {
            indexSidebar.setVisibility(View.GONE);
            indexSidebarHorizontal.setVisibility(View.GONE);
            return;
        }

        java.util.TreeSet<Character> letterSet = new java.util.TreeSet<>();
        for (int i = 0; i < filteredApps.size(); i++) {
            String label = filteredApps.get(i).label;
            if (label != null && !label.isEmpty()) {
                char first = Character.toUpperCase(label.charAt(0));
                if (first >= 'A' && first <= 'Z') {
                    letterSet.add(first);
                }
            }
        }
        if (letterSet.isEmpty()) {
            indexSidebar.setVisibility(View.GONE);
            indexSidebarHorizontal.setVisibility(View.GONE);
            return;
        }

        sidebarLetters = new String[letterSet.size()];
        int idx = 0;
        for (Character c : letterSet) {
            sidebarLetters[idx++] = String.valueOf(c);
        }
        int count = sidebarLetters.length;
        int textColor = ThemeUtils.getTextColor(theme, this);
        highlightedLetterIndex = -1;

        if (scrollAppList) {
            indexSidebarHorizontal.setVisibility(View.GONE);

            indexSidebar.removeAllViews();
            for (int i = 0; i < count; i++) {
                TextView tv = new TextView(this);
                tv.setText(sidebarLetters[i]);
                tv.setTextSize(sidebarLetterTextSize);
                tv.setTextColor(textColor);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
                indexSidebar.addView(tv);
            }
            indexSidebar.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            indexSidebar.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE: {
                        hideKeyboard();
                        int letterIdx = (int) (event.getY() / (v.getHeight() / (float) count));
                        if (letterIdx < 0) letterIdx = 0;
                        if (letterIdx >= count) letterIdx = count - 1;
                        if (letterIdx != highlightedLetterIndex) {
                            highlightedLetterIndex = letterIdx;
                            updateVerticalSidebarHighlight(textColor);
                            scrollToLetter(sidebarLetters[letterIdx]);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        highlightedLetterIndex = -1;
                        updateVerticalSidebarHighlight(textColor);
                        return true;
                }
                return false;
            });

            indexSidebar.setVisibility(View.VISIBLE);
        } else {
            indexSidebar.setVisibility(View.GONE);

            indexSidebarHorizontal.removeAllViews();
            float density = getResources().getDisplayMetrics().density;
            int horizTextSize = (int) (sidebarLetterTextSize * 0.9f);

            int bgColor = ThemeUtils.getBgColor(theme, this);
            for (int i = 0; i < count; i++) {
                TextView tv = new TextView(this);
                tv.setText(sidebarLetters[i]);
                tv.setTextSize(horizTextSize);
                tv.setTextColor(textColor);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                indexSidebarHorizontal.addView(tv);
            }
            indexSidebarHorizontal.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            indexSidebarHorizontal.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE: {
                        hideKeyboard();
                        int letterIdx = (int) (event.getX() / (v.getWidth() / (float) count));
                        if (letterIdx < 0) letterIdx = 0;
                        if (letterIdx >= count) letterIdx = count - 1;
                        if (letterIdx != highlightedLetterIndex) {
                            highlightedLetterIndex = letterIdx;
                            updateHorizontalSidebarHighlight(textColor, bgColor);
                            navigateToLetterPage(sidebarLetters[letterIdx]);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        highlightedLetterIndex = -1;
                        updateHorizontalSidebarHighlight(textColor, bgColor);
                        return true;
                }
                return false;
            });

            indexSidebarHorizontal.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToLetterPage(String letter) {
        if (filteredApps == null || filteredApps.isEmpty()) return;

        char target = letter.charAt(0);
        for (int i = 0; i < filteredApps.size(); i++) {
            String label = filteredApps.get(i).label;
            if (label != null && !label.isEmpty()
                    && Character.toUpperCase(label.charAt(0)) == target) {
                int targetPage = i / itemsPerPage;
                int totalPages = (int) Math.ceil((double) filteredApps.size() / itemsPerPage);
                if (targetPage >= totalPages) targetPage = totalPages - 1;
                if (targetPage < 0) targetPage = 0;

                currentPage = targetPage;
                if (pageNavigator != null) {
                    pageNavigator.setCurrentPage(targetPage);
                }
                launcherAdapter.notifyDataSetChanged();
                updatePageIndicator();
                EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                return;
            }
        }
    }

    private float getDockScale(int i, int highlighted) {
        if (highlighted < 0) return 1f;
        int dist = Math.abs(i - highlighted);
        if (dist == 0) return 2.2f;
        if (dist == 1) return 1.6f;
        if (dist == 2) return 1.2f;
        return 1f;
    }

    private void updateVerticalSidebarHighlight(int textColor) {
        int bgColor = ThemeUtils.getBgColor(theme, this);
        for (int i = 0; i < indexSidebar.getChildCount(); i++) {
            View child = indexSidebar.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (appIndexAnimation != 0) {
                    tv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    tv.setTextColor(textColor);
                    float scale = getDockScale(i, highlightedLetterIndex);
                    tv.setPivotX(tv.getWidth());
                    tv.setPivotY(tv.getHeight() / 2f);
                    tv.setScaleX(scale);
                    tv.setScaleY(scale);
                    tv.setTranslationZ(scale > 1f ? 10f : 0f);
                } else {
                    if (i == highlightedLetterIndex) {
                        tv.setBackgroundColor(textColor);
                        tv.setTextColor(bgColor);
                    } else {
                        tv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        tv.setTextColor(textColor);
                    }
                    tv.setScaleX(1f);
                    tv.setScaleY(1f);
                    tv.setTranslationZ(0f);
                }
            }
        }
    }

    private void updateHorizontalSidebarHighlight(int textColor, int bgColor) {
        for (int i = 0; i < indexSidebarHorizontal.getChildCount(); i++) {
            View child = indexSidebarHorizontal.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (appIndexAnimation != 0) {
                    tv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    tv.setTextColor(textColor);
                    float scale = getDockScale(i, highlightedLetterIndex);
                    tv.setPivotX(tv.getWidth() / 2f);
                    tv.setPivotY(0);
                    tv.setScaleX(scale);
                    tv.setScaleY(scale);
                    tv.setTranslationZ(scale > 1f ? 10f : 0f);
                } else {
                    if (i == highlightedLetterIndex) {
                        tv.setBackgroundColor(textColor);
                        tv.setTextColor(bgColor);
                    } else {
                        tv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        tv.setTextColor(textColor);
                    }
                    tv.setScaleX(1f);
                    tv.setScaleY(1f);
                    tv.setTranslationZ(0f);
                }
            }
        }
    }

    private void scrollToLetter(String letter) {
        if (filteredApps == null) return;
        RecyclerView rv = findViewById(R.id.app_selector_list);
        if (rv == null) return;

        char target = letter.charAt(0);
        for (int i = 0; i < filteredApps.size(); i++) {
            String label = filteredApps.get(i).label;
            if (label != null && !label.isEmpty()
                    && Character.toUpperCase(label.charAt(0)) == target) {
                final int pos = i;
                rv.scrollToPosition(pos);
                return;
            }
        }
    }

    private class AppLauncherAdapter extends RecyclerView.Adapter<AppLauncherAdapter.ViewHolder> {

        private List<AppSearchHelper.AppItem> apps;
        private AppLauncherActivity activity;
        private int theme;

        public AppLauncherAdapter(List<AppSearchHelper.AppItem> apps, AppLauncherActivity activity, int theme) {
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
            int globalPosition = activity.scrollAppList ? position : currentPage * itemsPerPage + position;
            AppSearchHelper.AppItem app = apps.get(globalPosition);
            holder.textView.setText(app.label);
            holder.textView.setTextSize(activity.textSize);
            holder.textView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);
            FontHelper.applyFont(activity, holder.textView);
            LauncherBackdropHelper.applySurfaceBackground(holder.itemView, activity.showWallpaperBackdrop,
                    activity.launcherSurfaceColor);
            ThemeUtils.applyTextColor(holder.textView, theme, activity);
            holder.itemView.setOnClickListener(v -> activity.launchApp(app.label, app.packageName));
            holder.itemView.setOnLongClickListener(v -> {
                new AppOptionsDialog(activity, app.packageName, null, null, () -> {
                    activity.renameApp(position);
                }, () -> {
                    activity.hideApp(position);
                }).show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            if (activity.scrollAppList) {
                return apps.size();
            }
            int start = currentPage * itemsPerPage;
            int end = Math.min(start + itemsPerPage, apps.size());
            return end - start;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.app_name);
            }
        }
    }
}
