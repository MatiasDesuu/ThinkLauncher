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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.services.LockAccessibilityService;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;

import android.os.Build;

public class GestureSettingsActivity extends AppCompatActivity {

    private LinearLayout rootLayout;
    private int doubleTapLock;
    private SettingsPaginationHelper paginationHelper;
    private int theme;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    // Bring MainActivity to front
                    Intent mainIntent = new Intent(GestureSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentGestureDirection = savedInstanceState.getString("currentGestureDirection");
        }
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_settings);

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

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        TextView swipeLeftTv = findViewById(R.id.swipe_left_app);
        TextView swipeRightTv = findViewById(R.id.swipe_right_app);
        TextView swipeDownTv = findViewById(R.id.swipe_down_app);
        TextView swipeUpTv = findViewById(R.id.swipe_up_app);
        TextView clockAppTv = findViewById(R.id.clock_app);
        TextView dateAppTv = findViewById(R.id.date_app);

        String leftLabel = prefs.getString("swipe_left_label", "None");
        String rightLabel = prefs.getString("swipe_right_label", "None");
        String downLabel = prefs.getString("swipe_down_label", "Notification Panel");
        String upLabel = prefs.getString("swipe_up_label", "App Launcher");
        String clockLabel = prefs.getString("clock_app_label", "System Default");
        String dateLabel = prefs.getString("date_app_label", "System Default");

        // Set default apps if not set
        if (!prefs.contains("swipe_down_app")) {
            prefs.edit().putString("swipe_down_app", "notification_panel").putString("swipe_down_label", "Notification Panel").apply();
        }
        if (!prefs.contains("swipe_up_app")) {
            prefs.edit().putString("swipe_up_app", "app_launcher").putString("swipe_up_label", "App Launcher").apply();
        }
        if (!prefs.contains("clock_app_pkg")) {
            prefs.edit().putString("clock_app_pkg", "system_default").putString("clock_app_label", "System Default").apply();
        }
        if (!prefs.contains("date_app_pkg")) {
            prefs.edit().putString("date_app_pkg", "system_default").putString("date_app_label", "System Default").apply();
        }

        doubleTapLock = prefs.getInt("double_tap_lock", 0);

        swipeLeftTv.setText(leftLabel);
        swipeRightTv.setText(rightLabel);
        swipeDownTv.setText(downLabel);
        swipeUpTv.setText(upLabel);
        clockAppTv.setText(clockLabel);
        dateAppTv.setText(dateLabel);

        swipeLeftTv.setOnClickListener(v -> selectAppForGesture("left"));
        swipeRightTv.setOnClickListener(v -> selectAppForGesture("right"));
        swipeDownTv.setOnClickListener(v -> selectAppForGesture("down"));
        swipeUpTv.setOnClickListener(v -> selectAppForGesture("up"));
        clockAppTv.setOnClickListener(v -> selectAppForGesture("clock"));
        dateAppTv.setOnClickListener(v -> selectAppForGesture("date"));

        View doubleTapLockContainer = findViewById(R.id.double_tap_lock_container);
        TextView doubleTapLockValueTv = doubleTapLockContainer.findViewById(R.id.value_text);
        doubleTapLockValueTv.setText(getDoubleTapLockText(doubleTapLock));
        doubleTapLockValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(doubleTapLockValueTv, new String[]{"OFF", "ON"}));

        TextView minusDoubleTapBtn = doubleTapLockContainer.findViewById(R.id.btn_minus);
        TextView plusDoubleTapBtn = doubleTapLockContainer.findViewById(R.id.btn_plus);

        minusDoubleTapBtn.setOnClickListener(v -> {
            doubleTapLock = (doubleTapLock - 1 + 2) % 2;
            doubleTapLockValueTv.setText(getDoubleTapLockText(doubleTapLock));
            prefs.edit().putInt("double_tap_lock", doubleTapLock).apply();
            if (doubleTapLock == 1 && !LockAccessibilityService.isServiceRunning()) {
                Toast.makeText(this, "Please enable accessibility to use double tap to lock", Toast.LENGTH_SHORT).show();
            }
        });

        plusDoubleTapBtn.setOnClickListener(v -> {
            doubleTapLock = (doubleTapLock + 1) % 2;
            doubleTapLockValueTv.setText(getDoubleTapLockText(doubleTapLock));
            prefs.edit().putInt("double_tap_lock", doubleTapLock).apply();
            if (doubleTapLock == 1 && !LockAccessibilityService.isServiceRunning()) {
                Toast.makeText(this, "Please enable accessibility to use double tap to lock", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);
        
        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(null);
    }

    private String currentGestureDirection;

    private void selectAppForGesture(String direction) {
        currentGestureDirection = direction;
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION, direction.equals("clock") || direction.equals("date") ? -3 : -1);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, 1000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentGestureDirection", currentGestureDirection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            if (currentGestureDirection == null) {
                // Try to recover from intent extras if possible, or just ignore
                return;
            }
            String label = data.getStringExtra(AppSelectorActivity.EXTRA_LABEL);
            String pkg = data.getStringExtra(AppSelectorActivity.EXTRA_PACKAGE);
            
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            TextView tv;
            int resId;
            String appKey, labelKey;
            switch (currentGestureDirection) {
                case "left":
                    appKey = "swipe_left_app";
                    labelKey = "swipe_left_label";
                    resId = R.id.swipe_left_app;
                    break;
                case "right":
                    appKey = "swipe_right_app";
                    labelKey = "swipe_right_label";
                    resId = R.id.swipe_right_app;
                    break;
                case "down":
                    appKey = "swipe_down_app";
                    labelKey = "swipe_down_label";
                    resId = R.id.swipe_down_app;
                    break;
                case "up":
                    appKey = "swipe_up_app";
                    labelKey = "swipe_up_label";
                    resId = R.id.swipe_up_app;
                    break;
                case "clock":
                    appKey = "clock_app_pkg";
                    labelKey = "clock_app_label";
                    resId = R.id.clock_app;
                    break;
                case "date":
                    appKey = "date_app_pkg";
                    labelKey = "date_app_label";
                    resId = R.id.date_app;
                    break;
                default:
                    return;
            }
            prefs.edit().putString(appKey, pkg).putString(labelKey, label).apply();
            tv = findViewById(resId);
            if (pkg.equals("system_default")) {
                tv.setText("System Default");
            } else {
                tv.setText(pkg.isEmpty() ? "None" : label);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    private String getDoubleTapLockText(int pos) {
        switch (pos) {
            case 0: return "OFF";
            case 1: return "ON";
            default: return "OFF";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), Context.RECEIVER_NOT_EXPORTED);
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
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
        unregisterReceiver(homeButtonReceiver);
    }
}