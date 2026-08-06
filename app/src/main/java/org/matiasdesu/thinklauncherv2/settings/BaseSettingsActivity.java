package org.matiasdesu.thinklauncherv2.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

/**
 * Base activity for settings screens with pagination. Handles theme setup,
 * status bar colors, back button, pagination helper wiring and the eink
 * refresh on focus. Screens only need to call {@link #initPagination(Runnable)}
 * (and set visibility inside the updater) to get pagination working.
 */
public abstract class BaseSettingsActivity extends AppCompatActivity {

    protected SharedPreferences prefs;
    protected int theme;
    protected boolean screenAnimations;
    protected SettingsPaginationHelper paginationHelper;
    protected View rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        rootLayout = findViewById(android.R.id.content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    ThemeUtils.isDarkTheme(theme, this) ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
    }

    /** The layout resource for this screen. */
    @LayoutRes
    protected abstract int getLayoutResId();

    /**
     * Wire the pagination helper to the standard container ids. The
     * visibilityUpdater should only toggle visibility of items (it must not
     * call updateVisibleItemsList itself).
     */
    protected void initPagination(Runnable visibilityUpdater) {
        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(visibilityUpdater);
    }

    /** Refresh pagination after a visibility change. */
    protected void refreshPagination() {
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontHelper.applyToViewTree(this, rootLayout);
        DialogEffectHelper.applyCornerRadiusToTree(rootLayout, this);
        refreshPagination();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
    }
}
