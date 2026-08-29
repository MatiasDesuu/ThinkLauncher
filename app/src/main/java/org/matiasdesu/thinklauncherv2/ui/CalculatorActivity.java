package org.matiasdesu.thinklauncherv2.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Stack;

public class CalculatorActivity extends AppCompatActivity {

    private TextView expressionView;
    private TextView resultView;
    private StringBuilder expression = new StringBuilder();
    private String lastResult = "";
    private boolean resetOnNextDigit = false;
    private int theme;
    private boolean showWallpaperBackdrop;
    private int surfaceColor;
    private boolean appLauncherAnimations;
    private SharedPreferences prefs;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(CalculatorActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        boolean opacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1;
        appLauncherAnimations = prefs.getInt("screen_animations", 0) == 1;
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
        surfaceColor = backdrop.surfaceColor;
        showWallpaperBackdrop = backdrop.showWallpaperBackdrop;

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        View topLayout = findViewById(R.id.top_layout);
        View divider = findViewById(R.id.divider);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, surfaceColor, topLayout);
        if (divider != null) divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        TextView titleView = findViewById(R.id.calculator_title);
        ThemeUtils.applyTextColor(titleView, theme, this);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        expressionView = findViewById(R.id.expression_view);
        resultView = findViewById(R.id.result_view);
        ThemeUtils.applyTextColor(expressionView, theme, this);
        ThemeUtils.applyTextColor(resultView, theme, this);
        if (prefs.getBoolean("bold_text", true)) {
            expressionView.setTypeface(null, android.graphics.Typeface.BOLD);
            resultView.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        bindButtons();
        updateDisplay();
    }

    private void bindButtons() {
        int txt = ThemeUtils.getTextColor(theme, this);
        int bg = ThemeUtils.getBgColor(theme, this);
        int[] ids = new int[]{
                R.id.btn_ac, R.id.btn_paren, R.id.btn_percent, R.id.btn_div,
                R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_mul,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_sub,
                R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_add,
                R.id.btn_0, R.id.btn_dot, R.id.btn_del, R.id.btn_eq
        };
        for (int id : ids) {
            TextView tv = findViewById(id);
            if (tv == null) continue;
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setColor(bg);
            d.setStroke((int) (2 * getResources().getDisplayMetrics().density), txt);
            int r = org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.getCornerRadiusPx(this);
            if (r > 0) d.setCornerRadius(r);
            tv.setBackground(d);
            tv.setTextColor(txt);
            int pad = (int) (4 * getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad);
        }
        TextView eq = findViewById(R.id.btn_eq);
        if (eq != null) {
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setColor(txt);
            d.setStroke((int) (2 * getResources().getDisplayMetrics().density), txt);
            int r = org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.getCornerRadiusPx(this);
            if (r > 0) d.setCornerRadius(r);
            eq.setBackground(d);
            eq.setTextColor(bg);
        }

        findViewById(R.id.btn_0).setOnClickListener(v -> inputDigit("0"));
        findViewById(R.id.btn_1).setOnClickListener(v -> inputDigit("1"));
        findViewById(R.id.btn_2).setOnClickListener(v -> inputDigit("2"));
        findViewById(R.id.btn_3).setOnClickListener(v -> inputDigit("3"));
        findViewById(R.id.btn_4).setOnClickListener(v -> inputDigit("4"));
        findViewById(R.id.btn_5).setOnClickListener(v -> inputDigit("5"));
        findViewById(R.id.btn_6).setOnClickListener(v -> inputDigit("6"));
        findViewById(R.id.btn_7).setOnClickListener(v -> inputDigit("7"));
        findViewById(R.id.btn_8).setOnClickListener(v -> inputDigit("8"));
        findViewById(R.id.btn_9).setOnClickListener(v -> inputDigit("9"));
        findViewById(R.id.btn_dot).setOnClickListener(v -> inputDot());
        findViewById(R.id.btn_add).setOnClickListener(v -> inputOperator("+"));
        findViewById(R.id.btn_sub).setOnClickListener(v -> inputOperator("-"));
        findViewById(R.id.btn_mul).setOnClickListener(v -> inputOperator("*"));
        findViewById(R.id.btn_div).setOnClickListener(v -> inputOperator("/"));
        findViewById(R.id.btn_percent).setOnClickListener(v -> inputPercent());
        findViewById(R.id.btn_paren).setOnClickListener(v -> inputParen());
        findViewById(R.id.btn_ac).setOnClickListener(v -> clearAll());
        findViewById(R.id.btn_del).setOnClickListener(v -> deleteLast());
        findViewById(R.id.btn_eq).setOnClickListener(v -> evaluate());
    }

