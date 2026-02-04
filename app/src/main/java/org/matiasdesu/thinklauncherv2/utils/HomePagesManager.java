package org.matiasdesu.thinklauncherv2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.SwipePageNavigator;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages home pages functionality, including pagination and app storage per page.
 */
public class HomePagesManager {

    private final Context context;
    private final SharedPreferences prefs;
    private int currentPage = 0;
    private int homePages;
    private int homeColumns;
    private int maxApps;
    private SwipePageNavigator pageNavigator;
    private TextView pageIndicator;
    private List<String> appLabels;
    private List<String> appPackages;
    private LinearLayout[] appSlots;

    public HomePagesManager(Context context, SharedPreferences prefs, int homePages, int homeColumns, int maxApps) {
        this.context = context;
        this.prefs = prefs;
        this.homePages = homePages;
        this.homeColumns = homeColumns;
        this.maxApps = maxApps;
        this.appLabels = new ArrayList<>();
        this.appPackages = new ArrayList<>();
    }

    public void setPageIndicator(TextView pageIndicator) {
        this.pageIndicator = pageIndicator;
    }

    public void setAppSlots(LinearLayout[] appSlots) {
        this.appSlots = appSlots;
    }

    public void setupPagination(RecyclerView recyclerView, View container, int theme) {
        if (homePages > 1) {
            pageNavigator = new SwipePageNavigator(context, recyclerView, container,
                new SwipePageNavigator.PageChangeCallback() {
                    @Override
                    public void onPageChanged(int newPage) {
                        currentPage = newPage;
                        loadAppsForCurrentPage();
                        updatePageIndicator();
                    }

                    @Override
                    public int getTotalPages() {
                        return homePages;
                    }

                    @Override
                    public void updatePageIndicator() {
                        HomePagesManager.this.updatePageIndicator();
                    }
                }, theme);
            pageNavigator.setItemsPerPage(homeColumns * maxApps);
            pageNavigator.setTotalItems(homePages);
            pageNavigator.setCurrentPage(currentPage);
        }
    }

    public void loadAppsForCurrentPage() {
        appLabels.clear();
        appPackages.clear();
        int totalApps = homeColumns * maxApps;
        for (int i = 0; i < totalApps; i++) {
            String label = prefs.getString("slot_label_page_" + currentPage + "_" + i, "Empty");
            String pkg = prefs.getString("slot_pkg_page_" + currentPage + "_" + i, "");
            appLabels.add(label);
            appPackages.add(pkg);
        }
    }

    public void saveAppsForCurrentPage() {
        int totalApps = homeColumns * maxApps;
        for (int i = 0; i < totalApps; i++) {
            prefs.edit().putString("slot_label_page_" + currentPage + "_" + i, appLabels.get(i))
                    .putString("slot_pkg_page_" + currentPage + "_" + i, appPackages.get(i))
                    .apply();
        }
    }

    public void updatePageIndicator() {
        if (pageIndicator != null && homePages > 1) {
            pageIndicator.setText((currentPage + 1) + " / " + homePages);
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
        loadAppsForCurrentPage();
        updatePageIndicator();
    }

    public List<String> getAppLabels() {
        return appLabels;
    }

    public List<String> getAppPackages() {
        return appPackages;
    }

    public void setAppLabel(int position, String label) {
        if (position >= 0 && position < appLabels.size()) {
            appLabels.set(position, label);
            saveAppsForCurrentPage();
        }
    }

    public void setAppPackage(int position, String pkg) {
        if (position >= 0 && position < appPackages.size()) {
            appPackages.set(position, pkg);
            saveAppsForCurrentPage();
        }
    }
}