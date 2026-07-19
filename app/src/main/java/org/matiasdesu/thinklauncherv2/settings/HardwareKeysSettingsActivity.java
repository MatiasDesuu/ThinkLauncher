package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
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
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class HardwareKeysSettingsActivity extends AppCompatActivity {

    private String currentKey;
    private SettingsPaginationHelper paginationHelper;
    private int theme;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(HardwareKeysSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentKey = savedInstanceState.getString("currentKey");
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
        setContentView(R.layout.activity_hardware_keys_settings);

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

        LinearLayout rootLayout = findViewById(R.id.root_layout);
        rootLayout.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(rootLayout, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        if (!prefs.contains("hardware_key_volume_up")) {
            prefs.edit()
                    .putString("hardware_key_volume_up", "")
                    .putString("hardware_key_volume_up_label", "None")
                    .putString("hardware_key_volume_down", "")
                    .putString("hardware_key_volume_down_label", "None")
                    .apply();
        }

        TextView volumeUpTv = findViewById(R.id.volume_up_button);
        TextView volumeDownTv = findViewById(R.id.volume_down_button);

        volumeUpTv.setText(prefs.getString("hardware_key_volume_up_label", "None"));
        volumeDownTv.setText(prefs.getString("hardware_key_volume_down_label", "None"));

        volumeUpTv.setOnClickListener(v -> selectAppForKey("volume_up"));
        volumeDownTv.setOnClickListener(v -> selectAppForKey("volume_down"));

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(null);
    }

    private void selectAppForKey(String key) {
        currentKey = key;
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION, -1);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, 1000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentKey", currentKey);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            if (currentKey == null) return;
            String label = data.getStringExtra(AppSelectorActivity.EXTRA_LABEL);
            String pkg = data.getStringExtra(AppSelectorActivity.EXTRA_PACKAGE);

            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            String appKey, labelKey;
            int resId;
            switch (currentKey) {
                case "volume_up":
                    appKey = "hardware_key_volume_up";
                    labelKey = "hardware_key_volume_up_label";
                    resId = R.id.volume_up_button;
                    break;
                case "volume_down":
                    appKey = "hardware_key_volume_down";
                    labelKey = "hardware_key_volume_down_label";
                    resId = R.id.volume_down_button;
                    break;
                default:
                    return;
            }
            prefs.edit().putString(appKey, pkg).putString(labelKey, label).apply();
            TextView tv = findViewById(resId);
            tv.setText(label);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);
        if (paginationHelper != null) {
            paginationHelper.updateVisibleItemsList();
        }
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
        overridePendingTransition(0, 0);
    }
}
