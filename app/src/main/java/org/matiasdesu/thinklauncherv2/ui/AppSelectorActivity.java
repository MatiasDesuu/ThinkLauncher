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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.view.WindowManager;
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
import org.matiasdesu.thinklauncherv2.ui.RenameDialog;

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
    private LinearLayout rootLayout;
    private boolean scrollAppList;

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
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_app_selector);

        position = getIntent().getIntExtra(EXTRA_POSITION, -1);

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

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, this);
        }

        View topLayout = findViewById(R.id.top_layout);
        ThemeUtils.applyBackgroundColor(topLayout, theme, this);

        EditText searchEditText = findViewById(R.id.search_edit_text);
        ThemeUtils.applyEditTextTheme(searchEditText, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        RecyclerView recyclerView = findViewById(R.id.app_selector_list);
        ThemeUtils.applyBackgroundColor(recyclerView, theme, this);

        textSize = prefs.getInt("text_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;

        itemsPerPage = AppListSizeHelper.calculateItemsPerPage(this, textSize);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }
        });

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
        }

        if (position >= 0) {
            int folderIndex = 2;
            installedAppLabels.add(folderIndex, "Folder");
            installedAppPackages.add(folderIndex, "folder");
        }

        if (position >= 0 || position == -2) {
            int webAppIndex = position >= 0 ? 3 : 1;
            installedAppLabels.add(webAppIndex, "Web App");
            installedAppPackages.add(webAppIndex, "web_apps");
        }

        if (position != -3) {
            int specialIndex = position >= 0 ? 4 : (position == -2 ? 2 : 1);
            installedAppLabels.add(specialIndex, "Launcher Settings");
            installedAppPackages.add(specialIndex, "launcher_settings");

            if (position != -2) {
                installedAppLabels.add(specialIndex + 1, "Notification Panel");
                installedAppPackages.add(specialIndex + 1, "notification_panel");
            }

            if (position != -2) {
                installedAppLabels.add(specialIndex + 2, "App Launcher");
                installedAppPackages.add(specialIndex + 2, "app_launcher");
            }

            if (position != -2) {
                installedAppLabels.add(specialIndex + 3, "KOReader History");
                installedAppPackages.add(specialIndex + 3, "koreader_history");
            }

            if (position == -1) {
                int nextPageIndex = specialIndex + 4;
                installedAppLabels.add(nextPageIndex, "Next Home Page");
                installedAppPackages.add(nextPageIndex, "next_home_page");
            }

            if (position == -1) {
                installedAppLabels.add(specialIndex + 4, "Previous Home Page");
                installedAppPackages.add(specialIndex + 4, "previous_home_page");
            }
        }

        if (position == -3) {
            installedAppLabels.add(1, "System Default");
            installedAppPackages.add(1, "system_default");
        }

        originalApps = new ArrayList<>();
        for (int i = 0; i < installedAppLabels.size(); i++) {
            originalApps.add(new AppSearchHelper.AppItem(installedAppLabels.get(i), installedAppPackages.get(i)));
        }
        filteredApps = new ArrayList<>(originalApps);

        AppSelectorAdapter adapter = new AppSelectorAdapter(filteredApps, this, theme);
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
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
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
                overridePendingTransition(0, 0);
            } else if (pkg.equals("blank")) {

                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LABEL, "");
                resultIntent.putExtra(EXTRA_PACKAGE, "blank");
                resultIntent.putExtra(EXTRA_POSITION, position);
                setResult(RESULT_OK, resultIntent);
                finish();
                overridePendingTransition(0, 0);
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
                    overridePendingTransition(0, 0);
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
                    overridePendingTransition(0, 0);
                }).show();
            } else {

                new RenameDialog(this, label, newLabel -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_LABEL, newLabel.isEmpty() ? label : newLabel);
                    resultIntent.putExtra(EXTRA_PACKAGE, pkg);
                    resultIntent.putExtra(EXTRA_POSITION, position);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    overridePendingTransition(0, 0);
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
                    overridePendingTransition(0, 0);
                }).show();
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LABEL, label);
                resultIntent.putExtra(EXTRA_PACKAGE, pkg);
                resultIntent.putExtra(EXTRA_POSITION, position);
                setResult(RESULT_OK, resultIntent);
                finish();
                overridePendingTransition(0, 0);
            }
        } else {

            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_LABEL, label);
            resultIntent.putExtra(EXTRA_PACKAGE, pkg);
            resultIntent.putExtra(EXTRA_POSITION, position);
            setResult(RESULT_OK, resultIntent);
            finish();
            overridePendingTransition(0, 0);
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
        super.onBackPressed();
        overridePendingTransition(0, 0);
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
            ThemeUtils.applyBackgroundColor(holder.itemView, theme, activity);
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