    private void inputDigit(String d) {
        if (resetOnNextDigit) {
            expression.setLength(0);
            resetOnNextDigit = false;
        }
        if (expression.length() > 0 && expression.charAt(expression.length() - 1) == ')') {
            expression.append("*");
        }
        expression.append(d);
        updateDisplay();
    }

    private void inputDot() {
        if (resetOnNextDigit) {
            expression.setLength(0);
            expression.append("0.");
            resetOnNextDigit = false;
            updateDisplay();
            return;
        }
        int lastOp = lastOperatorIndex();
        String current = expression.substring(lastOp + 1);
        if (current.contains(".")) return;
        if (current.isEmpty()) expression.append("0.");
        else expression.append(".");
        updateDisplay();
    }

    private void inputOperator(String op) {
        if (expression.length() == 0) {
            if (!lastResult.isEmpty() && !lastResult.equals("Error")) {
                expression.append(lastResult);
            } else return;
        }
        if (resetOnNextDigit) resetOnNextDigit = false;
        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (last == '(') return;
            if (last == '+' || last == '-' || last == '*' || last == '/' ) {
                expression.setCharAt(expression.length() - 1, op.charAt(0));
                updateDisplay();
                return;
            }
        }
        expression.append(op);
        updateDisplay();
    }

    private void inputPercent() {
        if (expression.length() == 0) return;
        int lastOp = lastOperatorIndex();
        String token = expression.substring(lastOp + 1);
        if (token.isEmpty() || token.equals("-")) return;
        try {
            double v = Double.parseDouble(token);
            double p = v / 100.0;
            String s = formatResult(p);
            expression.replace(lastOp + 1, expression.length(), s);
            updateDisplay();
        } catch (Exception ignored) {}
    }

    private void inputParen() {
        if (resetOnNextDigit) {
            expression.setLength(0);
            resetOnNextDigit = false;
        }
        int open = 0, close = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') open++;
            else if (c == ')') close++;
        }
        boolean shouldOpen = true;
        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (open > close && (last >= '0' && last <= '9' || last == '.' || last == ')')) {
                shouldOpen = false;
            } else if (last == '(' || last == '+' || last == '-' || last == '*' || last == '/' ) {
                shouldOpen = true;
            } else if (open > close) {
                shouldOpen = false;
            }
        }
        if (shouldOpen) {
            if (expression.length() > 0) {
                char last = expression.charAt(expression.length() - 1);
                if ((last >= '0' && last <= '9') || last == '.' || last == ')') {
                    expression.append("*");
                }
            }
            expression.append("(");
        } else {
            if (close >= open) {
                if (expression.length() > 0) {
                    char last = expression.charAt(expression.length() - 1);
                    if ((last >= '0' && last <= '9') || last == '.' || last == ')') {
                        expression.append("*");
                    }
                }
                expression.append("(");
            } else {
                if (expression.length() == 0) return;
                char last = expression.charAt(expression.length() - 1);
                if (last == '+' || last == '-' || last == '*' || last == '/' || last == '(') return;
                expression.append(")");
            }
        }
        updateDisplay();
    }

    private void deleteLast() {
        if (resetOnNextDigit) {
            clearAll();
            return;
        }
        if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
            updateDisplay();
        }
    }

    private void clearAll() {
        expression.setLength(0);
        lastResult = "";
        resetOnNextDigit = false;
        updateDisplay();
    }

    private void evaluate() {
        if (expression.length() == 0) return;
        String expr = expression.toString();
        char last = expr.charAt(expr.length() - 1);
        if (last == '+' || last == '-' || last == '*' || last == '/' || last == '(') {
            expr = expr.substring(0, expr.length() - 1);
        }
        if (expr.isEmpty()) return;
        try {
            double res = eval(expr);
            if (Double.isInfinite(res) || Double.isNaN(res)) {
                lastResult = "Error";
            } else {
                lastResult = formatResult(res);
            }
            expressionView.setText(expr);
            resultView.setText(lastResult);
            expression.setLength(0);
            expression.append(lastResult.equals("Error") ? "" : lastResult);
            resetOnNextDigit = true;
        } catch (Exception e) {
            lastResult = "Error";
            resultView.setText("Error");
            resetOnNextDigit = true;
        }
    }

    private void updateDisplay() {
        if (expression.length() == 0) {
            expressionView.setText("");
            resultView.setText(lastResult.isEmpty() ? "0" : lastResult);
        } else {
            expressionView.setText(expression.toString().replace("*", "×").replace("/", "÷"));
            try {
                String expr = expression.toString();
                char last = expr.charAt(expr.length() - 1);
                if (last == '+' || last == '-' || last == '*' || last == '/' || last == '(') expr = expr.substring(0, expr.length() - 1);
                if (!expr.isEmpty()) {
                    double v = eval(expr);
                    if (!Double.isInfinite(v) && !Double.isNaN(v)) resultView.setText(formatResult(v));
                }
            } catch (Exception ignored) {}
            if (expressionView.getText().length() == 0) resultView.setText("0");
        }
    }

    private int lastOperatorIndex() {
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == '+' || c == '*' || c == '/' || c == '(' || c == ')') return i;
            if (c == '-') {
                if (i == 0) return -1;
                char prev = expression.charAt(i - 1);
                if (prev == '+' || prev == '-' || prev == '*' || prev == '/' || prev == '(') continue;
                return i;
            }
        }
        return -1;
    }

    private String formatResult(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        String s = String.valueOf(v);
        if (s.contains("E") || s.contains("e")) return s;
        if (s.length() > 10) {
            s = String.format("%.8f", v);
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    private double eval(String expr) throws Exception {
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.') {
                num.append(c);
            } else if (c == '-' && (i == 0 || expr.charAt(i - 1) == '+' || expr.charAt(i - 1) == '-' || expr.charAt(i - 1) == '*' || expr.charAt(i - 1) == '/' || expr.charAt(i - 1) == '(')) {
                num.append(c);
            } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')') {
                if (num.length() > 0) {
                    tokens.add(num.toString());
                    num.setLength(0);
                }
                tokens.add(String.valueOf(c));
            }
        }
        if (num.length() > 0) tokens.add(num.toString());

        Stack<Double> values = new Stack<>();
        Stack<String> ops = new Stack<>();
        for (String tok : tokens) {
            if (tok.equals("(")) {
                ops.push(tok);
            } else if (tok.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {
                    applyOp(values, ops.pop());
                }
                if (ops.isEmpty()) throw new Exception("mismatch");
                ops.pop();
            } else if (tok.equals("+") || tok.equals("-") || tok.equals("*") || tok.equals("/")) {
                while (!ops.isEmpty() && !ops.peek().equals("(") && precedence(ops.peek()) >= precedence(tok)) {
                    applyOp(values, ops.pop());
                }
                ops.push(tok);
            } else {
                values.push(Double.parseDouble(tok));
            }
        }
        while (!ops.isEmpty()) {
            if (ops.peek().equals("(") || ops.peek().equals(")")) throw new Exception("mismatch");
            applyOp(values, ops.pop());
        }
        if (values.isEmpty()) throw new Exception("empty");
        return values.pop();
    }

    private int precedence(String op) {
        if (op.equals("*") || op.equals("/")) return 2;
        if (op.equals("+") || op.equals("-")) return 1;
        return 0;
    }

    private void applyOp(Stack<Double> values, String op) throws Exception {
        if (values.size() < 2) throw new Exception("invalid");
        double b = values.pop();
        double a = values.pop();
        double r;
        if (op.equals("+")) r = a + b;
        else if (op.equals("-")) r = a - b;
        else if (op.equals("*")) r = a * b;
        else if (op.equals("/")) {
            if (b == 0) throw new Exception("div0");
            r = a / b;
        } else throw new Exception("op");
        values.push(r);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontHelper.applyToViewTree(this, findViewById(android.R.id.content));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(homeButtonReceiver); } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
    }
}
