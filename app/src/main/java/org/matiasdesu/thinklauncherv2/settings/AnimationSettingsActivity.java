package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;

import android.os.Build;

public class AnimationSettingsActivity extends AppCompatActivity {

    private int appIndexAnimation;
    private int appIndexSidebar;
    private int dialogAnimations;
    private int screenAnimations;
    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(AnimationSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animation_settings);

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
        rootLayout.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(rootLayout, theme, this);

        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        appIndexAnimation = prefs.getInt("app_index_animation", 0);
        screenAnimations = prefs.getInt("screen_animations", 0);
        dialogAnimations = prefs.getInt("dialog_animations", 0);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, screenAnimations == 1 ? R.anim.dialog_fade_out : 0);
        });

        View appIndexAnimationContainer = findViewById(R.id.app_index_animation_container);
        TextView appIndexAnimationValueTv = appIndexAnimationContainer.findViewById(R.id.value_text);
        appIndexAnimationValueTv.setText(getOnOffText(appIndexAnimation));

        TextView minusBtn = appIndexAnimationContainer.findViewById(R.id.btn_minus);
        TextView plusBtn = appIndexAnimationContainer.findViewById(R.id.btn_plus);

        minusBtn.setOnClickListener(v -> {
            appIndexAnimation = (appIndexAnimation - 1 + 2) % 2;
            appIndexAnimationValueTv.setText(getOnOffText(appIndexAnimation));
            prefs.edit().putInt("app_index_animation", appIndexAnimation).apply();
        });

        plusBtn.setOnClickListener(v -> {
            appIndexAnimation = (appIndexAnimation + 1) % 2;
            appIndexAnimationValueTv.setText(getOnOffText(appIndexAnimation));
            prefs.edit().putInt("app_index_animation", appIndexAnimation).apply();
        });

        View screenAnimationsContainer = findViewById(R.id.screen_animations_container);
        TextView screenAnimationsValueTv = screenAnimationsContainer.findViewById(R.id.value_text);
        screenAnimationsValueTv.setText(getOnOffText(screenAnimations));

        TextView minusScreenBtn = screenAnimationsContainer.findViewById(R.id.btn_minus);
        TextView plusScreenBtn = screenAnimationsContainer.findViewById(R.id.btn_plus);

        minusScreenBtn.setOnClickListener(v -> {
            screenAnimations = (screenAnimations - 1 + 2) % 2;
            screenAnimationsValueTv.setText(getOnOffText(screenAnimations));
            prefs.edit().putInt("screen_animations", screenAnimations).apply();
        });

        plusScreenBtn.setOnClickListener(v -> {
            screenAnimations = (screenAnimations + 1) % 2;
            screenAnimationsValueTv.setText(getOnOffText(screenAnimations));
            prefs.edit().putInt("screen_animations", screenAnimations).apply();
        });

        View dialogAnimationsContainer = findViewById(R.id.dialog_animations_container);
        TextView dialogAnimationsValueTv = dialogAnimationsContainer.findViewById(R.id.value_text);
        dialogAnimationsValueTv.setText(getOnOffText(dialogAnimations));

        TextView minusDialogBtn = dialogAnimationsContainer.findViewById(R.id.btn_minus);
        TextView plusDialogBtn = dialogAnimationsContainer.findViewById(R.id.btn_plus);

        minusDialogBtn.setOnClickListener(v -> {
            dialogAnimations = (dialogAnimations - 1 + 2) % 2;
            dialogAnimationsValueTv.setText(getOnOffText(dialogAnimations));
            prefs.edit().putInt("dialog_animations", dialogAnimations).apply();
        });

        plusDialogBtn.setOnClickListener(v -> {
            dialogAnimations = (dialogAnimations + 1) % 2;
            dialogAnimationsValueTv.setText(getOnOffText(dialogAnimations));
            prefs.edit().putInt("dialog_animations", dialogAnimations).apply();
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout animationLayout = findViewById(R.id.app_index_animation_layout);
        if (appIndexSidebar == 0) {
            animationLayout.setVisibility(View.GONE);
        } else {
            animationLayout.setVisibility(View.VISIBLE);
        }
    }

    private String getOnOffText(int pos) {
        return pos == 1 ? "ON" : "OFF";
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        refreshVisibility();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, screenAnimations == 1 ? R.anim.dialog_fade_out : 0);
    }
}
