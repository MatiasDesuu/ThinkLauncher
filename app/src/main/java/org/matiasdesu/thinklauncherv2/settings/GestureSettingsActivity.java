package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.matiasdesu.thinklauncherv2.services.LockAccessibilityService;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class GestureSettingsActivity extends BaseSettingsActivity {

    private int doubleTapLock;
    private String currentGestureDirection;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {

                    Intent mainIntent = new Intent(GestureSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_gesture_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentGestureDirection = savedInstanceState.getString("currentGestureDirection");
        }
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

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

        if (!prefs.contains("swipe_down_app")) {
            prefs.edit().putString("swipe_down_app", "notification_panel")
                    .putString("swipe_down_label", "Notification Panel").apply();
        }
        if (!prefs.contains("swipe_up_app")) {
            prefs.edit().putString("swipe_up_app", "app_launcher").putString("swipe_up_label", "App Launcher").apply();
        }
        if (!prefs.contains("clock_app_pkg")) {
            prefs.edit().putString("clock_app_pkg", "system_default").putString("clock_app_label", "System Default")
                    .apply();
        }
        if (!prefs.contains("date_app_pkg")) {
            prefs.edit().putString("date_app_pkg", "system_default").putString("date_app_label", "System Default")
                    .apply();
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

        LinearLayout customGesturesButton = findViewById(R.id.custom_gestures_button);
        customGesturesButton.setOnClickListener(v -> {
            Intent intent = new Intent(GestureSettingsActivity.this, CustomGestureSettingsActivity.class);
            if (!screenAnimations) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, screenAnimations ? R.anim.slide_out_left : 0);
        });

        View doubleTapLockContainer = findViewById(R.id.double_tap_lock_container);
        TextView doubleTapLockValueTv = doubleTapLockContainer.findViewById(R.id.value_text);
        doubleTapLockValueTv.setText(getDoubleTapLockText(doubleTapLock));
        doubleTapLockValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(doubleTapLockValueTv, new String[] { "OFF", "ON" }));

        ImageButton minusDoubleTapBtn = doubleTapLockContainer.findViewById(R.id.btn_minus);
        ImageButton plusDoubleTapBtn = doubleTapLockContainer.findViewById(R.id.btn_plus);

        minusDoubleTapBtn.setOnClickListener(v -> {
            doubleTapLock = (doubleTapLock - 1 + 2) % 2;
            doubleTapLockValueTv.setText(getDoubleTapLockText(doubleTapLock));
            prefs.edit().putInt("double_tap_lock", doubleTapLock).apply();
            if (doubleTapLock == 1 && !LockAccessibilityService.isServiceRunning()) {
                Toast.makeText(this, "Please enable accessibility to use double tap to lock", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        plusDoubleTapBtn.setOnClickListener(v -> {
            doubleTapLock = (doubleTapLock + 1) % 2;
            doubleTapLockValueTv.setText(getDoubleTapLockText(doubleTapLock));
            prefs.edit().putInt("double_tap_lock", doubleTapLock).apply();
            if (doubleTapLock == 1 && !LockAccessibilityService.isServiceRunning()) {
                Toast.makeText(this, "Please enable accessibility to use double tap to lock", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        initPagination(null);
    }

    private void selectAppForGesture(String direction) {
        currentGestureDirection = direction;
        boolean animate = prefs.getInt("screen_animations", 0) == 1;
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION,
                direction.equals("clock") || direction.equals("date") ? -3 : -1);
        if (!animate) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        startActivityForResult(intent, 1000);
        if (animate) {
            overridePendingTransition(R.anim.dialog_fade_in, 0);
        }
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

                return;
            }
            String label = data.getStringExtra(AppSelectorActivity.EXTRA_LABEL);
            String pkg = data.getStringExtra(AppSelectorActivity.EXTRA_PACKAGE);

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

    private String getDoubleTapLockText(int pos) {
        switch (pos) {
            case 0:
                return "OFF";
            case 1:
                return "ON";
            default:
                return "OFF";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }
}
