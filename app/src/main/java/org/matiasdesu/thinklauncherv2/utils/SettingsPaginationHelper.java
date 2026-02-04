package org.matiasdesu.thinklauncherv2.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable helper class for managing pagination in Settings screens.
 * Handles showing/hiding items per page, navigation buttons, and page
 * indicators.
 */
public class SettingsPaginationHelper {

    private final Activity activity;
    private final SharedPreferences prefs;
    private final int theme;
    private final LinearLayout settingsItemsContainer;
    private final ScrollView scrollView;
    private final FrameLayout container;

    private List<View> settingItems;
    private List<View> allChildren;
    private Runnable visibilityUpdater;
    private int itemsPerPage;
    private int currentPage = 0;
    private boolean scrollAppList;

    public SettingsPaginationHelper(Activity activity, int theme,
            LinearLayout settingsItemsContainer,
            ScrollView scrollView,
            FrameLayout container) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences("prefs", Activity.MODE_PRIVATE);
        this.theme = theme;
        this.settingsItemsContainer = settingsItemsContainer;
        this.scrollView = scrollView;
        this.container = container;
    }

    /**
     * Initialize pagination system
     * 
     * @param visibilityUpdater Runnable to update visibility of items based on
     *                          settings
     */
    public void initialize(Runnable visibilityUpdater) {
        this.visibilityUpdater = visibilityUpdater;
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;

        allChildren = new ArrayList<>();
        for (int i = 0; i < settingsItemsContainer.getChildCount(); i++) {
            allChildren.add(settingsItemsContainer.getChildAt(i));
        }

        for (View child : allChildren) {
            setVisibilityRecursive(child, View.VISIBLE);
        }

        if (this.visibilityUpdater != null) {
            this.visibilityUpdater.run();
        }

        settingItems = new ArrayList<>();
        for (View child : allChildren) {
            collectVisibleItems(child, settingItems);
        }

        if (!scrollAppList) {
            itemsPerPage = SettingsListSizeHelper.calculateItemsPerPage(activity);

            setupNavigationButtons();

            scrollView.setOnTouchListener((v, event) -> true);

            updateVisibleItems();
        } else {
            scrollView.setOnTouchListener(null);
        }

        updatePageIndicator();
    }

    private void collectVisibleItems(View view, List<View> items) {
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }

        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            if (layout.getOrientation() == LinearLayout.VERTICAL && layout.getChildCount() > 0) {
                if (layout.getChildCount() > 1) {
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        collectVisibleItems(layout.getChildAt(i), items);
                    }
                    return;
                }
            }
        }

        items.add(view);
    }

    private void setVisibilityRecursive(View view, int visibility) {
        view.setVisibility(visibility);
        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            for (int i = 0; i < layout.getChildCount(); i++) {
                setVisibilityRecursive(layout.getChildAt(i), visibility);
            }
        }
    }

    public void updateVisibleItemsList() {
        boolean newScrollAppList = prefs.getInt("scroll_app_list", 0) == 1;
        if (this.scrollAppList != newScrollAppList) {
            initialize(this.visibilityUpdater);
            return;
        }

        if (scrollAppList) {
            if (visibilityUpdater != null) {
                visibilityUpdater.run();
            }
            return;
        }

        if (allChildren != null) {
            for (View child : allChildren) {
                setVisibilityRecursive(child, View.VISIBLE);
            }
        }

        if (visibilityUpdater != null) {
            visibilityUpdater.run();
        }

        settingItems.clear();
        if (allChildren != null) {
            for (View child : allChildren) {
                collectVisibleItems(child, settingItems);
            }
        } else {
            for (int i = 0; i < settingsItemsContainer.getChildCount(); i++) {
                View child = settingsItemsContainer.getChildAt(i);
                collectVisibleItems(child, settingItems);
            }
        }

        int totalPages = (int) Math.ceil((double) settingItems.size() / itemsPerPage);
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1;
        }

        updateVisibleItems();
        updatePageIndicator();
    }

    private void setupNavigationButtons() {
        ImageView prevButton = activity.findViewById(R.id.prev_page_button);
        ImageView nextButton = activity.findViewById(R.id.next_page_button);

        if (prevButton != null) {
            prevButton.setColorFilter(ThemeUtils.getTextColor(theme, activity));
            prevButton.setOnClickListener(v -> {
                int totalPages = (int) Math.ceil((double) settingItems.size() / itemsPerPage);
                currentPage = currentPage > 0 ? currentPage - 1 : totalPages - 1;
                updateVisibleItems();
                updatePageIndicator();
                EinkRefreshHelper.refreshEink(activity.getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            });
        }

        if (nextButton != null) {
            nextButton.setColorFilter(ThemeUtils.getTextColor(theme, activity));
            nextButton.setOnClickListener(v -> {
                int totalPages = (int) Math.ceil((double) settingItems.size() / itemsPerPage);
                currentPage = currentPage < totalPages - 1 ? currentPage + 1 : 0;
                updateVisibleItems();
                updatePageIndicator();
                EinkRefreshHelper.refreshEink(activity.getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            });
        }
    }

    private void updateVisibleItems() {
        if (scrollAppList) {
            for (View item : settingItems) {
                item.setVisibility(View.VISIBLE);
            }
            return;
        }

        for (View item : settingItems) {
            item.setVisibility(View.GONE);
        }

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, settingItems.size());

        for (int i = start; i < end; i++) {
            settingItems.get(i).setVisibility(View.VISIBLE);
        }
    }

    private void updatePageIndicator() {
        TextView pageIndicator = activity.findViewById(R.id.page_indicator);
        View bottomDivider = activity.findViewById(R.id.bottom_divider);
        View bottomBar = activity.findViewById(R.id.bottom_bar);
        ImageView prevButton = activity.findViewById(R.id.prev_page_button);
        ImageView nextButton = activity.findViewById(R.id.next_page_button);

        if (scrollAppList) {
            if (pageIndicator != null)
                pageIndicator.setVisibility(View.GONE);
            if (bottomDivider != null)
                bottomDivider.setVisibility(View.GONE);
            if (bottomBar != null)
                bottomBar.setVisibility(View.GONE);
            return;
        }

        if (pageIndicator != null)
            pageIndicator.setVisibility(View.VISIBLE);
        if (bottomDivider != null)
            bottomDivider.setVisibility(View.VISIBLE);
        if (bottomBar != null)
            bottomBar.setVisibility(View.VISIBLE);

        int totalPages = (int) Math.ceil((double) settingItems.size() / itemsPerPage);
        if (totalPages == 0)
            totalPages = 1;

        if (pageIndicator != null) {
            pageIndicator.setText("Page " + (currentPage + 1) + " / " + totalPages);
        }

        int bgColor = ThemeUtils.getBgColor(theme, activity);
        int textColor = ThemeUtils.getTextColor(theme, activity);

        if (bottomBar != null)
            bottomBar.setBackgroundColor(bgColor);
        if (bottomDivider != null)
            bottomDivider.setBackgroundColor(textColor);
        if (pageIndicator != null)
            ThemeUtils.applyTextColor(pageIndicator, theme, activity);

        if (prevButton != null) {
            prevButton.setColorFilter(textColor);
        }
        if (nextButton != null) {
            nextButton.setColorFilter(textColor);
        }
    }
}
