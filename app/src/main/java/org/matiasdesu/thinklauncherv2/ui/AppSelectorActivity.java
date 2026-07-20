package org.matiasdesu.thinklauncherv2.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.AppListSizeHelper;
import org.matiasdesu.thinklauncherv2.utils.AppSearchHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.ui.RenameDialog;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class AppSelectorActivity extends AppCompatActivity {

    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_PACKAGE = "package";

    private int position;
    private int textSize;
    private boolean boldText;
    private List<AppSearchHelper.AppItem> originalApps;
    private List<AppSearchHelper.AppItem> filteredApps;
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private SharedPreferences prefs;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private boolean showWallpaperBackdrop;
    private int selectorSurfaceColor;
    private int appIndexSidebar;
    private int appIndexAnimation;
    private LinearLayout indexSidebar;
    private LinearLayout indexSidebarHorizontal;
    private SwipePageNavigator pageNavigator;
    private String[] sidebarLetters;
    private int highlightedLetterIndex = -1;
    private int sidebarLetterTextSize = 16;
    private AppSelectorAdapter adapter;
    private boolean appLauncherAnimations;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(AppSelectorActivity.this, MainActivity.class);
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
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_app_selector);

        position = getIntent().getIntExtra(EXTRA_POSITION, -1);

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
        selectorSurfaceColor = backdrop.surfaceColor;
        showWallpaperBackdrop = backdrop.showWallpaperBackdrop;

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        View topLayout = findViewById(R.id.top_layout);
        EditText searchEditText = findViewById(R.id.search_edit_text);
        ThemeUtils.applyEditTextTheme(searchEditText, theme, this);
        RecyclerView recyclerView = findViewById(R.id.app_selector_list);
        View container = findViewById(R.id.app_list_container);
        View listWrapper = findViewById(R.id.list_wrapper);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, selectorSurfaceColor,
                topLayout, searchEditText, recyclerView, container, listWrapper);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        textSize = prefs.getInt("text_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;
        appLauncherAnimations = prefs.getInt("screen_animations", 0) == 1;

        itemsPerPage = AppListSizeHelper.calculateItemsPerPage(this, textSize);

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
                            recyclerView.getAdapter().notifyDataSetChanged();
                            updatePageIndicator();
                            clearSidebarHighlight();
                            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                        }

                        @Override
                        public int getTotalPages() {
                            return (int) Math.ceil((double) filteredApps.size() / itemsPerPage);
                        }

                        @Override
                        public void updatePageIndicator() {
                            AppSelectorActivity.this.updatePageIndicator();
                        }
                    }, theme);
        }

        List<String> installedAppLabels = new ArrayList<>();
        List<String> installedAppPackages = new ArrayList<>();

        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        apps.sort((a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        for (ResolveInfo ri : apps) {
            installedAppLabels.add(ri.loadLabel(pm).toString());
            installedAppPackages.add(ri.activityInfo.packageName);
        }

        installedAppLabels.add(0, "None");
        installedAppPackages.add(0, "");

        if (position >= 0) {
            installedAppLabels.add(1, "Blank");
            installedAppPackages.add(1, "blank");
            installedAppLabels.add(2, "Folder");
            installedAppPackages.add(2, "folder");
            installedAppLabels.add(3, "Web App");
            installedAppPackages.add(3, "web_apps");
            installedAppLabels.add(4, "Hidden App");
            installedAppPackages.add(4, "hidden_app");
        }

        if (position == -2) {
            installedAppLabels.add(1, "Web App");
            installedAppPackages.add(1, "web_apps");
        }

        if (position == -3) {
            installedAppLabels.add(1, "System Default");
            installedAppPackages.add(1, "system_default");
        }

        int specialIndex;
        if (position >= 0) {
            specialIndex = 5;
        } else if (position == -2) {
            specialIndex = 2;
        } else if (position == -3) {
            specialIndex = 2;
        } else {
            specialIndex = 1;
        }

        installedAppLabels.add(specialIndex, "Launcher Settings");
        installedAppPackages.add(specialIndex, "launcher_settings");

        if (position != -2) {
            installedAppLabels.add(specialIndex + 1, "Notification Panel");
            installedAppPackages.add(specialIndex + 1, "notification_panel");

            installedAppLabels.add(specialIndex + 2, "App Launcher");
            installedAppPackages.add(specialIndex + 2, "app_launcher");

            installedAppLabels.add(specialIndex + 3, "KOReader History");
            installedAppPackages.add(specialIndex + 3, "koreader_history");

            installedAppLabels.add(specialIndex + 4, "Calendar Screen");
            installedAppPackages.add(specialIndex + 4, "calendar");

            if (position == -1) {
                installedAppLabels.add(specialIndex + 5, "Next Home Page");
                installedAppPackages.add(specialIndex + 5, "next_home_page");

                installedAppLabels.add(specialIndex + 6, "Previous Home Page");
                installedAppPackages.add(specialIndex + 6, "previous_home_page");
            }
        }

        originalApps = new ArrayList<>();
        for (int i = 0; i < installedAppLabels.size(); i++) {
            originalApps.add(new AppSearchHelper.AppItem(installedAppLabels.get(i), installedAppPackages.get(i)));
        }
        filteredApps = new ArrayList<>(originalApps);

        adapter = new AppSelectorAdapter(filteredApps, this, theme);
        recyclerView.setAdapter(adapter);

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
                if (query.isEmpty()) {
                    filteredApps.addAll(originalApps);
                } else {
                    List<AppSearchHelper.AppItem> filtered = AppSearchHelper.filterApps(installedAppLabels,
                            installedAppPackages, query);
                    filteredApps.addAll(filtered);
                }
                currentPage = 0;
                adapter.notifyDataSetChanged();
                updatePageIndicator();
                buildIndexSidebar();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        appIndexAnimation = prefs.getInt("app_index_animation", 0);
        indexSidebar = findViewById(R.id.index_sidebar);
        indexSidebarHorizontal = findViewById(R.id.index_sidebar_horizontal);
        buildIndexSidebar();
    }

    private void selectApp(String label, String pkg) {
        if (position >= 0) {
            if (pkg.equals("")) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LABEL, "Empty");
                resultIntent.putExtra(EXTRA_PACKAGE, "");
                resultIntent.putExtra(EXTRA_POSITION, position);
                setResult(RESULT_OK, resultIntent);
                finish();
                overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
            } else if (pkg.equals("blank")) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LABEL, "");
                resultIntent.putExtra(EXTRA_PACKAGE, "blank");
                resultIntent.putExtra(EXTRA_POSITION, position);
                setResult(RESULT_OK, resultIntent);
                finish();
                overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
            } else if (pkg.equals("folder")) {
                new RenameDialog(this, "New Folder", newLabel -> {
                    String folderName = newLabel.isEmpty() ? "New Folder" : newLabel;
                    String folderId = "folder_" + System.currentTimeMillis();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_LABEL, folderName);
                    resultIntent.putExtra(EXTRA_PACKAGE, folderId);
                    resultIntent.putExtra(EXTRA_POSITION, position);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
                }).show();
            } else if (pkg.equals("web_apps")) {
                new WebAppDialog(this, "New Web App", "", (name, url) -> {
                    String webAppId = "webapp_" + System.currentTimeMillis();
                    prefs.edit().putString(webAppId + "_url", url).apply();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_LABEL, name);
                    resultIntent.putExtra(EXTRA_PACKAGE, webAppId);
                    resultIntent.putExtra(EXTRA_POSITION, position);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
                }).show();
            } else if (pkg.equals("hidden_app")) {
                Intent intent = new Intent(AppSelectorActivity.this, AppSelectorActivity.class);
                intent.putExtra(EXTRA_POSITION, -4);
                if (!appLauncherAnimations) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }
                startActivityForResult(intent, 1001);
                if (appLauncherAnimations) {
                    overridePendingTransition(R.anim.dialog_fade_in, 0);
                }
            } else {
                new RenameDialog(this, label, newLabel -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_LABEL, newLabel.isEmpty() ? label : newLabel);
                    resultIntent.putExtra(EXTRA_PACKAGE, pkg);
                    resultIntent.putExtra(EXTRA_POSITION, position);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
                }).show();
            }
        } else if (position == -2) {
            if (pkg.equals("web_apps")) {
                new WebAppDialog(this, "New Web App", "", (name, url) -> {
                    String webAppId = "webapp_" + System.currentTimeMillis();
                    prefs.edit().putString(webAppId + "_url", url).apply();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_LABEL, name);
                    resultIntent.putExtra(EXTRA_PACKAGE, webAppId);
                    resultIntent.putExtra(EXTRA_POSITION, position);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
                }).show();
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LABEL, label);
                resultIntent.putExtra(EXTRA_PACKAGE, pkg);
                resultIntent.putExtra(EXTRA_POSITION, position);
                setResult(RESULT_OK, resultIntent);
                finish();
                overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
            }
        } else {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_LABEL, label);
            resultIntent.putExtra(EXTRA_PACKAGE, pkg);
            resultIntent.putExtra(EXTRA_POSITION, position);
            setResult(RESULT_OK, resultIntent);
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String pkg = data.getStringExtra(EXTRA_PACKAGE);
            String label = data.getStringExtra(EXTRA_LABEL);
            if (pkg != null && !pkg.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LABEL, label != null ? label : "");
                resultIntent.putExtra(EXTRA_PACKAGE, "hidden_app_" + pkg);
                resultIntent.putExtra(EXTRA_POSITION, position);
                setResult(RESULT_OK, resultIntent);
                finish();
                overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
            }
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
        int totalPages = (int) Math.ceil((double) filteredApps.size() / itemsPerPage);
        if (totalPages == 0)
            totalPages = 1;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
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
                adapter.notifyDataSetChanged();
                updatePageIndicator();
                EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                return;
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
                rv.scrollToPosition(i);
                return;
            }
        }
    }

    private void clearSidebarHighlight() {
        highlightedLetterIndex = -1;
        if (indexSidebar != null && indexSidebar.getVisibility() == View.VISIBLE) {
            int textColor = ThemeUtils.getTextColor(theme, this);
            updateVerticalSidebarHighlight(textColor);
        } else if (indexSidebarHorizontal != null && indexSidebarHorizontal.getVisibility() == View.VISIBLE) {
            int textColor = ThemeUtils.getTextColor(theme, this);
            int bgColor = ThemeUtils.getBgColor(theme, this);
            updateHorizontalSidebarHighlight(textColor, bgColor);
        }
    }

    private class AppSelectorAdapter extends RecyclerView.Adapter<AppSelectorAdapter.ViewHolder> {

        private List<AppSearchHelper.AppItem> apps;
        private AppSelectorActivity activity;
        private int theme;

        public AppSelectorAdapter(List<AppSearchHelper.AppItem> apps, AppSelectorActivity activity, int theme) {
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
            LauncherBackdropHelper.applySurfaceBackground(holder.itemView, activity.showWallpaperBackdrop,
                    activity.selectorSurfaceColor);
            ThemeUtils.applyTextColor(holder.textView, theme, activity);
            holder.itemView.setOnClickListener(v -> activity.selectApp(app.label, app.packageName));
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
