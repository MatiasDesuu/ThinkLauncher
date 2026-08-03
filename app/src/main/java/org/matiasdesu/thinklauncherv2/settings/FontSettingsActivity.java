package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.io.InputStream;

public class FontSettingsActivity extends BaseSettingsActivity {

    private ActivityResultLauncher<Intent> fontPickerLauncher;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(FontSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_font_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        setupPickerLauncher();

        LinearLayout chooseFontButton = findViewById(R.id.choose_font_button);
        chooseFontButton.setOnClickListener(v -> openFontPicker());

        TextView selectButtonText = findViewById(R.id.select_button_text);
        ThemeUtils.applyButtonTheme(selectButtonText, theme, this);

        LinearLayout resetFontButton = findViewById(R.id.reset_font_button);
        resetFontButton.setOnClickListener(v -> resetFont());

        TextView resetButtonText = findViewById(R.id.reset_button_text);
        ThemeUtils.applyButtonTheme(resetButtonText, theme, this);

        updateFontStatus();

        initPagination(null);
    }

    private void setupPickerLauncher() {
        fontPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fontUri = result.getData().getData();
                        if (fontUri != null) {
                            applySelectedFont(fontUri);
                        }
                    }
                });
    }

    private void openFontPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            fontPickerLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "No file picker available. Please install a file manager.", Toast.LENGTH_LONG).show();
        }
    }

    private void applySelectedFont(Uri fontUri) {
        try {
            InputStream in = getContentResolver().openInputStream(fontUri);
            if (in != null && FontHelper.saveFont(this, in)) {
                Toast.makeText(this, "Font applied", Toast.LENGTH_SHORT).show();
                updateFontStatus();
            } else {
                Toast.makeText(this, "Failed to apply font", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to apply font", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetFont() {
        FontHelper.removeFont(this);
        updateFontStatus();
        Toast.makeText(this, "Font reset", Toast.LENGTH_SHORT).show();
    }

    private void updateFontStatus() {
        FontHelper.applyToViewTree(this, rootLayout);
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