package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.ui.ClearAllGesturesDialog;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;

public class CustomGestureSettingsActivity extends BaseSettingsActivity {

    private static final int GESTURE_COUNT = 4;
    private static final int REQUEST_CODE_APP_SELECT = 1000;
    private static final int REQUEST_CODE_RECORD_GESTURE = 1001;

    private GestureLibrary gestureLibrary;
    private LinearLayout gestureListContainer;
    private final String[] gestureNames = {"custom_1", "custom_2", "custom_3", "custom_4"};
    private final ArrayList<GestureSlot> slots = new ArrayList<>();
    private int currentGestureIndex;

    private static class GestureSlot {
        int index;
        TextView recordButton;
        TextView appButton;

        GestureSlot(int index) {
            this.index = index;
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_custom_gesture_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentGestureIndex = savedInstanceState.getInt("currentGestureIndex", 0);
        }
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        ImageView clearAllButton = findViewById(R.id.clear_all_button);
        clearAllButton.setOnClickListener(v -> {
            ClearAllGesturesDialog dialog = new ClearAllGesturesDialog(this, this::clearAllGestures);
            dialog.show();
        });

        gestureListContainer = findViewById(R.id.gesture_list_container);

        File gestureFile = new File(getFilesDir(), "custom_gestures");
        gestureLibrary = GestureLibraries.fromFile(gestureFile);
        gestureLibrary.load();

        setupGestureSlots();
        loadGestureData();

        initPagination(null);
    }

    private void setupGestureSlots() {
        for (int i = 0; i < GESTURE_COUNT; i++) {
            GestureSlot slot = new GestureSlot(i);
            View row = LayoutInflater.from(this).inflate(R.layout.item_custom_gesture, gestureListContainer, false);

            ThemeUtils.applyThemeToViewGroup((ViewGroup) row, theme, this);

            TextView titleText = row.findViewById(R.id.gesture_title);
            titleText.setText("Gesture " + (i + 1) + ":");

            slot.recordButton = row.findViewById(R.id.gesture_record_button);
            slot.appButton = row.findViewById(R.id.gesture_app_button);

            int index = i;
            slot.recordButton.setOnClickListener(v -> openRecordScreen(index));
            slot.appButton.setOnClickListener(v -> selectAppForGesture(index));

            gestureListContainer.addView(row);
            slots.add(slot);
        }
    }

    private void loadGestureData() {
        for (int i = 0; i < GESTURE_COUNT; i++) {
            String name = gestureNames[i];
            String appLabel = prefs.getString("custom_gesture_" + name + "_app_label", "");

            ArrayList<Gesture> storedGestures = gestureLibrary.getGestures(name);
            if (storedGestures != null && !storedGestures.isEmpty()) {
                slots.get(i).recordButton.setText("Recorded");
            } else {
                slots.get(i).recordButton.setText("Tap to record");
            }

            if (appLabel.isEmpty()) {
                slots.get(i).appButton.setText("App: None");
            } else {
                slots.get(i).appButton.setText(appLabel);
            }
        }
    }

    private void clearAllGestures() {
        for (int i = 0; i < GESTURE_COUNT; i++) {
            gestureLibrary.removeEntry(gestureNames[i]);
        }
        for (int i = 0; i < GESTURE_COUNT; i++) {
            String name = gestureNames[i];
            prefs.edit()
                .remove("custom_gesture_" + name + "_app")
                .remove("custom_gesture_" + name + "_app_label")
                .apply();
            slots.get(i).recordButton.setText("Tap to record");
            slots.get(i).appButton.setText("App: None");
        }
        gestureLibrary.save();
        Toast.makeText(this, "All gestures cleared", Toast.LENGTH_SHORT).show();
    }

    private void openRecordScreen(int index) {
        currentGestureIndex = index;
        Intent intent = new Intent(this, RecordGestureActivity.class);
        intent.putExtra(RecordGestureActivity.EXTRA_GESTURE_INDEX, index);
        intent.putExtra(RecordGestureActivity.EXTRA_GESTURE_NAME, gestureNames[index]);
        if (!screenAnimations) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        startActivityForResult(intent, REQUEST_CODE_RECORD_GESTURE);
        if (screenAnimations) {
            overridePendingTransition(R.anim.slide_in_right, 0);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
    }

    private void selectAppForGesture(int index) {
        currentGestureIndex = index;
        boolean animate = prefs.getInt("screen_animations", 0) == 1;
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION, -3);
        if (!animate) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        startActivityForResult(intent, REQUEST_CODE_APP_SELECT);
        if (animate) {
            overridePendingTransition(R.anim.dialog_fade_in, 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentGestureIndex", currentGestureIndex);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_APP_SELECT && resultCode == RESULT_OK && data != null) {
            int index = currentGestureIndex;
            String label = data.getStringExtra(AppSelectorActivity.EXTRA_LABEL);
            String pkg = data.getStringExtra(AppSelectorActivity.EXTRA_PACKAGE);

            String name = gestureNames[index];
            prefs.edit()
                .putString("custom_gesture_" + name + "_app", pkg)
                .putString("custom_gesture_" + name + "_app_label", label)
                .apply();

            if (pkg == null || pkg.isEmpty() || pkg.equals("system_default")) {
                slots.get(index).appButton.setText("App: None");
            } else {
                slots.get(index).appButton.setText(label);
            }
        } else if (requestCode == REQUEST_CODE_RECORD_GESTURE && resultCode == RESULT_OK && data != null) {
            boolean gestureDeleted = data.getBooleanExtra(RecordGestureActivity.RESULT_GESTURE_DELETED, false);
            if (gestureDeleted) {
                loadGestureData();
            } else {
                loadGestureData();
            }
        }
    }

}