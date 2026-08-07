package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import android.widget.ImageButton;

public class TextSettingsActivity extends BaseSettingsActivity {

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Black", "White", "Dynamic Dark", "Dynamic White" };
    private static final String[] TEXT_COLOR_NAMES = { "Follow Theme", "Dark", "White", "Dynamic Dark", "Dynamic White" };

    private boolean boldText;
    private int appTextColor;
    private int textEffect;
    private int effectColor;

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
    protected int getLayoutResId() {
        return R.layout.activity_text_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        boldText = prefs.getBoolean("bold_text", true);
        appTextColor = prefs.getInt("app_text_color", 0);
        textEffect = prefs.getInt("text_effect", 0);
        effectColor = prefs.getInt("effect_color", 0);

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

        ImageButton minusBoldBtn = boldTextContainer.findViewById(R.id.btn_minus);
        ImageButton plusBoldBtn = boldTextContainer.findViewById(R.id.btn_plus);
        ImageButton minusTextColorBtn = textColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusTextColorBtn = textColorContainer.findViewById(R.id.btn_plus);
        ImageButton minusTextEffectBtn = textEffectContainer.findViewById(R.id.btn_minus);
        ImageButton plusTextEffectBtn = textEffectContainer.findViewById(R.id.btn_plus);
        ImageButton minusEffectColorBtn = effectColorContainer.findViewById(R.id.btn_minus);
        ImageButton plusEffectColorBtn = effectColorContainer.findViewById(R.id.btn_plus);

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
            refreshPagination();
        });

        plusTextEffectBtn.setOnClickListener(v -> {
            textEffect = (textEffect + 1) % EFFECT_NAMES.length;
            textEffectValueTv.setText(EFFECT_NAMES[textEffect]);
            prefs.edit().putInt("text_effect", textEffect).apply();
            refreshPagination();
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

        initPagination(this::refreshVisibility);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
    }
}
