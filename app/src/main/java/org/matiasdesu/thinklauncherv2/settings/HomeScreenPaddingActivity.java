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
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import android.os.Build;

public class HomeScreenPaddingActivity extends AppCompatActivity {

    private int paddingTop;
    private int paddingBottom;
    private int paddingLeft;
    private int paddingRight;
    private LinearLayout rootLayout;
    private SettingsPaginationHelper paginationHelper;
    private int theme;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    // Bring MainActivity to front
                    Intent mainIntent = new Intent(HomeScreenPaddingActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_home_screen_padding);

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

        paddingTop = prefs.getInt("home_padding_top", 0);
        paddingBottom = prefs.getInt("home_padding_bottom", 0);
        paddingLeft = prefs.getInt("home_padding_left", 0);
        paddingRight = prefs.getInt("home_padding_right", 0);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View paddingTopContainer = findViewById(R.id.top_padding_container);
        TextView paddingTopValueTv = paddingTopContainer.findViewById(R.id.value_text);
        paddingTopValueTv.setText(String.valueOf(paddingTop));

        View paddingBottomContainer = findViewById(R.id.bottom_padding_container);
        TextView paddingBottomValueTv = paddingBottomContainer.findViewById(R.id.value_text);
        paddingBottomValueTv.setText(String.valueOf(paddingBottom));

        View paddingLeftContainer = findViewById(R.id.left_padding_container);
        TextView paddingLeftValueTv = paddingLeftContainer.findViewById(R.id.value_text);
        paddingLeftValueTv.setText(String.valueOf(paddingLeft));

        View paddingRightContainer = findViewById(R.id.right_padding_container);
        TextView paddingRightValueTv = paddingRightContainer.findViewById(R.id.value_text);
        paddingRightValueTv.setText(String.valueOf(paddingRight));

        TextView minusPaddingTopBtn = paddingTopContainer.findViewById(R.id.btn_minus);
        TextView plusPaddingTopBtn = paddingTopContainer.findViewById(R.id.btn_plus);
        TextView minusPaddingBottomBtn = paddingBottomContainer.findViewById(R.id.btn_minus);
        TextView plusPaddingBottomBtn = paddingBottomContainer.findViewById(R.id.btn_plus);
        TextView minusPaddingLeftBtn = paddingLeftContainer.findViewById(R.id.btn_minus);
        TextView plusPaddingLeftBtn = paddingLeftContainer.findViewById(R.id.btn_plus);
        TextView minusPaddingRightBtn = paddingRightContainer.findViewById(R.id.btn_minus);
        TextView plusPaddingRightBtn = paddingRightContainer.findViewById(R.id.btn_plus);

        minusPaddingTopBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingTop > 0) {
                paddingTop--;
                paddingTopValueTv.setText(String.valueOf(paddingTop));
                prefs.edit().putInt("home_padding_top", paddingTop).apply();
            }
        }));

        plusPaddingTopBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingTop < 200) {
                paddingTop++;
                paddingTopValueTv.setText(String.valueOf(paddingTop));
                prefs.edit().putInt("home_padding_top", paddingTop).apply();
            }
        }));

        minusPaddingBottomBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingBottom > 0) {
                paddingBottom--;
                paddingBottomValueTv.setText(String.valueOf(paddingBottom));
                prefs.edit().putInt("home_padding_bottom", paddingBottom).apply();
            }
        }));

        plusPaddingBottomBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingBottom < 200) {
                paddingBottom++;
                paddingBottomValueTv.setText(String.valueOf(paddingBottom));
                prefs.edit().putInt("home_padding_bottom", paddingBottom).apply();
            }
        }));

        minusPaddingLeftBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingLeft > 0) {
                paddingLeft--;
                paddingLeftValueTv.setText(String.valueOf(paddingLeft));
                prefs.edit().putInt("home_padding_left", paddingLeft).apply();
            }
        }));

        plusPaddingLeftBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingLeft < 200) {
                paddingLeft++;
                paddingLeftValueTv.setText(String.valueOf(paddingLeft));
                prefs.edit().putInt("home_padding_left", paddingLeft).apply();
            }
        }));

        minusPaddingRightBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingRight > 0) {
                paddingRight--;
                paddingRightValueTv.setText(String.valueOf(paddingRight));
                prefs.edit().putInt("home_padding_right", paddingRight).apply();
            }
        }));

        plusPaddingRightBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (paddingRight < 200) {
                paddingRight++;
                paddingRightValueTv.setText(String.valueOf(paddingRight));
                prefs.edit().putInt("home_padding_right", paddingRight).apply();
            }
        }));

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(null);
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
