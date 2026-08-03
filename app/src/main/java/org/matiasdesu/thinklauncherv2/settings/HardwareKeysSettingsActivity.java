package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class HardwareKeysSettingsActivity extends BaseSettingsActivity {

    private String currentKey;

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
    protected int getLayoutResId() {
        return R.layout.activity_hardware_keys_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentKey = savedInstanceState.getString("currentKey");
        }
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

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

        initPagination(null);
    }

    private void selectAppForKey(String key) {
        currentKey = key;
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean animate = prefs.getInt("screen_animations", 0) == 1;
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION, -1);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }
}
