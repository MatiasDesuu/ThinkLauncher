package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.views.ColorPickerView;

public class ColorPickerDialog extends Dialog {

    public interface ColorPickerCallback {
        void onColorSelected(int color);
    }

    private ColorPickerCallback callback;
    private int currentColor;
    private int theme;

    public ColorPickerDialog(Context context, int initialColor, ColorPickerCallback callback) {
        super(context, R.style.NoAnimationDialog);
        this.currentColor = initialColor;
        this.callback = callback;
        init();
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        
        setContentView(R.layout.dialog_color_picker);
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        View root = findViewById(R.id.root_layout);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, getContext());
            GradientDrawable drawable = (GradientDrawable) root.getBackground();
            drawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));
        }

        ColorPickerView colorPickerView = findViewById(R.id.color_picker_view);
        View colorPreview = findViewById(R.id.color_preview);
        EditText hexEditText = findViewById(R.id.hex_edit_text);
        TextView cancelButton = findViewById(R.id.cancel_button);
        TextView okButton = findViewById(R.id.ok_button);

        // Aplicar bordes a los elementos
        int textColor = ThemeUtils.getTextColor(theme, getContext());
        float density = getContext().getResources().getDisplayMetrics().density;
        
        colorPickerView.setBorderColor(textColor);
        
        GradientDrawable previewDrawable = new GradientDrawable();
        previewDrawable.setColor(currentColor);
        previewDrawable.setStroke((int) (2 * density), textColor);
        colorPreview.setBackground(previewDrawable);

        colorPickerView.setColor(currentColor);
        hexEditText.setText(String.format("#%06X", (0xFFFFFF & currentColor)));

        ThemeUtils.applyEditTextTheme(hexEditText, theme, getContext());
        GradientDrawable editDrawable = (GradientDrawable) hexEditText.getBackground();
        editDrawable.setStroke((int) (2 * getContext().getResources().getDisplayMetrics().density), ThemeUtils.getTextColor(theme, getContext()));
        hexEditText.setTextColor(ThemeUtils.getTextColor(theme, getContext()));

        ThemeUtils.applyButtonTheme(cancelButton, theme, getContext());
        ThemeUtils.applyButtonTheme(okButton, theme, getContext());

        final boolean[] isUpdating = {false};

        colorPickerView.setOnColorChangedListener(color -> {
            if (!isUpdating[0]) {
                isUpdating[0] = true;
                currentColor = color;
                ((GradientDrawable) colorPreview.getBackground()).setColor(color);
                hexEditText.setText(String.format("#%06X", (0xFFFFFF & color)));
                isUpdating[0] = false;
            }
        });

        hexEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdating[0]) {
                    try {
                        int color = Color.parseColor(s.toString());
                        isUpdating[0] = true;
                        currentColor = color;
                        colorPickerView.setColor(color);
                        ((GradientDrawable) colorPreview.getBackground()).setColor(color);
                        isUpdating[0] = false;
                    } catch (Exception ignored) {}
                }
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
        okButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onColorSelected(currentColor);
            }
            dismiss();
        });
    }
}
