package org.matiasdesu.thinklauncherv2.utils;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a clickListener, mimicking repeat-click behavior.
 */
public class RepeatListener implements OnTouchListener {

    private final int initialInterval;
    private final int normalInterval;
    private final OnClickListener clickListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable handlerRunnable;
    private View touchedView;

    /**
     * @param initialInterval The interval after first click event
     * @param normalInterval  The interval after second and subsequent click
     *                        events
     * @param clickListener   The OnClickListener, that will be called
     */
    public RepeatListener(int initialInterval, int normalInterval,
            OnClickListener clickListener) {
        if (clickListener == null)
            throw new IllegalArgumentException("null runnable");
        if (initialInterval < 0 || normalInterval < 0)
            throw new IllegalArgumentException("negative interval");

        this.initialInterval = initialInterval;
        this.normalInterval = normalInterval;
        this.clickListener = clickListener;

        handlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (touchedView.isEnabled()) {
                    handler.postDelayed(this, normalInterval);
                    clickListener.onClick(touchedView);
                } else {
                    // stop if view becomes disabled
                    handler.removeCallbacks(handlerRunnable);
                    touchedView.setPressed(false);
                    touchedView = null;
                }
            }
        };
    }

    public RepeatListener(OnClickListener clickListener) {
        this(400, 100, clickListener);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(handlerRunnable);
                handler.postDelayed(handlerRunnable, initialInterval);
                touchedView = view;
                touchedView.setPressed(true);
                clickListener.onClick(view);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(handlerRunnable);
                if (touchedView != null) {
                    touchedView.setPressed(false);
                    touchedView = null;
                }
                return true;
        }
        return false;
    }
}
