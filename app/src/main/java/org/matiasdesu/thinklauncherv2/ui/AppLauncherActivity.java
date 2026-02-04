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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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
import org.matiasdesu.thinklauncherv2.utils.AppListSizeHelper;
import org.matiasdesu.thinklauncherv2.utils.AppSearchHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
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
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private AppLauncherAdapter launcherAdapter;
    private LinearLayout rootLayout;
    private boolean keyboardShown = false;
    private SharedPreferences prefs;
    private List<String> installedAppLabels;
    private List<String> installedAppPackages;
    private Set<String> hiddenApps;
    private boolean scrollAppList;

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
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_app_selector);

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

        EditText searchEditText = findViewById(R.id.search_edit_text);
        ThemeUtils.applyEditTextTheme(searchEditText, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        RecyclerView rv = findViewById(R.id.app_selector_list);
        ThemeUtils.applyBackgroundColor(rv, theme, this);

        textSize = prefs.getInt("text_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;

        itemsPerPage = AppListSizeHelper.calculateItemsPerPage(this, textSize);

        RecyclerView recyclerView = findViewById(R.id.app_selector_list);
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
                        AppLauncherActivity.this.updatePageIndicator();
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

        // Add settings option at the top
        installedAppLabels.add(0, "Launcher Settings");
        installedAppPackages.add(0, "launcher_settings");

        loadApps(installedAppLabels, installedAppPackages);

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
        
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                filteredApps.clear();
                if (query.isEmpty()) {
                    filteredApps.addAll(originalApps);
                } else {
                    List<AppSearchHelper.AppItem> filtered = AppSearchHelper.filterApps(installedAppLabels, installedAppPackages, query);
                    filteredApps.addAll(filtered);
                }
                currentPage = 0;
                launcherAdapter.notifyDataSetChanged();
                updatePageIndicator();

                if (!query.isEmpty() && filteredApps.size() == 1) {
                    AppSearchHelper.AppItem app = filteredApps.get(0);
                    launchApp(app.label, app.packageName);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
        if (totalPages == 0) totalPages = 1;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private void removeAppFromList(String packageName) {
        originalApps.removeIf(app -> app.packageName.equals(packageName));
        filteredApps.removeIf(app -> app.packageName.equals(packageName));
        launcherAdapter.notifyDataSetChanged();
        updatePageIndicator();
    }

    private void loadApps(List<String> labels, List<String> packages) {
        installedAppLabels = labels;
        installedAppPackages = packages;
        originalApps = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String pkg = packages.get(i);
            originalApps.add(new AppSearchHelper.AppItem(labels.get(i), pkg));
        }
        originalApps.removeIf(app -> hiddenApps.contains(app.packageName));
        filteredApps = new ArrayList<>(originalApps);
    }

    public void refreshApps() {
        // Reload installed apps
        List<String> labels = new ArrayList<>();
        List<String> packages = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        apps.sort((a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        for (ResolveInfo ri : apps) {
            labels.add(ri.loadLabel(pm).toString());
            packages.add(ri.activityInfo.packageName);
        }
        // Add settings
        labels.add(0, "Launcher Settings");
        packages.add(0, "launcher_settings");
        loadApps(labels, packages);
        currentPage = 0;
        launcherAdapter.notifyDataSetChanged();
        updatePageIndicator();
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
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
            ThemeUtils.applyBackgroundColor(holder.itemView, theme, activity);
            ThemeUtils.applyTextColor(holder.textView, theme, activity);
            holder.itemView.setOnClickListener(v -> activity.launchApp(app.label, app.packageName));
            holder.itemView.setOnLongClickListener(v -> {
                new AppOptionsDialog(activity, app.packageName).show();
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
