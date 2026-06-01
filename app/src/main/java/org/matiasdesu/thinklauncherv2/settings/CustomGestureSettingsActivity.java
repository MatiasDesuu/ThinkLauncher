package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.ui.ClearAllGesturesDialog;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;

public class CustomGestureSettingsActivity extends AppCompatActivity {

    private static final int GESTURE_COUNT = 4;
    private static final int REQUEST_CODE_APP_SELECT = 1000;

    private int theme;
    private SharedPreferences prefs;
    private GestureLibrary gestureLibrary;
    private LinearLayout gestureListContainer;
    private final String[] gestureNames = {"custom_1", "custom_2", "custom_3", "custom_4"};
    private int currentRecordingIndex = -1;
    private FrameLayout recordingOverlay;
    private GestureOverlayView recordingGestureOverlay;
    private TextView recordingHint;
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
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        int bgColor = ThemeUtils.getBgColor(theme, this);
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_gesture_settings);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
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

        findViewById(R.id.clear_all_button).setOnClickListener(v ->
                new ClearAllGesturesDialog(this, this::clearAllGestures).show());

        gestureListContainer = findViewById(R.id.gesture_list_container);

        File gestureFile = new File(getFilesDir(), "custom_gestures");
        gestureLibrary = GestureLibraries.fromFile(gestureFile);
        gestureLibrary.load();

        recordingOverlay = findViewById(R.id.recording_overlay);
        recordingOverlay.setBackgroundColor(bgColor);
        recordingGestureOverlay = findViewById(R.id.recording_gesture_overlay);
        int accentColor = ThemeUtils.getTextColor(theme, this);
        recordingGestureOverlay.setGestureColor(accentColor);
        recordingGestureOverlay.setUncertainGestureColor(accentColor);
        recordingGestureOverlay.setFadeEnabled(false);
        recordingHint = findViewById(R.id.recording_hint);

        recordingGestureOverlay.addOnGesturePerformedListener((overlay, gesture) -> {
            if (currentRecordingIndex >= 0 && currentRecordingIndex < GESTURE_COUNT) {
                String name = gestureNames[currentRecordingIndex];
                gestureLibrary.removeEntry(name);
                gestureLibrary.addGesture(name, gesture);
                gestureLibrary.save();

                slots.get(currentRecordingIndex).recordButton.setText("Recorded");

                recordingOverlay.setVisibility(View.GONE);
                currentRecordingIndex = -1;
                Toast.makeText(this, "Gesture recorded", Toast.LENGTH_SHORT).show();
            }
        });

        setupGestureSlots();
        loadGestureData();
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
            slot.recordButton.setOnClickListener(v -> startRecording(index));
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
            String name = gestureNames[i];
            gestureLibrary.removeEntry(name);
            prefs.edit()
                .remove("custom_gesture_" + name + "_app")
                .remove("custom_gesture_" + name + "_app_label")
                .apply();
            slots.get(i).recordButton.setText("Tap to record");
            slots.get(i).appButton.setText("App: None");
        }
        gestureLibrary.save();
        Toast.makeText(this, "All custom gestures cleared", Toast.LENGTH_SHORT).show();
    }

    private void startRecording(int index) {
        currentRecordingIndex = index;
        recordingGestureOverlay.clear(false);
        recordingHint.setText("Draw gesture for Gesture " + (index + 1));
        recordingOverlay.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (recordingOverlay.getVisibility() == View.VISIBLE) {
            recordingOverlay.setVisibility(View.GONE);
            currentRecordingIndex = -1;
            return;
        }
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    private void selectAppForGesture(int index) {
        currentGestureIndex = index;
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION, -3);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, REQUEST_CODE_APP_SELECT);
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
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }
    }
}
