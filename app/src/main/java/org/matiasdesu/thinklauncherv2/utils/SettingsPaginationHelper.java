package org.matiasdesu.thinklauncherv2.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
 * indicators. Page boundaries are computed from the real measured height
 * of each item, so they adapt automatically to the font size (including a
 * custom font) and any other layout that changes row height.
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
    private int currentPage = 0;
    private boolean scrollAppList;
    private float touchDownX;
    private static final int SWIPE_THRESHOLD = 100;
    private boolean layoutReady = false;

    private List<Integer> pageStarts = new ArrayList<>();
    private int fallbackItemsPerPage = 1;

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

        fallbackItemsPerPage = SettingsListSizeHelper.calculateItemsPerPage(activity);

        if (!scrollAppList) {
            setupNavigationButtons();

            scrollView.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownX = event.getX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        return true;
                    case MotionEvent.ACTION_UP: {
                        float diffX = event.getX() - touchDownX;
                        int totalPages = getTotalPages();
                        if (totalPages > 1) {
                            if (diffX > SWIPE_THRESHOLD) {
                                currentPage = currentPage > 0 ? currentPage - 1 : totalPages - 1;
                                updateVisibleItems();
                                updatePageIndicator();
                                EinkRefreshHelper.refreshEink(activity.getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                            } else if (diffX < -SWIPE_THRESHOLD) {
                                currentPage = currentPage < totalPages - 1 ? currentPage + 1 : 0;
                                updateVisibleItems();
                                updatePageIndicator();
                                EinkRefreshHelper.refreshEink(activity.getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                            }
                        }
                        return true;
                    }
                }
                return false;
            });

            if (layoutReady) {
                rebuildPages();
            } else {
                buildFallbackPages();
                container.addOnLayoutChangeListener((v, left, top, right, bottom,
                        oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (!layoutReady && v.getHeight() > 0 && canMeasure()) {
                        layoutReady = true;
                        rebuildPages();
                    }
                });
            }
            updatePageIndicator();
        } else {
            scrollView.setOnTouchListener(null);
            updatePageIndicator();
        }
    }

    private boolean canMeasure() {
        return container.getWidth() > 0 && container.getHeight() > 0;
    }

    private int getContainerContentHeight() {
        return container.getHeight() - container.getPaddingTop() - container.getPaddingBottom();
    }

    private int getContainerContentWidth() {
        return container.getWidth() - container.getPaddingLeft() - container.getPaddingRight();
    }

    /**
     * Measure the real height of each visible item and compute page boundaries.
     * Each page holds as many consecutive items as fit within the container
     * height, so pages stay perfectly aligned even with different fonts.
     */
    public void rebuildPages() {
        if (scrollAppList || settingItems == null || settingItems.isEmpty()) {
            pageStarts = new ArrayList<>();
            pageStarts.add(0);
            pageStarts.add(settingItems == null ? 0 : settingItems.size());
            return;
        }

        int contentHeight = getContainerContentHeight();
        int contentWidth = getContainerContentWidth();
        if (contentHeight <= 0 || contentWidth <= 0) {
            buildFallbackPages();
            return;
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        List<Integer> heights = new ArrayList<>();
        for (View item : settingItems) {
            item.measure(widthSpec, heightSpec);
            int itemHeight = item.getMeasuredHeight();
            ViewGroup.LayoutParams lp = item.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                itemHeight += mlp.topMargin + mlp.bottomMargin;
            }
            heights.add(itemHeight);
        }

        List<Integer> newPageStarts = new ArrayList<>();
        newPageStarts.add(0);
        int used = 0;
        for (int i = 0; i < heights.size(); i++) {
            int h = heights.get(i);
            if (used > 0 && used + h > contentHeight) {
                newPageStarts.add(i);
                used = 0;
            }
            used += h;
        }
        if (newPageStarts.get(newPageStarts.size() - 1) != settingItems.size()) {
            newPageStarts.add(settingItems.size());
        }

        pageStarts = newPageStarts;

        if (currentPage >= getTotalPages()) {
            currentPage = Math.max(0, getTotalPages() - 1);
        }

        updateVisibleItems();
        updatePageIndicator();
    }

    private void buildFallbackPages() {
        int pageSize = fallbackItemsPerPage < 1 ? 1 : fallbackItemsPerPage;
        pageStarts = new ArrayList<>();
        for (int start = 0; start < settingItems.size(); start += pageSize) {
            pageStarts.add(start);
        }
        if (pageStarts.isEmpty()) {
            pageStarts.add(0);
        }
        pageStarts.add(settingItems.size());

        if (currentPage >= getTotalPages()) {
            currentPage = Math.max(0, getTotalPages() - 1);
        }
        updateVisibleItems();
        updatePageIndicator();
    }

    private int getTotalPages() {
        if (pageStarts == null || pageStarts.size() < 2) {
            return 1;
        }
        int pages = pageStarts.size() - 1;
        return pages < 1 ? 1 : pages;
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

        if (layoutReady && canMeasure()) {
            rebuildPages();
        } else {
            updateVisibleItems();
            updatePageIndicator();
        }
    }

    private void setupNavigationButtons() {
        ImageView prevButton = activity.findViewById(R.id.prev_page_button);
        ImageView nextButton = activity.findViewById(R.id.next_page_button);

        if (prevButton != null) {
            prevButton.setColorFilter(ThemeUtils.getTextColor(theme, activity));
            prevButton.setOnClickListener(v -> {
                int totalPages = getTotalPages();
                currentPage = currentPage > 0 ? currentPage - 1 : totalPages - 1;
                updateVisibleItems();
                updatePageIndicator();
                EinkRefreshHelper.refreshEink(activity.getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            });
        }

        if (nextButton != null) {
            nextButton.setColorFilter(ThemeUtils.getTextColor(theme, activity));
            nextButton.setOnClickListener(v -> {
                int totalPages = getTotalPages();
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

        if (pageStarts == null || pageStarts.isEmpty()) {
            return;
        }

        int pageIndex = Math.max(0, Math.min(currentPage, pageStarts.size() - 1));
        int start = pageStarts.get(pageIndex);
        int end = pageIndex + 1 < pageStarts.size() ? pageStarts.get(pageIndex + 1) : settingItems.size();

        for (int i = start; i < end && i < settingItems.size(); i++) {
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

        int totalPages = getTotalPages();
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