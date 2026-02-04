package org.matiasdesu.thinklauncherv2.ui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

/**
 * Utility class for handling swipe navigation and page controls in dialogs
 */
public class SwipePageNavigator {

    private final Context context;
    private final RecyclerView recyclerView;
    private final View container;
    private final PageChangeCallback callback;
    private final int theme;

    private int currentPage = 0;
    private int itemsPerPage;
    private int totalItems;

    public interface PageChangeCallback {
        void onPageChanged(int newPage);
        int getTotalPages();
        void updatePageIndicator();
    }

    public SwipePageNavigator(Context context, RecyclerView recyclerView, View container,
                            PageChangeCallback callback, int theme) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.container = container;
        this.callback = callback;
        this.theme = theme;

        setupGestureNavigation();
        setupButtonNavigation();
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    private void setupGestureNavigation() {
        // Apply theme to container
        ThemeUtils.applyBackgroundColor(container, theme, context);
        container.setClickable(true);

        final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float xDiff = e2.getX() - e1.getX();
                float yDiff = e2.getY() - e1.getY();
                if (Math.abs(xDiff) > Math.abs(yDiff) && Math.abs(xDiff) > 100 && Math.abs(velocityX) > 100) {
                    int totalPages = callback.getTotalPages();
                    if (xDiff > 0) {
                        // Swipe right - previous page (circular)
                        currentPage = currentPage > 0 ? currentPage - 1 : totalPages - 1;
                        callback.onPageChanged(currentPage);
                    } else if (xDiff < 0) {
                        // Swipe left - next page (circular)
                        currentPage = currentPage < totalPages - 1 ? currentPage + 1 : 0;
                        callback.onPageChanged(currentPage);
                    }
                    return true;
                }
                return false;
            }
        });

        container.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        // Only add RecyclerView listener if RecyclerView is provided
        if (recyclerView != null) {
            final float[] startX = new float[1];
            final float[] startY = new float[1];
            recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        startX[0] = e.getX();
                        startY[0] = e.getY();
                    } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                        float xDiff = Math.abs(e.getX() - startX[0]);
                        float yDiff = Math.abs(e.getY() - startY[0]);
                        if (xDiff > yDiff && xDiff > 50) {
                            return true;
                        }
                    }
                    gestureDetector.onTouchEvent(e);
                    return false;
                }

                @Override
                public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                    gestureDetector.onTouchEvent(e);
                }

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                }
            });
        }
    }

    private void setupButtonNavigation() {
        // Setup bottom bar buttons
        ImageView prevButton = container.getRootView().findViewById(R.id.prev_page_button);
        if (prevButton != null) {
            prevButton.setColorFilter(ThemeUtils.getTextColor(theme, context));
            prevButton.setOnClickListener(v -> {
                int totalPages = callback.getTotalPages();
                currentPage = currentPage > 0 ? currentPage - 1 : totalPages - 1;
                callback.onPageChanged(currentPage);
            });
        }

        ImageView nextButton = container.getRootView().findViewById(R.id.next_page_button);
        if (nextButton != null) {
            nextButton.setColorFilter(ThemeUtils.getTextColor(theme, context));
            nextButton.setOnClickListener(v -> {
                int totalPages = callback.getTotalPages();
                currentPage = currentPage < callback.getTotalPages() - 1 ? currentPage + 1 : 0;
                callback.onPageChanged(currentPage);
            });
        }
    }

    public void updatePageIndicator() {
        callback.updatePageIndicator();
    }
}