package org.matiasdesu.thinklauncherv2.settings;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;

public class RecordGestureActivity extends BaseSettingsActivity {

    public static final String EXTRA_GESTURE_INDEX = "gesture_index";
    public static final String EXTRA_GESTURE_NAME = "gesture_name";
    public static final String RESULT_GESTURE_DELETED = "gesture_deleted";

    private GestureLibrary gestureLibrary;
    private int gestureIndex;
    private String gestureName;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_record_gesture;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);

        gestureIndex = getIntent().getIntExtra(EXTRA_GESTURE_INDEX, 0);
        gestureName = getIntent().getStringExtra(EXTRA_GESTURE_NAME);

        File gestureFile = new File(getFilesDir(), "custom_gestures");
        gestureLibrary = GestureLibraries.fromFile(gestureFile);
        gestureLibrary.load();

        ImageView deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            gestureLibrary.removeEntry(gestureName);
            prefs.edit()
                .remove("custom_gesture_" + gestureName + "_app")
                .remove("custom_gesture_" + gestureName + "_app_label")
                .apply();
            gestureLibrary.save();
            Toast.makeText(this, "Gesture " + (gestureIndex + 1) + " deleted", Toast.LENGTH_SHORT).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_GESTURE_DELETED, true);
            setResult(RESULT_OK, resultIntent);
            finish();
            overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
        });

        TextView titleText = findViewById(R.id.record_title);
        titleText.setText("Gesture " + (gestureIndex + 1));

        GestureOverlayView recordingOverlay = findViewById(R.id.recording_gesture_overlay);
        int accentColor = ThemeUtils.getTextColor(theme, this);
        recordingOverlay.setGestureColor(accentColor);
        recordingOverlay.setUncertainGestureColor(accentColor);
        recordingOverlay.setFadeEnabled(false);

        TextView recordingHint = findViewById(R.id.recording_hint);

        ArrayList<Gesture> storedGestures = gestureLibrary.getGestures(gestureName);
        if (storedGestures != null && !storedGestures.isEmpty()) {
            recordingHint.setText("Draw to replace current gesture");
        } else {
            recordingHint.setText("Draw a gesture");
        }

        recordingOverlay.addOnGesturePerformedListener((overlay, gesture) -> {
            gestureLibrary.removeEntry(gestureName);
            gestureLibrary.addGesture(gestureName, gesture);
            gestureLibrary.save();
            Toast.makeText(this, "Gesture recorded", Toast.LENGTH_SHORT).show();
            recordingHint.setText("Draw to replace current gesture");
        });
    }
}