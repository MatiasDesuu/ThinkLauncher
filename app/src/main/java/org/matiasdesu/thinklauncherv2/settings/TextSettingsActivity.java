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
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.SettingsPaginationHelper;

import android.os.Build;

public class TextSettingsActivity extends AppCompatActivity {

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Black", "White", "Dynamic Dark", "Dynamic White" };
    private static final String[] TEXT_COLOR_NAMES = { "Follow Theme", "Dark", "White", "Dynamic Dark", "Dynamic White" };

    private int textSize;
    private boolean boldText;
    private int appTextColor;
    private int textEffect;
    private int effectColor;
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
                    Intent mainIntent = new Intent(TextSettingsActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_text_settings);

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

        textSize = prefs.getInt("text_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        appTextColor = prefs.getInt("app_text_color", 0);
        textEffect = prefs.getInt("text_effect", 0);
        effectColor = prefs.getInt("effect_color", 0);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        View textSizeContainer = findViewById(R.id.text_size_container);
        TextView textSizeValueTv = textSizeContainer.findViewById(R.id.value_text);
        textSizeValueTv.setText(String.valueOf(textSize));

        View boldTextContainer = findViewById(R.id.bold_text_container);
        TextView boldTextValueTv = boldTextContainer.findViewById(R.id.value_text);
        boldTextValueTv.setText(boldText ? "ON" : "OFF");
        boldTextValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(boldTextValueTv, new String[] { "ON", "OFF" }));

        View textColorContainer = findViewById(R.id.text_color_container);
        TextView textColorValueTv = textColorContainer.findViewById(R.id.value_text);
        textColorValueTv.setText(TEXT_COLOR_NAMES[appTextColor]);
        textColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(textColorValueTv, TEXT_COLOR_NAMES));

        View textEffectContainer = findViewById(R.id.text_effect_container);
        TextView textEffectValueTv = textEffectContainer.findViewById(R.id.value_text);
        textEffectValueTv.setText(EFFECT_NAMES[textEffect]);
        textEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(textEffectValueTv, EFFECT_NAMES));

        View effectColorContainer = findViewById(R.id.effect_color_container);
        TextView effectColorValueTv = effectColorContainer.findViewById(R.id.value_text);
        effectColorValueTv.setText(EFFECT_COLOR_NAMES[effectColor]);
        effectColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(effectColorValueTv, EFFECT_COLOR_NAMES));

        TextView minusTextSizeBtn = textSizeContainer.findViewById(R.id.btn_minus);
        TextView plusTextSizeBtn = textSizeContainer.findViewById(R.id.btn_plus);
        TextView minusBoldBtn = boldTextContainer.findViewById(R.id.btn_minus);
        TextView plusBoldBtn = boldTextContainer.findViewById(R.id.btn_plus);
        TextView minusTextColorBtn = textColorContainer.findViewById(R.id.btn_minus);
        TextView plusTextColorBtn = textColorContainer.findViewById(R.id.btn_plus);
        TextView minusTextEffectBtn = textEffectContainer.findViewById(R.id.btn_minus);
        TextView plusTextEffectBtn = textEffectContainer.findViewById(R.id.btn_plus);
        TextView minusEffectColorBtn = effectColorContainer.findViewById(R.id.btn_minus);
        TextView plusEffectColorBtn = effectColorContainer.findViewById(R.id.btn_plus);

        minusTextSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (textSize > 10) {
                textSize--;
                textSizeValueTv.setText(String.valueOf(textSize));
                prefs.edit().putInt("text_size", textSize).apply();
            }
        }));

        plusTextSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (textSize < 50) {
                textSize++;
                textSizeValueTv.setText(String.valueOf(textSize));
                prefs.edit().putInt("text_size", textSize).apply();
            }
        }));

        minusBoldBtn.setOnClickListener(v -> toggleBold(prefs, boldTextValueTv));
        plusBoldBtn.setOnClickListener(v -> toggleBold(prefs, boldTextValueTv));

        minusTextColorBtn.setOnClickListener(v -> {
            appTextColor = (appTextColor - 1 + TEXT_COLOR_NAMES.length) % TEXT_COLOR_NAMES.length;
            textColorValueTv.setText(TEXT_COLOR_NAMES[appTextColor]);
            prefs.edit().putInt("app_text_color", appTextColor).apply();
        });

        plusTextColorBtn.setOnClickListener(v -> {
            appTextColor = (appTextColor + 1) % TEXT_COLOR_NAMES.length;
            textColorValueTv.setText(TEXT_COLOR_NAMES[appTextColor]);
            prefs.edit().putInt("app_text_color", appTextColor).apply();
        });

        minusTextEffectBtn.setOnClickListener(v -> {
            textEffect = (textEffect - 1 + EFFECT_NAMES.length) % EFFECT_NAMES.length;
            textEffectValueTv.setText(EFFECT_NAMES[textEffect]);
            prefs.edit().putInt("text_effect", textEffect).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        plusTextEffectBtn.setOnClickListener(v -> {
            textEffect = (textEffect + 1) % EFFECT_NAMES.length;
            textEffectValueTv.setText(EFFECT_NAMES[textEffect]);
            prefs.edit().putInt("text_effect", textEffect).apply();
            if (paginationHelper != null)
                paginationHelper.updateVisibleItemsList();
        });

        minusEffectColorBtn.setOnClickListener(v -> {
            effectColor = (effectColor - 1 + EFFECT_COLOR_NAMES.length) % EFFECT_COLOR_NAMES.length;
            effectColorValueTv.setText(EFFECT_COLOR_NAMES[effectColor]);
            prefs.edit().putInt("effect_color", effectColor).apply();
        });

        plusEffectColorBtn.setOnClickListener(v -> {
            effectColor = (effectColor + 1) % EFFECT_COLOR_NAMES.length;
            effectColorValueTv.setText(EFFECT_COLOR_NAMES[effectColor]);
            prefs.edit().putInt("effect_color", effectColor).apply();
        });

        LinearLayout settingsItemsContainer = findViewById(R.id.settings_items_container);
        ScrollView scrollView = findViewById(R.id.settings_scroll_view);
        FrameLayout container = findViewById(R.id.settings_container);

        paginationHelper = new SettingsPaginationHelper(this, theme, settingsItemsContainer, scrollView, container);
        paginationHelper.initialize(this::refreshVisibility);
    }

    private void toggleBold(SharedPreferences prefs, TextView valueTv) {
        boldText = !boldText;
        valueTv.setText(boldText ? "ON" : "OFF");
        prefs.edit().putBoolean("bold_text", boldText).apply();
    }

    private void refreshVisibility() {
        View effectColorLayout = findViewById(R.id.effect_color_layout);
        effectColorLayout.setVisibility(textEffect > 0 ? View.VISIBLE : View.GONE);
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
