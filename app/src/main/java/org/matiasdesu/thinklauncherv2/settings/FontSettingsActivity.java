package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.io.InputStream;

public class FontSettingsActivity extends AppCompatActivity {

    private LinearLayout rootLayout;
    private int theme;
    private boolean screenAnimations;

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
        setContentView(R.layout.activity_font_settings);

        screenAnimations = prefs.getInt("screen_animations", 0) == 1;

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

        setupPickerLauncher();

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
        });

        LinearLayout chooseFontButton = findViewById(R.id.choose_font_button);
        chooseFontButton.setOnClickListener(v -> openFontPicker());

        TextView selectButtonText = findViewById(R.id.select_button_text);
        ThemeUtils.applyButtonTheme(selectButtonText, theme, this);

        LinearLayout resetFontButton = findViewById(R.id.reset_font_button);
        resetFontButton.setOnClickListener(v -> resetFont());

        TextView resetButtonText = findViewById(R.id.reset_button_text);
        ThemeUtils.applyButtonTheme(resetButtonText, theme, this);

        updateFontStatus();
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
        finish();
        overridePendingTransition(R.anim.slide_in_left, screenAnimations ? R.anim.slide_out_right : 0);
    }
}