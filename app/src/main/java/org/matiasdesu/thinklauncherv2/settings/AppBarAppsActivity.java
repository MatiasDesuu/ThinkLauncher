package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;

public class AppBarAppsActivity extends BaseSettingsActivity {

    public static final String EXTRA_PREFIX = "prefix";

    private static final int REQUEST_CODE_APP_SELECT = 1000;

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
    protected int getLayoutResId() {
        return R.layout.activity_app_bar_apps;
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
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        appListContainer = findViewById(R.id.app_list_container);

        setupSlots();
        loadSlotData();

        initPagination(null);
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
}