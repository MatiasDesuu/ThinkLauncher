package org.matiasdesu.thinklauncherv2.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.TextWidthHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class SearchButtonSettingsActivity extends BaseSettingsActivity {

    private static final String[] EFFECT_NAMES = { "Nothing", "Shadow", "Outline" };
    private static final String[] EFFECT_COLOR_NAMES = { "Dark", "White", "Dynamic Dark", "Dynamic Light" };

    private int showSearchButton;
    private int searchButtonSize;
    private int searchButtonColor;
    private int searchButtonEffect;
    private int searchButtonEffectColor;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    // Bring MainActivity to front
                    Intent mainIntent = new Intent(SearchButtonSettingsActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_search_button_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bgColor = ThemeUtils.getBgColor(theme, this);
        LinearLayout root = findViewById(R.id.root_layout);
        root.setBackgroundColor(bgColor);
        ThemeUtils.applyThemeToViewGroup(root, theme, this);

        showSearchButton = prefs.getInt("show_search_button", 0);
        searchButtonSize = prefs.getInt("search_button_size", 42);
        searchButtonColor = prefs.getInt("search_button_color", 0);
        searchButtonEffect = prefs.getInt("search_button_effect", 0);
        searchButtonEffectColor = prefs.getInt("search_button_effect_color", 0);
        screenAnimations = prefs.getInt("screen_animations", 0) == 1;

        View showSearchButtonContainer = findViewById(R.id.show_search_button_container);
        TextView showSearchButtonValueTv = showSearchButtonContainer.findViewById(R.id.value_text);
        showSearchButtonValueTv.setText(getOnOffText(showSearchButton));
        showSearchButtonValueTv.setMinWidth(
                TextWidthHelper.getMaxTextWidthPx(showSearchButtonValueTv, new String[] { "OFF", "ON" }));

        View searchButtonSizeContainer = findViewById(R.id.search_button_size_container);
        TextView searchButtonSizeValueTv = searchButtonSizeContainer.findViewById(R.id.value_text);
        searchButtonSizeValueTv.setText(String.valueOf(searchButtonSize));

        View searchButtonColorContainer = findViewById(R.id.search_button_color_container);
        TextView searchButtonColorValueTv = searchButtonColorContainer.findViewById(R.id.value_text);
        searchButtonColorValueTv.setText(getSearchButtonColorText(searchButtonColor));
        searchButtonColorValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(searchButtonColorValueTv,
                new String[] { "FOLLOW THEME", "DARK", "WHITE", "DYNAMIC DARK", "DYNAMIC LIGHT" }));

        View searchButtonEffectContainer = findViewById(R.id.search_button_effect_container);
        TextView searchButtonEffectValueTv = searchButtonEffectContainer.findViewById(R.id.value_text);
        searchButtonEffectValueTv.setText(EFFECT_NAMES[searchButtonEffect]);
        searchButtonEffectValueTv.setMinWidth(TextWidthHelper.getMaxTextWidthPx(searchButtonEffectValueTv, EFFECT_NAMES));

        View searchButtonEffectColorContainer = findViewById(R.id.search_button_effect_color_container);
        TextView searchButtonEffectColorValueTv = searchButtonEffectColorContainer.findViewById(R.id.value_text);
        searchButtonEffectColorValueTv.setText(EFFECT_COLOR_NAMES[searchButtonEffectColor]);
        searchButtonEffectColorValueTv
                .setMinWidth(TextWidthHelper.getMaxTextWidthPx(searchButtonEffectColorValueTv, EFFECT_COLOR_NAMES));

        TextView minusShowSearchBtn = showSearchButtonContainer.findViewById(R.id.btn_minus);
        TextView plusShowSearchBtn = showSearchButtonContainer.findViewById(R.id.btn_plus);
        TextView minusSearchButtonSizeBtn = searchButtonSizeContainer.findViewById(R.id.btn_minus);
        TextView plusSearchButtonSizeBtn = searchButtonSizeContainer.findViewById(R.id.btn_plus);
        TextView minusSearchButtonColorBtn = searchButtonColorContainer.findViewById(R.id.btn_minus);
        TextView plusSearchButtonColorBtn = searchButtonColorContainer.findViewById(R.id.btn_plus);
        TextView minusSearchButtonEffectBtn = searchButtonEffectContainer.findViewById(R.id.btn_minus);
        TextView plusSearchButtonEffectBtn = searchButtonEffectContainer.findViewById(R.id.btn_plus);
        TextView minusSearchButtonEffectColorBtn = searchButtonEffectColorContainer.findViewById(R.id.btn_minus);
        TextView plusSearchButtonEffectColorBtn = searchButtonEffectColorContainer.findViewById(R.id.btn_plus);

        minusShowSearchBtn.setOnClickListener(v -> {
            showSearchButton = (showSearchButton - 1 + 2) % 2;
            showSearchButtonValueTv.setText(getOnOffText(showSearchButton));
            prefs.edit().putInt("show_search_button", showSearchButton).apply();
            refreshVisibility();
            refreshPagination();
        });

        plusShowSearchBtn.setOnClickListener(v -> {
            showSearchButton = (showSearchButton + 1) % 2;
            showSearchButtonValueTv.setText(getOnOffText(showSearchButton));
            prefs.edit().putInt("show_search_button", showSearchButton).apply();
            refreshVisibility();
            refreshPagination();
        });

        minusSearchButtonSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (searchButtonSize > 10) {
                searchButtonSize--;
                searchButtonSizeValueTv.setText(String.valueOf(searchButtonSize));
                prefs.edit().putInt("search_button_size", searchButtonSize).apply();
            }
        }));

        plusSearchButtonSizeBtn.setOnTouchListener(new org.matiasdesu.thinklauncherv2.utils.RepeatListener(v -> {
            if (searchButtonSize < 100) {
                searchButtonSize++;
                searchButtonSizeValueTv.setText(String.valueOf(searchButtonSize));
                prefs.edit().putInt("search_button_size", searchButtonSize).apply();
            }
        }));

        minusSearchButtonColorBtn.setOnClickListener(v -> {
            searchButtonColor = (searchButtonColor - 1 + 5) % 5;
            searchButtonColorValueTv.setText(getSearchButtonColorText(searchButtonColor));
            prefs.edit().putInt("search_button_color", searchButtonColor).apply();
        });

        plusSearchButtonColorBtn.setOnClickListener(v -> {
            searchButtonColor = (searchButtonColor + 1) % 5;
            searchButtonColorValueTv.setText(getSearchButtonColorText(searchButtonColor));
            prefs.edit().putInt("search_button_color", searchButtonColor).apply();
        });

        minusSearchButtonEffectBtn.setOnClickListener(v -> {
            searchButtonEffect = (searchButtonEffect - 1 + EFFECT_NAMES.length) % EFFECT_NAMES.length;
            searchButtonEffectValueTv.setText(EFFECT_NAMES[searchButtonEffect]);
            prefs.edit().putInt("search_button_effect", searchButtonEffect).apply();
            refreshVisibility();
            refreshPagination();
        });

        plusSearchButtonEffectBtn.setOnClickListener(v -> {
            searchButtonEffect = (searchButtonEffect + 1) % EFFECT_NAMES.length;
            searchButtonEffectValueTv.setText(EFFECT_NAMES[searchButtonEffect]);
            prefs.edit().putInt("search_button_effect", searchButtonEffect).apply();
            refreshVisibility();
            refreshPagination();
        });

        minusSearchButtonEffectColorBtn.setOnClickListener(v -> {
            searchButtonEffectColor = (searchButtonEffectColor - 1 + EFFECT_COLOR_NAMES.length) % EFFECT_COLOR_NAMES.length;
            searchButtonEffectColorValueTv.setText(EFFECT_COLOR_NAMES[searchButtonEffectColor]);
            prefs.edit().putInt("search_button_effect_color", searchButtonEffectColor).apply();
        });

        plusSearchButtonEffectColorBtn.setOnClickListener(v -> {
            searchButtonEffectColor = (searchButtonEffectColor + 1) % EFFECT_COLOR_NAMES.length;
            searchButtonEffectColorValueTv.setText(EFFECT_COLOR_NAMES[searchButtonEffectColor]);
            prefs.edit().putInt("search_button_effect_color", searchButtonEffectColor).apply();
        });

        initPagination(this::refreshVisibility);
    }

    private void refreshVisibility() {
        LinearLayout searchButtonSizeLayout = findViewById(R.id.search_button_size_layout);
        LinearLayout searchButtonColorLayout = findViewById(R.id.search_button_color_layout);
        LinearLayout searchButtonEffectLayout = findViewById(R.id.search_button_effect_layout);
        LinearLayout searchButtonEffectColorLayout = findViewById(R.id.search_button_effect_color_layout);

        if (showSearchButton == 0) {
            searchButtonSizeLayout.setVisibility(View.GONE);
            searchButtonColorLayout.setVisibility(View.GONE);
            searchButtonEffectLayout.setVisibility(View.GONE);
            searchButtonEffectColorLayout.setVisibility(View.GONE);
        } else {
            searchButtonSizeLayout.setVisibility(View.VISIBLE);
            searchButtonColorLayout.setVisibility(View.VISIBLE);
            searchButtonEffectLayout.setVisibility(View.VISIBLE);
            searchButtonEffectColorLayout.setVisibility(searchButtonEffect > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private String getSearchButtonColorText(int color) {
        switch (color) {
            case 0:
                return "FOLLOW THEME";
            case 1:
                return "DARK";
            case 2:
                return "WHITE";
            case 3:
                return "DYNAMIC DARK";
            case 4:
                return "DYNAMIC LIGHT";
            default:
                return "FOLLOW THEME";
        }
    }

    private String getOnOffText(int pos) {
        return pos == 1 ? "ON" : "OFF";
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
