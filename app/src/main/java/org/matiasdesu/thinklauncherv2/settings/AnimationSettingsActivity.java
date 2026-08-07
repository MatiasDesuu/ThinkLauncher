package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class AnimationSettingsActivity extends BaseSettingsActivity {

    private int appIndexAnimation;
    private int appIndexSidebar;
    private int dialogAnimations;
    private int appLaunchAnimation;

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
    protected int getLayoutResId() {
        return R.layout.activity_animation_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        appIndexAnimation = prefs.getInt("app_index_animation", 0);
        dialogAnimations = prefs.getInt("dialog_animations", 0);
        appLaunchAnimation = prefs.getInt("app_launch_animation", 0);

        View appIndexAnimationContainer = findViewById(R.id.app_index_animation_container);
        TextView appIndexAnimationValueTv = appIndexAnimationContainer.findViewById(R.id.value_text);
        appIndexAnimationValueTv.setText(getOnOffText(appIndexAnimation));

        ImageButton minusBtn = appIndexAnimationContainer.findViewById(R.id.btn_minus);
        ImageButton plusBtn = appIndexAnimationContainer.findViewById(R.id.btn_plus);

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
        screenAnimationsValueTv.setText(getOnOffText(screenAnimations ? 1 : 0));

        ImageButton minusScreenBtn = screenAnimationsContainer.findViewById(R.id.btn_minus);
        ImageButton plusScreenBtn = screenAnimationsContainer.findViewById(R.id.btn_plus);

        minusScreenBtn.setOnClickListener(v -> {
            screenAnimations = !screenAnimations;
            screenAnimationsValueTv.setText(getOnOffText(screenAnimations ? 1 : 0));
            prefs.edit().putInt("screen_animations", screenAnimations ? 1 : 0).apply();
        });

        plusScreenBtn.setOnClickListener(v -> {
            screenAnimations = !screenAnimations;
            screenAnimationsValueTv.setText(getOnOffText(screenAnimations ? 1 : 0));
            prefs.edit().putInt("screen_animations", screenAnimations ? 1 : 0).apply();
        });

        View dialogAnimationsContainer = findViewById(R.id.dialog_animations_container);
        TextView dialogAnimationsValueTv = dialogAnimationsContainer.findViewById(R.id.value_text);
        dialogAnimationsValueTv.setText(getOnOffText(dialogAnimations));

        ImageButton minusDialogBtn = dialogAnimationsContainer.findViewById(R.id.btn_minus);
        ImageButton plusDialogBtn = dialogAnimationsContainer.findViewById(R.id.btn_plus);

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

        View appLaunchAnimationContainer = findViewById(R.id.app_launch_animation_container);
        TextView appLaunchAnimationValueTv = appLaunchAnimationContainer.findViewById(R.id.value_text);
        appLaunchAnimationValueTv.setText(getOnOffText(appLaunchAnimation));

        ImageButton minusAppLaunchBtn = appLaunchAnimationContainer.findViewById(R.id.btn_minus);
        ImageButton plusAppLaunchBtn = appLaunchAnimationContainer.findViewById(R.id.btn_plus);

        minusAppLaunchBtn.setOnClickListener(v -> {
            appLaunchAnimation = (appLaunchAnimation - 1 + 2) % 2;
            appLaunchAnimationValueTv.setText(getOnOffText(appLaunchAnimation));
            prefs.edit().putInt("app_launch_animation", appLaunchAnimation).apply();
        });

        plusAppLaunchBtn.setOnClickListener(v -> {
            appLaunchAnimation = (appLaunchAnimation + 1) % 2;
            appLaunchAnimationValueTv.setText(getOnOffText(appLaunchAnimation));
            prefs.edit().putInt("app_launch_animation", appLaunchAnimation).apply();
        });

        initPagination(this::refreshVisibility);
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
        appIndexSidebar = prefs.getInt("app_index_sidebar", 0);
        refreshVisibility();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }
}
