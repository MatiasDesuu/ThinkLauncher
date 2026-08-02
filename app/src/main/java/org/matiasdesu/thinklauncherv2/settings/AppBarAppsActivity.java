package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;

public class AppBarAppsActivity extends AppCompatActivity {

    public static final String EXTRA_PREFIX = "prefix";

    private static final int REQUEST_CODE_APP_SELECT = 1000;

    private int theme;
    private SharedPreferences prefs;
    private boolean screenAnimations;
    private LinearLayout appListContainer;
    private final ArrayList<Slot> slots = new ArrayList<>();
    private int currentSlotIndex;
    private String prefix = "app_bar";

    private String p(String key) {
        return prefix + "_" + key;
    }

    private static class Slot {
        int index;
        TextView appButton;

        Slot(int index) {
            this.index = index;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentSlotIndex = savedInstanceState.getInt("currentSlotIndex", 0);
        }
        prefix = getIntent().getStringExtra(EXTRA_PREFIX);
        if (prefix == null || prefix.isEmpty()) {
            prefix = "app_bar";
        }
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
        setContentView(R.layout.activity_app_bar_apps);

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
            overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
        });

        appListContainer = findViewById(R.id.app_list_container);

        setupSlots();
        loadSlotData();
    }

    private void setupSlots() {
        int count = prefs.getInt(p("num_apps"), 4);
        if (count < 1) count = 1;
        appListContainer.removeAllViews();
        slots.clear();
        for (int i = 0; i < count; i++) {
            Slot slot = new Slot(i);
            View row = LayoutInflater.from(this).inflate(R.layout.item_app_bar_app, appListContainer, false);
            ThemeUtils.applyThemeToViewGroup((ViewGroup) row, theme, this);

            TextView titleText = row.findViewById(R.id.app_title);
            titleText.setText("App " + (i + 1) + ":");

            slot.appButton = row.findViewById(R.id.app_button);

            int index = i;
            slot.appButton.setOnClickListener(v -> selectAppForSlot(index));

            appListContainer.addView(row);
            slots.add(slot);
        }
    }

    private void loadSlotData() {
        for (Slot slot : slots) {
            String label = prefs.getString(p("app_label_") + slot.index, "");
            if (label == null || label.isEmpty()) {
                slot.appButton.setText("None");
            } else {
                slot.appButton.setText(label);
            }
        }
    }

    private void selectAppForSlot(int index) {
        currentSlotIndex = index;
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION, -5);
        if (!screenAnimations) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        startActivityForResult(intent, REQUEST_CODE_APP_SELECT);
        if (screenAnimations) {
            overridePendingTransition(R.anim.dialog_fade_in, 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentSlotIndex", currentSlotIndex);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_APP_SELECT && resultCode == RESULT_OK && data != null) {
            int index = currentSlotIndex;
            String label = data.getStringExtra(AppSelectorActivity.EXTRA_LABEL);
            String pkg = data.getStringExtra(AppSelectorActivity.EXTRA_PACKAGE);

            prefs.edit()
                    .putString(p("app_package_") + index, pkg)
                    .putString(p("app_label_") + index, label)
                    .apply();

            if (pkg == null || pkg.isEmpty()) {
                slots.get(index).appButton.setText("None");
            } else {
                slots.get(index).appButton.setText(label);
            }
        }
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