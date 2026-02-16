package org.matiasdesu.thinklauncherv2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.core.view.WindowCompat;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.os.Build;

import org.matiasdesu.thinklauncherv2.adapters.AppAdapter;
import org.matiasdesu.thinklauncherv2.services.LockAccessibilityService;
import org.matiasdesu.thinklauncherv2.ui.AppLauncherActivity;
import org.matiasdesu.thinklauncherv2.ui.AppSelectorActivity;
import org.matiasdesu.thinklauncherv2.ui.StrokeTextView;
import org.matiasdesu.thinklauncherv2.ui.ShadowOutlineDrawable;
import org.matiasdesu.thinklauncherv2.utils.AppNamePositionHelper;
import org.matiasdesu.thinklauncherv2.utils.DynamicIconHelper;
import org.matiasdesu.thinklauncherv2.utils.HomePagesManager;
import org.matiasdesu.thinklauncherv2.utils.IconMonochromeHelper;
import org.matiasdesu.thinklauncherv2.utils.IconShapeHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.BigmeShims;
import org.matiasdesu.thinklauncherv2.utils.WallpaperHelper;
import android.graphics.Bitmap;

public class MainActivity extends Activity {

    private List<String> appLabels;
    private List<String> appPackages;
    private LinearLayout[] appSlots;
    private int maxApps;
    private int textSize;
    private int iconSize;
    private boolean boldText;
    private int textEffect;
    private int effectColor;
    private int iconEffect;
    private int iconEffectColor;
    private boolean showIcons;
    private boolean showAppNames;
    private int appNamePosition;
    private boolean monochromeIcons;
    private boolean dynamicIcons;
    private boolean dynamicColors;
    private boolean invertIconColors;
    private boolean invertHomeColors;
    private boolean iconBackground;
    private int iconShape;
    private int homeAlignment;
    private int homeVerticalAlignment;
    private int homeColumns;
    private int homePages;
    private boolean hidePagination;
    private int timePosition;
    private int dateVerticalPosition;
    private int datePosition;
    private int dateHorizontalPosition;
    private int timeHorizontalPosition;
    private int timeFontSize;
    private int timeColor;
    private int timeEffect;
    private int timeEffectColor;
    private int dateFontSize;
    private int dateColor;
    private int dateEffect;
    private int dateEffectColor;
    private int fullMonthName;
    private int theme;
    private boolean hasWallpaper;
    private int textColor;
    private int appTextColor;
    private int doubleTapLock;
    private int showSettingsButton;
    private int showSearchButton;
    private int settingsButtonSize;
    private int settingsButtonColor;
    private int settingsButtonEffect;
    private int settingsButtonEffectColor;
    private int searchButtonSize;
    private int searchButtonColor;
    private int searchButtonEffect;
    private int searchButtonEffectColor;
    private String clockAppPkg;
    private String dateAppPkg;
    private TextView timeView;
    private TextView dateView;
    private RelativeLayout rootLayout;
    private LinearLayout mainLayout;
    private HomePagesManager homePagesManager;
    private SimpleDateFormat timeSdf;
    private SimpleDateFormat dateSdf;
    private Handler handler;
    private GestureHandler gestureHandler;
    private ImageView settingsButton;
    private ImageView searchButton;
    private ImageView wallpaperView;
    private int statusBarInset = 0;
    private int navBarInset = 0;
    private int homePaddingTop;
    private int homePaddingBottom;
    private int homePaddingLeft;
    private int homePaddingRight;
    private int homePaddingTopPx;
    private int homePaddingBottomPx;
    private int homePaddingLeftPx;
    private int homePaddingRightPx;
    private int customBgColor;
    private int customAccentColor;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(MainActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(mainIntent);
                }
            }
        }
    };

    private boolean handleFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String leftApp = prefs.getString("swipe_left_app", "");
        String rightApp = prefs.getString("swipe_right_app", "");
        String downApp = prefs.getString("swipe_down_app", "");
        String upApp = prefs.getString("swipe_up_app", "");
        float xDiff = e2.getX() - e1.getX();
        float yDiff = e2.getY() - e1.getY();
        if (Math.abs(xDiff) > Math.abs(yDiff) && Math.abs(xDiff) > 100 && Math.abs(velocityX) > 100) {
            if (xDiff > 0) {
                launchApp(rightApp);
            } else {
                launchApp(leftApp);
            }
        } else if (Math.abs(yDiff) > Math.abs(xDiff) && Math.abs(yDiff) > 100 && Math.abs(velocityY) > 100) {
            if (yDiff > 0) {
                launchApp(downApp);
            } else {
                launchApp(upApp);
            }
        }
        return true;
    }

    private int getEffectColorValue() {
        android.content.SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (theme == ThemeUtils.THEME_CUSTOM) {
            return ThemeUtils.getBgColor(theme, this);
        }
        switch (effectColor) {
            case 0:
                return android.graphics.Color.BLACK;
            case 1:
                return android.graphics.Color.WHITE;
            case 2:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
            case 3:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
            default:
                return android.graphics.Color.BLACK;
        }
    }

    private int getTimeEffectColorValue() {
        android.content.SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (theme == ThemeUtils.THEME_CUSTOM) {
            if (invertHomeColors) {
                return this.textColor;
            }
            return ThemeUtils.getBgColor(theme, this);
        }
        int color;
        switch (timeEffectColor) {
            case 0:
                color = android.graphics.Color.BLACK;
                break;
            case 1:
                color = android.graphics.Color.WHITE;
                break;
            case 2:
                color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
                break;
            case 3:
                color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
                break;
            default:
                color = android.graphics.Color.BLACK;
                break;
        }
        if (invertHomeColors && timeEffectColor == 4) {

        }
        return color;
    }

    private int getDateEffectColorValue() {
        android.content.SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (theme == ThemeUtils.THEME_CUSTOM) {
            if (invertHomeColors) {
                return this.textColor;
            }
            return ThemeUtils.getBgColor(theme, this);
        }
        int color;
        switch (dateEffectColor) {
            case 0:
                color = android.graphics.Color.BLACK;
                break;
            case 1:
                color = android.graphics.Color.WHITE;
                break;
            case 2:
                color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
                break;
            case 3:
                color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
                break;
            default:
                color = android.graphics.Color.BLACK;
                break;
        }
        return color;
    }

    private int getIconEffectColorValue() {
        android.content.SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (theme == ThemeUtils.THEME_CUSTOM) {
            return ThemeUtils.getBgColor(theme, this);
        }
        switch (iconEffectColor) {
            case 0:
                return android.graphics.Color.BLACK;
            case 1:
                return android.graphics.Color.WHITE;
            case 2:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
            case 3:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
            default:
                return android.graphics.Color.BLACK;
        }
    }

    private int getTimeColorValue() {
        int color;
        if (theme == ThemeUtils.THEME_CUSTOM) {
            color = this.textColor;
        } else if (timeColor == 0) {
            color = this.textColor;
        } else {
            switch (timeColor) {
                case 1:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_LIGHT, this);
                    break;
                case 2:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DARK, this);
                    break;
                case 3:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
                    break;
                case 4:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
                    break;
                default:
                    color = this.textColor;
                    break;
            }
        }

        if (invertHomeColors && (theme == ThemeUtils.THEME_CUSTOM || timeColor == 0)) {
            return ThemeUtils.getBgColor(theme, this);
        }
        return color;
    }

    private int getAppTextColorValue() {
        if (theme == ThemeUtils.THEME_CUSTOM) {
            return this.textColor;
        }
        if (appTextColor == 0) {
            return this.textColor;
        }
        switch (appTextColor) {
            case 1:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_LIGHT, this);
            case 2:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DARK, this);
            case 3:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
            case 4:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
            default:
                return this.textColor;
        }
    }

    private int getDateColorValue() {
        int color;
        if (theme == ThemeUtils.THEME_CUSTOM) {
            color = this.textColor;
        } else if (dateColor == 0) {
            color = this.textColor;
        } else {
            switch (dateColor) {
                case 1:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_LIGHT, this);
                    break;
                case 2:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DARK, this);
                    break;
                case 3:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
                    break;
                case 4:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
                    break;
                default:
                    color = this.textColor;
                    break;
            }
        }

        if (invertHomeColors && (theme == ThemeUtils.THEME_CUSTOM || dateColor == 0)) {
            return ThemeUtils.getBgColor(theme, this);
        }
        return color;
    }

    private int getSettingsButtonColorValue() {
        int color;
        if (theme == ThemeUtils.THEME_CUSTOM) {
            color = this.textColor;
        } else if (settingsButtonColor == 0) {
            color = this.textColor;
        } else {
            switch (settingsButtonColor) {
                case 1:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_LIGHT, this);
                    break;
                case 2:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DARK, this);
                    break;
                case 3:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
                    break;
                case 4:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
                    break;
                default:
                    color = this.textColor;
                    break;
            }
        }

        if (invertHomeColors && (theme == ThemeUtils.THEME_CUSTOM || settingsButtonColor == 0)) {
            return ThemeUtils.getBgColor(theme, this);
        }
        return color;
    }

    private int getSettingsButtonEffectColorValue() {
        android.content.SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (theme == ThemeUtils.THEME_CUSTOM) {
            if (invertHomeColors) {
                return this.textColor;
            }
            return ThemeUtils.getBgColor(theme, this);
        }
        switch (settingsButtonEffectColor) {
            case 0:
                return android.graphics.Color.BLACK;
            case 1:
                return android.graphics.Color.WHITE;
            case 2:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
            case 3:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
            default:
                return android.graphics.Color.BLACK;
        }
    }

    private int getSearchButtonColorValue() {
        int color;
        if (theme == ThemeUtils.THEME_CUSTOM) {
            color = this.textColor;
        } else if (searchButtonColor == 0) {
            color = this.textColor;
        } else {
            switch (searchButtonColor) {
                case 1:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_LIGHT, this);
                    break;
                case 2:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DARK, this);
                    break;
                case 3:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
                    break;
                case 4:
                    color = ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
                    break;
                default:
                    color = this.textColor;
                    break;
            }
        }

        if (invertHomeColors && (theme == ThemeUtils.THEME_CUSTOM || searchButtonColor == 0)) {
            return ThemeUtils.getBgColor(theme, this);
        }
        return color;
    }

    private int getSearchButtonEffectColorValue() {
        android.content.SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (theme == ThemeUtils.THEME_CUSTOM) {
            if (invertHomeColors) {
                return this.textColor;
            }
            return ThemeUtils.getBgColor(theme, this);
        }
        switch (searchButtonEffectColor) {
            case 0:
                return android.graphics.Color.BLACK;
            case 1:
                return android.graphics.Color.WHITE;
            case 2:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_LIGHT, this);
            case 3:
                return ThemeUtils.getTextColor(ThemeUtils.THEME_DYNAMIC_DARK, this);
            default:
                return android.graphics.Color.BLACK;
        }
    }

    private int getPaginationColorValue() {
        if (invertHomeColors) {
            return ThemeUtils.getBgColor(theme, this);
        }
        return this.textColor;
    }

    private void applyTextEffect(TextView tv) {
        applyTextEffect(tv, textEffect, getEffectColorValue());
    }

    private void applyTextEffect(TextView tv, int effect, int colorValue) {
        if (effect == 0) {
            tv.setShadowLayer(0, 0, 0, 0);
            if (tv instanceof StrokeTextView) {
                ((StrokeTextView) tv).setStroke(0, 0);
            }
            return;
        }

        if (effect == 1) {
            if (tv instanceof StrokeTextView) {
                ((StrokeTextView) tv).setStroke(0, 0);
            }
            tv.setShadowLayer(4.0f, 2.0f, 2.0f, colorValue);
        } else if (effect == 2) {
            tv.setShadowLayer(0, 0, 0, 0);
            if (tv instanceof StrokeTextView) {
                float width = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 4f,
                        getResources().getDisplayMetrics());
                ((StrokeTextView) tv).setStroke(colorValue, width);
            } else {
                tv.setShadowLayer(5.0f, 0.0f, 0.0f, colorValue);
            }
        }
    }

    private void applyIconEffect(ImageView iv) {
        applyIconEffect(iv, iconEffect, getIconEffectColorValue());
    }

    private void applyIconEffect(ImageView iv, int effect, int color) {
        if (iv == null)
            return;

        int iconPaddingLeft = 0, iconPaddingRight = 0, iconPaddingTop = 0, iconPaddingBottom = 0;
        if (showAppNames) {
            if (appNamePosition == AppNamePositionHelper.POSITION_RIGHT) {
                iconPaddingRight = 16;
            } else if (appNamePosition == AppNamePositionHelper.POSITION_LEFT) {
                iconPaddingLeft = 16;
            } else if (appNamePosition == AppNamePositionHelper.POSITION_TOP) {
                iconPaddingTop = 8;
            } else if (appNamePosition == AppNamePositionHelper.POSITION_BOTTOM) {
                iconPaddingBottom = 8;
            }
        }
        iv.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);

        if (effect == 0) {
            Drawable current = iv.getDrawable();
            if (current instanceof ShadowOutlineDrawable) {
                iv.setImageDrawable(((ShadowOutlineDrawable) current).getInnerDrawable());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                iv.setElevation(0);
            }
            return;
        }

        Drawable original = iv.getDrawable();
        if (original != null) {
            if (original instanceof ShadowOutlineDrawable) {
                original = ((ShadowOutlineDrawable) original).getInnerDrawable();
            }

            if (iv == settingsButton || iv == searchButton) {
                if (original != null && !(original instanceof InsetDrawable)) {
                    original = new InsetDrawable(original, 0.15f);
                }
            }

            float offset = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 1.5f, getResources().getDisplayMetrics());

            float adjustedOffset = offset * 1.5f;

            if ("special".equals(iv.getTag()) && (iv == settingsButton || iv == searchButton || !iconBackground)) {
                adjustedOffset = offset;
            }

            int p = (int) (adjustedOffset * 1.5f);

            iv.setPadding(iv.getPaddingLeft() + p, iv.getPaddingTop() + p,
                    iv.getPaddingRight() + p, iv.getPaddingBottom() + p);

            iv.setImageDrawable(new ShadowOutlineDrawable(original, effect, color, adjustedOffset));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                iv.setElevation(0);
            }
        }
    }

    private void createTimeViews(int bgColor, int textColor) {
        boolean showTime = timePosition == 1;
        boolean showDate = datePosition != 0;
        boolean hasWallpaper = WallpaperHelper.hasWallpaper(this);
        int timeDateBgColor = hasWallpaper ? android.graphics.Color.TRANSPARENT : bgColor;

        if (showDate && dateVerticalPosition == 0) {

            String format = fullMonthName == 1 ? "dd MMMM yyyy" : "dd MMM yyyy";
            dateSdf = new SimpleDateFormat(format);
            dateView = new StrokeTextView(this);
            dateView.setId(View.generateViewId());
            dateView.setText(dateSdf.format(new Date()));
            dateView.setTextColor(getDateColorValue());
            dateView.setTextSize(dateFontSize);
            dateView.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
            applyTextEffect(dateView, dateEffect, getDateEffectColorValue());
            dateView.setPadding(32, 5, 32, 5);
            dateView.setBackgroundColor(timeDateBgColor);
            dateView.setGravity(getHorizontalGravity(dateHorizontalPosition));
            GestureDetector.SimpleOnGestureListener dateGestureListener = new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (dateAppPkg.equals("system_default")) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    } else if (!dateAppPkg.isEmpty()) {
                        launchApp(dateAppPkg);
                    }
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return handleFling(e1, e2, velocityX, velocityY);
                }
            };
            GestureDetector dateGestureDetector = new GestureDetector(this, dateGestureListener);
            dateView.setOnTouchListener((v, event) -> dateGestureDetector.onTouchEvent(event));
            RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            dateParams.addRule(getRelativeHorizontalRule(dateHorizontalPosition));
            if ((showSettingsButton == 1 || showSearchButton == 1) && dateHorizontalPosition == 2) {
                int maxBtnSize = Math.max(showSettingsButton == 1 ? settingsButtonSize : 0,
                        showSearchButton == 1 ? searchButtonSize : 0);
                int buttonSizePx = (int) android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                dateParams.rightMargin = buttonSizePx + 16;
            }
            rootLayout.addView(dateView, dateParams);
            if (showTime) {
                timeSdf = new SimpleDateFormat("HH:mm");
                timeView = new StrokeTextView(this);
                timeView.setId(View.generateViewId());
                timeView.setText(timeSdf.format(new Date()));
                timeView.setTextColor(getTimeColorValue());
                timeView.setTextSize(timeFontSize);
                timeView.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
                applyTextEffect(timeView, timeEffect, getTimeEffectColorValue());
                timeView.setPadding(32, 5, 32, 5);
                timeView.setBackgroundColor(timeDateBgColor);
                timeView.setGravity(getHorizontalGravity(timeHorizontalPosition));
                GestureDetector.SimpleOnGestureListener timeGestureListener = new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        if (clockAppPkg.equals("system_default")) {
                            Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        } else if (!clockAppPkg.isEmpty()) {
                            launchApp(clockAppPkg);
                        }
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        return handleFling(e1, e2, velocityX, velocityY);
                    }
                };
                GestureDetector timeGestureDetector = new GestureDetector(this, timeGestureListener);
                timeView.setOnTouchListener((v, event) -> timeGestureDetector.onTouchEvent(event));
                RelativeLayout.LayoutParams timeParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                timeParams.addRule(RelativeLayout.BELOW, dateView.getId());
                timeParams.addRule(getRelativeHorizontalRule(timeHorizontalPosition));
                if ((showSettingsButton == 1 || showSearchButton == 1) && timeHorizontalPosition == 2) {
                    int maxBtnSize = Math.max(showSettingsButton == 1 ? settingsButtonSize : 0,
                            showSearchButton == 1 ? searchButtonSize : 0);
                    int buttonSizePx = (int) android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                    timeParams.rightMargin = buttonSizePx + 16;
                }
                rootLayout.addView(timeView, timeParams);
            }
        } else {

            if (showTime) {
                timeSdf = new SimpleDateFormat("HH:mm");
                timeView = new StrokeTextView(this);
                timeView.setId(View.generateViewId());
                timeView.setText(timeSdf.format(new Date()));
                timeView.setTextColor(getTimeColorValue());
                timeView.setTextSize(timeFontSize);
                timeView.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
                applyTextEffect(timeView, timeEffect, getTimeEffectColorValue());
                timeView.setPadding(32, 5, 32, 5);
                timeView.setBackgroundColor(timeDateBgColor);
                timeView.setGravity(getHorizontalGravity(timeHorizontalPosition));
                GestureDetector.SimpleOnGestureListener timeGestureListener = new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        if (clockAppPkg.equals("system_default")) {
                            Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        } else if (!clockAppPkg.isEmpty()) {
                            launchApp(clockAppPkg);
                        }
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        return handleFling(e1, e2, velocityX, velocityY);
                    }
                };

                GestureDetector timeGestureDetector = new GestureDetector(this,
                        timeGestureListener);
                timeView.setOnTouchListener((v, event) -> timeGestureDetector.onTouchEvent(event));
                RelativeLayout.LayoutParams timeParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                timeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                timeParams.addRule(getRelativeHorizontalRule(timeHorizontalPosition));
                if ((showSettingsButton == 1 || showSearchButton == 1) && timeHorizontalPosition == 2) {
                    int maxBtnSize = Math.max(showSettingsButton == 1 ? settingsButtonSize : 0,
                            showSearchButton == 1 ? searchButtonSize : 0);
                    int buttonSizePx = (int) android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                    timeParams.rightMargin = buttonSizePx + 16;
                }
                rootLayout.addView(timeView, timeParams);
            }

            if (showDate)

            {
                String format = fullMonthName == 1 ? "dd MMMM yyyy" : "dd MMM yyyy";
                dateSdf = new SimpleDateFormat(format);
                dateView = new StrokeTextView(this);
                dateView.setId(View.generateViewId());
                dateView.setText(dateSdf.format(new Date()));
                dateView.setTextColor(getDateColorValue());
                dateView.setTextSize(dateFontSize);
                dateView.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
                applyTextEffect(dateView, dateEffect, getDateEffectColorValue());
                dateView.setPadding(32, 5, 32, 5);
                dateView.setBackgroundColor(timeDateBgColor);
                dateView.setGravity(getHorizontalGravity(dateHorizontalPosition));
                GestureDetector.SimpleOnGestureListener dateGestureListener = new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        if (dateAppPkg.equals("system_default")) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        } else if (!dateAppPkg.isEmpty()) {
                            launchApp(dateAppPkg);
                        }
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        return handleFling(e1, e2, velocityX, velocityY);
                    }
                };
                GestureDetector dateGestureDetector = new GestureDetector(this, dateGestureListener);
                dateView.setOnTouchListener((v, event) -> dateGestureDetector.onTouchEvent(event));
                RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                if (showTime) {
                    dateParams.addRule(RelativeLayout.BELOW, timeView.getId());
                } else {
                    dateParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                }
                dateParams.addRule(getRelativeHorizontalRule(dateHorizontalPosition));
                if ((showSettingsButton == 1 || showSearchButton == 1) && dateHorizontalPosition == 2) {
                    int maxBtnSize = Math.max(showSettingsButton == 1 ? settingsButtonSize : 0,
                            showSearchButton == 1 ? searchButtonSize : 0);
                    int buttonSizePx = (int) android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                    dateParams.rightMargin = buttonSizePx + 16;
                }
                rootLayout.addView(dateView, dateParams);
            }
        }
    }

    private void createSettingsButton(int bgColor, int textColor) {
        if (showSettingsButton == 1) {
            settingsButton = new ImageView(this);
            settingsButton.setId(View.generateViewId());
            settingsButton.setTag("special");
            settingsButton.setImageResource(R.drawable.settings);
            settingsButton.setColorFilter(getSettingsButtonColorValue());
            applyIconEffect(settingsButton, settingsButtonEffect, getSettingsButtonEffectColorValue());
            int sizePx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                    settingsButtonSize, getResources().getDisplayMetrics());
            RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(sizePx, sizePx);
            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            buttonParams.rightMargin = 16;
            buttonParams.topMargin = 5;
            settingsButton.setPadding(16, 8, 16, 8);
            settingsButton.setOnClickListener(v -> {
                try {
                    Class<?> clazz = Class.forName("org.matiasdesu.thinklauncherv2.settings.SettingsActivity");
                    Intent intent = new Intent(MainActivity.this, clazz);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            });
            rootLayout.addView(settingsButton, buttonParams);
        }
    }

    private void createSearchButton(int bgColor, int textColor) {
        if (showSearchButton == 1) {
            searchButton = new ImageView(this);
            searchButton.setId(View.generateViewId());
            searchButton.setTag("special");
            searchButton.setImageResource(R.drawable.search);
            searchButton.setColorFilter(getSearchButtonColorValue());
            applyIconEffect(searchButton, searchButtonEffect, getSearchButtonEffectColorValue());
            int sizePx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                    searchButtonSize, getResources().getDisplayMetrics());
            RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(sizePx, sizePx);
            if (showSettingsButton == 1) {
                buttonParams.addRule(RelativeLayout.BELOW, settingsButton.getId());
            } else {
                buttonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            }
            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            buttonParams.rightMargin = 16;
            buttonParams.topMargin = 5;
            searchButton.setPadding(16, 8, 16, 8);
            searchButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AppLauncherActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            });
            rootLayout.addView(searchButton, buttonParams);
        }
    }

    private void adjustMainLayoutPosition() {
        RelativeLayout.LayoutParams mainParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        View topView = null;
        if (timePosition == 1 || datePosition != 0) {
            if (timePosition == 1 && datePosition != 0) {
                if (dateVerticalPosition == 0) {
                    topView = timeView;
                } else {
                    topView = dateView;
                }
            } else if (timePosition == 1) {
                topView = timeView;
            } else if (datePosition != 0) {
                topView = dateView;
            }
        }
        if (topView != null) {
            mainParams.addRule(RelativeLayout.BELOW, topView.getId());
        } else if (showSettingsButton == 1 || showSearchButton == 1) {
            if (showSearchButton == 1) {
                mainParams.addRule(RelativeLayout.BELOW, searchButton.getId());
            } else {
                mainParams.addRule(RelativeLayout.BELOW, settingsButton.getId());
            }
        } else {
            mainParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        }
        mainLayout.setLayoutParams(mainParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        theme = prefs.getInt("theme", 0);
        if (!prefs.contains("theme")) {
            prefs.edit().putInt("theme", 0).apply();
        }
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        BigmeShims.registerUnlockReceiver(this);
        BigmeShims.queryLauncherProvider(this);
        BigmeShims.queryLauncherProvider(this);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        int bgColor = ThemeUtils.getBgColor(theme, this);
        this.textColor = ThemeUtils.getTextColor(theme, this);
        this.hasWallpaper = WallpaperHelper.hasWallpaper(this);

        if (this.hasWallpaper) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(bgColor);
                getWindow().setNavigationBarColor(bgColor);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                if (!ThemeUtils.isDarkTheme(theme, this)) {
                    controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!ThemeUtils.isDarkTheme(theme, this)) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(0);
            }
        }
        maxApps = prefs.getInt("max_apps", 4);
        textSize = prefs.getInt("text_size", 32);
        iconSize = prefs.getInt("icon_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        appTextColor = prefs.getInt("app_text_color", 0);
        textEffect = prefs.getInt("text_effect", 0);
        effectColor = prefs.getInt("effect_color", 0);
        iconEffect = prefs.getInt("icon_effect", 0);
        iconEffectColor = prefs.getInt("icon_effect_color", 0);
        showIcons = prefs.getBoolean("show_icons", false);
        showAppNames = prefs.getBoolean("show_app_names", true);
        if (!showIcons)
            showAppNames = true;
        appNamePosition = prefs.getInt("app_name_position", AppNamePositionHelper.POSITION_RIGHT);
        monochromeIcons = prefs.getBoolean("monochrome_icons", false);
        dynamicIcons = prefs.getBoolean("dynamic_icons", false);
        dynamicColors = prefs.getBoolean("dynamic_colors", false);
        invertIconColors = prefs.getBoolean("invert_icon_colors", false);
        invertHomeColors = prefs.getBoolean("invert_home_colors", false);
        iconBackground = prefs.getBoolean("icon_background", true);
        iconShape = prefs.getInt("icon_shape", IconShapeHelper.SHAPE_SYSTEM);
        homeAlignment = prefs.getInt("home_alignment", 1);
        homeVerticalAlignment = prefs.getInt("home_vertical_alignment", 1);
        homeColumns = prefs.getInt("home_columns", 1);
        homePages = prefs.getInt("home_pages", 1);
        hidePagination = prefs.getBoolean("hide_pagination", false);
        timePosition = prefs.getInt("time_position", 0);
        dateVerticalPosition = prefs.getInt("date_vertical_position", 0);
        datePosition = prefs.getInt("date_position", 0);
        dateHorizontalPosition = prefs.getInt("date_horizontal_position", 0);
        timeHorizontalPosition = prefs.getInt("time_horizontal_position", 0);
        timeFontSize = prefs.getInt("time_font_size", 54);
        timeColor = prefs.getInt("time_color", 0);
        timeEffect = prefs.getInt("time_effect", 0);
        timeEffectColor = prefs.getInt("time_effect_color", 0);
        dateFontSize = prefs.getInt("date_font_size", 22);
        dateColor = prefs.getInt("date_color", 0);
        dateEffect = prefs.getInt("date_effect", 0);
        dateEffectColor = prefs.getInt("date_effect_color", 0);
        homePaddingTop = prefs.getInt("home_padding_top", 0);
        homePaddingBottom = prefs.getInt("home_padding_bottom", 0);
        homePaddingLeft = prefs.getInt("home_padding_left", 0);
        homePaddingRight = prefs.getInt("home_padding_right", 0);
        updatePaddingPx();
        fullMonthName = prefs.getInt("full_month_name", 0);
        doubleTapLock = prefs.getInt("double_tap_lock", 0);
        showSettingsButton = prefs.getInt("show_settings_button", 0);
        showSearchButton = prefs.getInt("show_search_button", 0);
        clockAppPkg = prefs.getString("clock_app_pkg", "system_default");
        dateAppPkg = prefs.getString("date_app_pkg", "system_default");
        settingsButtonSize = prefs.getInt("settings_button_size", 42);
        settingsButtonColor = prefs.getInt("settings_button_color", 0);
        settingsButtonEffect = prefs.getInt("settings_button_effect", 0);
        settingsButtonEffectColor = prefs.getInt("settings_button_effect_color", 0);
        searchButtonSize = prefs.getInt("search_button_size", 42);
        searchButtonColor = prefs.getInt("search_button_color", 0);
        searchButtonEffect = prefs.getInt("search_button_effect", 0);
        searchButtonEffectColor = prefs.getInt("search_button_effect_color", 0);
        customBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        customAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);

        appLabels = new ArrayList<>();
        appPackages = new ArrayList<>();
        int totalApps = homeColumns * maxApps;
        appSlots = new LinearLayout[totalApps];

        homePagesManager = new HomePagesManager(this, prefs, homePages, homeColumns, maxApps);
        homePagesManager.loadAppsForCurrentPage();
        appLabels.addAll(homePagesManager.getAppLabels());
        appPackages.addAll(homePagesManager.getAppPackages());

        this.rootLayout = (RelativeLayout) findViewById(R.id.root_layout);
        rootLayout.setBackgroundColor(bgColor);
        rootLayout.setClipChildren(false);
        rootLayout.setClipToPadding(false);
        this.mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        mainLayout.setClipChildren(false);
        mainLayout.setClipToPadding(false);

        wallpaperView = findViewById(R.id.wallpaper_view);
        loadWallpaper();

        TextView pageIndicator = findViewById(R.id.page_indicator);
        pageIndicator.setTextColor(getPaginationColorValue());
        homePagesManager.setPageIndicator(pageIndicator);
        homePagesManager.updatePageIndicator();
        pageIndicator.setVisibility((homePages > 1 && !hidePagination) ? View.VISIBLE : View.GONE);

        LinearLayout bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.setVisibility((homePages > 1 && !hidePagination) ? View.VISIBLE : View.GONE);
        updateGravity();

        ImageView prevButton = findViewById(R.id.prev_page_button);
        prevButton.setColorFilter(getPaginationColorValue());
        prevButton.setOnClickListener(v -> {
            int currentPage = homePagesManager.getCurrentPage();
            int newPage = currentPage > 0 ? currentPage - 1 : homePages - 1;
            homePagesManager.setCurrentPage(newPage);
            recreateHome();
        });

        ImageView nextButton = findViewById(R.id.next_page_button);
        nextButton.setColorFilter(getPaginationColorValue());
        nextButton.setOnClickListener(v -> {
            int currentPage = homePagesManager.getCurrentPage();
            int newPage = currentPage < homePages - 1 ? currentPage + 1 : 0;
            homePagesManager.setCurrentPage(newPage);
            recreateHome();
        });

        if (timePosition == 1 || datePosition != 0) {
            createTimeViews(bgColor, textColor);
        }

        createSettingsButton(bgColor, textColor);
        createSearchButton(bgColor, textColor);

        adjustMainLayoutPosition();

        createHomeLayout();

        gestureHandler = new GestureHandler();
        findViewById(R.id.root_layout).setOnTouchListener((v, event) -> gestureHandler.onTouch(event));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);
        gestureHandler.loadApps();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        int newMaxApps = prefs.getInt("max_apps", 4);
        int newTextSize = prefs.getInt("text_size", 32);
        int newIconSize = prefs.getInt("icon_size", 32);
        boolean newBoldText = prefs.getBoolean("bold_text", true);
        int newAppTextColor = prefs.getInt("app_text_color", 0);
        int newTextEffect = prefs.getInt("text_effect", 0);
        int newEffectColor = prefs.getInt("effect_color", 0);
        int newIconEffect = prefs.getInt("icon_effect", 0);
        int newIconEffectColor = prefs.getInt("icon_effect_color", 0);
        int newHomeAlignment = prefs.getInt("home_alignment", 1);
        int newHomeVerticalAlignment = prefs.getInt("home_vertical_alignment", 1);
        int newHomeColumns = prefs.getInt("home_columns", 1);
        int newHomePages = prefs.getInt("home_pages", 1);
        boolean newHidePagination = prefs.getBoolean("hide_pagination", false);
        int newTimePosition = prefs.getInt("time_position", 0);
        int newDateVerticalPosition = prefs.getInt("date_vertical_position", 0);
        int newDatePosition = prefs.getInt("date_position", 0);
        int newSettingsButtonSize = prefs.getInt("settings_button_size", 42);
        int newSettingsButtonColor = prefs.getInt("settings_button_color", 0);
        int newSettingsButtonEffect = prefs.getInt("settings_button_effect", 0);
        int newSettingsButtonEffectColor = prefs.getInt("settings_button_effect_color", 0);
        int newSearchButtonSize = prefs.getInt("search_button_size", 42);
        int newSearchButtonColor = prefs.getInt("search_button_color", 0);
        int newSearchButtonEffect = prefs.getInt("search_button_effect", 0);
        int newSearchButtonEffectColor = prefs.getInt("search_button_effect_color", 0);
        int newDateHorizontalPosition = prefs.getInt("date_horizontal_position", 0);
        int newTimeHorizontalPosition = prefs.getInt("time_horizontal_position", 0);
        int newTimeFontSize = prefs.getInt("time_font_size", 54);
        int newTimeColor = prefs.getInt("time_color", 0);
        int newTimeEffect = prefs.getInt("time_effect", 0);
        int newTimeEffectColor = prefs.getInt("time_effect_color", 0);
        int newDateFontSize = prefs.getInt("date_font_size", 22);
        int newDateColor = prefs.getInt("date_color", 0);
        int newDateEffect = prefs.getInt("date_effect", 0);
        int newDateEffectColor = prefs.getInt("date_effect_color", 0);
        int newHomePaddingTop = prefs.getInt("home_padding_top", 0);
        int newHomePaddingBottom = prefs.getInt("home_padding_bottom", 0);
        int newHomePaddingLeft = prefs.getInt("home_padding_left", 0);
        int newHomePaddingRight = prefs.getInt("home_padding_right", 0);
        int newFullMonthName = prefs.getInt("full_month_name", 0);
        int newTheme = prefs.getInt("theme", 0);
        int newShowSettingsButton = prefs.getInt("show_settings_button", 0);
        int newShowSearchButton = prefs.getInt("show_search_button", 0);
        String newClockAppPkg = prefs.getString("clock_app_pkg", "system_default");
        String newDateAppPkg = prefs.getString("date_app_pkg", "system_default");
        int newCustomBgColor = prefs.getInt("custom_bg_color", android.graphics.Color.WHITE);
        int newCustomAccentColor = prefs.getInt("custom_accent_color", android.graphics.Color.BLACK);
        boolean newShowIcons = prefs.getBoolean("show_icons", false);
        boolean newShowAppNames = prefs.getBoolean("show_app_names", true);
        if (!newShowIcons)
            newShowAppNames = true;
        int newAppNamePosition = prefs.getInt("app_name_position", AppNamePositionHelper.POSITION_RIGHT);
        boolean newMonochromeIcons = prefs.getBoolean("monochrome_icons", false);
        boolean newDynamicIcons = prefs.getBoolean("dynamic_icons", false);
        boolean newDynamicColors = prefs.getBoolean("dynamic_colors", false);
        boolean newInvertIconColors = prefs.getBoolean("invert_icon_colors", false);
        boolean newInvertHomeColors = prefs.getBoolean("invert_home_colors", false);
        boolean newIconBackground = prefs.getBoolean("icon_background", true);
        int newIconShape = prefs.getInt("icon_shape", IconShapeHelper.SHAPE_SYSTEM);
        int bgColor = ThemeUtils.getBgColor(newTheme, this);
        int textColor = ThemeUtils.getTextColor(newTheme, this);
        boolean newHasWallpaper = WallpaperHelper.hasWallpaper(this);
        boolean themeChanged = newTheme != theme ||
                (newTheme == ThemeUtils.THEME_CUSTOM
                        && (newCustomBgColor != customBgColor || newCustomAccentColor != customAccentColor));
        boolean textChanged = newTextSize != textSize || newBoldText != boldText || newAppTextColor != appTextColor
                || newTimeFontSize != timeFontSize
                || newDateFontSize != dateFontSize || newIconSize != iconSize
                || newSettingsButtonSize != settingsButtonSize || newSearchButtonSize != searchButtonSize
                || newTextEffect != textEffect || newEffectColor != effectColor
                || newTimeEffect != timeEffect || newTimeEffectColor != timeEffectColor
                || newDateEffect != dateEffect || newDateEffectColor != dateEffectColor;
        boolean iconChanged = newIconEffect != iconEffect || newIconEffectColor != iconEffectColor;
        boolean wallpaperChanged = newHasWallpaper != hasWallpaper;
        boolean layoutChanged = newMaxApps != maxApps || newHomeColumns != homeColumns || newHomePages != homePages
                || newHomeAlignment != homeAlignment || newHomeVerticalAlignment != homeVerticalAlignment
                || newTimePosition != timePosition || newDateVerticalPosition != dateVerticalPosition
                || newDatePosition != datePosition || newDateHorizontalPosition != dateHorizontalPosition
                || newHomePaddingTop != homePaddingTop || newHomePaddingBottom != homePaddingBottom
                || newHomePaddingLeft != homePaddingLeft || newHomePaddingRight != homePaddingRight
                || newTimeHorizontalPosition != timeHorizontalPosition || newFullMonthName != fullMonthName
                || newSearchButtonEffect != searchButtonEffect || newSearchButtonEffectColor != searchButtonEffectColor
                || newTimeEffect != timeEffect || newTimeEffectColor != timeEffectColor
                || newDateEffect != dateEffect || newDateEffectColor != dateEffectColor
                || newShowIcons != showIcons || newShowAppNames != showAppNames || newTimeColor != timeColor
                || newDateColor != dateColor
                || newShowSettingsButton != showSettingsButton || newShowSearchButton != showSearchButton
                || newAppNamePosition != appNamePosition
                || newTextEffect != textEffect || newEffectColor != effectColor || newIconEffect != iconEffect
                || newIconEffectColor != iconEffectColor || newMonochromeIcons != monochromeIcons
                || newDynamicIcons != dynamicIcons || newDynamicColors != dynamicColors
                || newInvertIconColors != invertIconColors || newInvertHomeColors != invertHomeColors
                || newIconBackground != iconBackground
                || newIconShape != iconShape || newHidePagination != hidePagination
                || !newClockAppPkg.equals(clockAppPkg) || !newDateAppPkg.equals(dateAppPkg)
                || newSettingsButtonColor != settingsButtonColor || newSearchButtonColor != searchButtonColor
                || wallpaperChanged;
        boolean onlyAlignmentChanged = (newHomeAlignment != homeAlignment
                || newHomeVerticalAlignment != homeVerticalAlignment)
                && !(newMaxApps != maxApps || newHomeColumns != homeColumns || newHomePages != homePages
                        || newTimePosition != timePosition || newDateVerticalPosition != dateVerticalPosition
                        || newDatePosition != datePosition || newDateHorizontalPosition != dateHorizontalPosition
                        || newTimeHorizontalPosition != timeHorizontalPosition || newFullMonthName != fullMonthName
                        || newShowSettingsButton != showSettingsButton || newShowSearchButton != showSearchButton
                        || newSettingsButtonColor != settingsButtonColor || newSearchButtonColor != searchButtonColor
                        || newShowIcons != showIcons || newShowAppNames != showAppNames
                        || newAppNamePosition != appNamePosition || newTextEffect != textEffect
                        || newEffectColor != effectColor || newIconEffect != iconEffect
                        || newIconEffectColor != iconEffectColor || !newClockAppPkg.equals(clockAppPkg)
                        || !newDateAppPkg.equals(dateAppPkg) || wallpaperChanged
                        || newInvertIconColors != invertIconColors || newInvertHomeColors != invertHomeColors);
        boolean visibilityChanged = newShowAppNames != showAppNames || newTextEffect != textEffect
                || newEffectColor != effectColor || newIconEffect != iconEffect
                || newIconEffectColor != iconEffectColor;

        if (themeChanged || textChanged || layoutChanged || iconChanged) {
            theme = newTheme;
            customBgColor = newCustomBgColor;
            customAccentColor = newCustomAccentColor;
            hasWallpaper = newHasWallpaper;
            textSize = newTextSize;
            iconSize = newIconSize;
            boldText = newBoldText;
            appTextColor = newAppTextColor;
            textEffect = newTextEffect;
            effectColor = newEffectColor;
            iconEffect = newIconEffect;
            iconEffectColor = newIconEffectColor;
            homeAlignment = newHomeAlignment;
            homeVerticalAlignment = newHomeVerticalAlignment;
            homeColumns = newHomeColumns;
            homePages = newHomePages;
            hidePagination = newHidePagination;
            timePosition = newTimePosition;
            timeEffect = newTimeEffect;
            timeEffectColor = newTimeEffectColor;
            dateEffect = newDateEffect;
            dateEffectColor = newDateEffectColor;
            dateVerticalPosition = newDateVerticalPosition;
            datePosition = newDatePosition;
            dateHorizontalPosition = newDateHorizontalPosition;
            timeHorizontalPosition = newTimeHorizontalPosition;
            timeFontSize = newTimeFontSize;
            timeColor = newTimeColor;
            dateFontSize = newDateFontSize;
            dateColor = newDateColor;
            homePaddingTop = newHomePaddingTop;
            homePaddingBottom = newHomePaddingBottom;
            homePaddingLeft = newHomePaddingLeft;
            homePaddingRight = newHomePaddingRight;
            updatePaddingPx();
            fullMonthName = newFullMonthName;
            if (newMaxApps != maxApps || newHomeColumns != homeColumns) {
                adjustSlotsForMaxAppsChange(maxApps, newMaxApps, newHomePages, newHomeColumns);
            }
            maxApps = newMaxApps;
            showSettingsButton = newShowSettingsButton;
            showSearchButton = newShowSearchButton;
            settingsButtonSize = newSettingsButtonSize;
            settingsButtonColor = newSettingsButtonColor;
            settingsButtonEffect = newSettingsButtonEffect;
            settingsButtonEffectColor = newSettingsButtonEffectColor;
            searchButtonSize = newSearchButtonSize;
            searchButtonColor = newSearchButtonColor;
            searchButtonEffect = newSearchButtonEffect;
            searchButtonEffectColor = newSearchButtonEffectColor;
            clockAppPkg = newClockAppPkg;
            dateAppPkg = newDateAppPkg;
            showIcons = newShowIcons;
            showAppNames = newShowAppNames;
            appNamePosition = newAppNamePosition;
            monochromeIcons = newMonochromeIcons;
            dynamicIcons = newDynamicIcons;
            dynamicColors = newDynamicColors;
            invertIconColors = newInvertIconColors;
            invertHomeColors = newInvertHomeColors;
            iconBackground = newIconBackground;
            iconShape = newIconShape;

            if (layoutChanged && !onlyAlignmentChanged) {
                recreateLayout();
            }

            if (themeChanged || wallpaperChanged || layoutChanged) {
                updateTheme();
            }

            if (textChanged) {
                updateTextStyles();
            }
        }

        TextView pageIndicator = findViewById(R.id.page_indicator);
        pageIndicator.setVisibility((homePages > 1 && !hidePagination) ? View.VISIBLE : View.GONE);
        homePagesManager.updatePageIndicator();

        LinearLayout bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.setVisibility((homePages > 1 && !hidePagination) ? View.VISIBLE : View.GONE);
        updateGravity();

        if (onlyAlignmentChanged) {
            homeAlignment = newHomeAlignment;
            homeVerticalAlignment = newHomeVerticalAlignment;
            monochromeIcons = newMonochromeIcons;
            dynamicIcons = newDynamicIcons;
            dynamicColors = newDynamicColors;
            updateGravity();
        }

        if (visibilityChanged && !layoutChanged) {
            showAppNames = newShowAppNames;
            updateVisibility();
        }

        updateGravity();

        loadWallpaper();

        if (timeView != null && timeSdf != null) {
            timeView.setText(timeSdf.format(new Date()));
        }
        if (dateView != null && dateSdf != null) {
            dateView.setText(dateSdf.format(new Date()));
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

    private void updateTheme() {
        int bgColor = ThemeUtils.getBgColor(theme, this);
        this.textColor = ThemeUtils.getTextColor(theme, this);
        boolean hasWallpaper = WallpaperHelper.hasWallpaper(this);

        if (hasWallpaper) {
            rootLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }
        } else {
            rootLayout.setBackgroundColor(bgColor);
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            mainLayout.setPadding(0, 0, 0, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(bgColor);
                getWindow().setNavigationBarColor(bgColor);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                if (!ThemeUtils.isDarkTheme(theme, this)) {
                    controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!ThemeUtils.isDarkTheme(theme, this)) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(0);
            }
        }
        int timeDateBgColor = hasWallpaper ? android.graphics.Color.TRANSPARENT : bgColor;
        if (timeView != null) {
            timeView.setBackgroundColor(timeDateBgColor);
            timeView.setTextColor(getTimeColorValue());
            applyTextEffect(timeView, timeEffect, getTimeEffectColorValue());
        }
        if (dateView != null) {
            dateView.setBackgroundColor(timeDateBgColor);
            dateView.setTextColor(getDateColorValue());
            applyTextEffect(dateView, dateEffect, getDateEffectColorValue());
        }
        int slotBgColor = hasWallpaper ? android.graphics.Color.TRANSPARENT : bgColor;
        for (int i = 0; i < appSlots.length; i++) {
            LinearLayout slot = appSlots[i];
            if (slot != null) {
                slot.setBackgroundColor(slotBgColor);
                TextView tv = getSlotTextView(slot);
                if (tv != null) {
                    tv.setTextColor(getAppTextColorValue());
                    applyTextEffect(tv);
                }
                ImageView iconView = getSlotImageView(slot);
                if (iconView != null) {
                    String pkg = appPackages.get(i);
                    boolean isSpecial = "launcher_settings".equals(pkg) || "app_launcher".equals(pkg)
                            || "notification_panel".equals(pkg) || "koreader_history".equals(pkg)
                            || (pkg != null && pkg.startsWith("folder_"))
                            || (pkg != null && pkg.startsWith("webapp_"));
                    iconView.setTag(isSpecial ? "special" : "app");

                    if (isSpecial) {
                        if (dynamicIcons || iconBackground) {
                            int drawableRes = "launcher_settings".equals(pkg) ? R.drawable.settings
                                    : "app_launcher".equals(pkg) ? R.drawable.search
                                            : "notification_panel".equals(pkg) ? R.drawable.notifications
                                                    : "koreader_history".equals(pkg) ? R.drawable.koreader
                                                            : (pkg != null && pkg.startsWith("webapp_"))
                                                                    ? R.drawable.webapps
                                                                    : R.drawable.folder;
                            Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, drawableRes, theme,
                                    iconBackground, dynamicColors, invertIconColors, iconShape);
                            iconView.setImageDrawable(specialIcon);
                            iconView.clearColorFilter();
                        } else {
                            iconView.setColorFilter(getSpecialIconColor());
                        }
                    } else {

                        try {
                            Drawable drawable = DynamicIconHelper.getAppIcon(this, pkg, dynamicIcons, theme,
                                    iconBackground, dynamicColors, invertIconColors, iconShape);
                            iconView.setImageDrawable(drawable);
                        } catch (Exception e) {
                            // Keep current icon if error
                        }

                        if (monochromeIcons && !dynamicIcons) {
                            iconView.setColorFilter(IconMonochromeHelper.getMonochromeFilter());
                        } else {
                            iconView.clearColorFilter();
                        }
                    }
                    applyIconEffect(iconView);
                }
            }
        }
        mainLayout.setBackgroundColor(bgColor);
        if (settingsButton != null) {
            settingsButton.setColorFilter(getSettingsButtonColorValue());
            applyIconEffect(settingsButton, settingsButtonEffect, getSettingsButtonEffectColorValue());
        }
        if (searchButton != null) {
            searchButton.setColorFilter(getSearchButtonColorValue());
            applyIconEffect(searchButton, searchButtonEffect, getSearchButtonEffectColorValue());
        }

        ImageView prevButton = findViewById(R.id.prev_page_button);
        if (prevButton != null) {
            prevButton.setColorFilter(getPaginationColorValue());
        }
        ImageView nextButton = findViewById(R.id.next_page_button);
        if (nextButton != null) {
            nextButton.setColorFilter(getPaginationColorValue());
        }

        TextView pageIndicator = findViewById(R.id.page_indicator);
        if (pageIndicator != null) {
            pageIndicator.setTextColor(getPaginationColorValue());
        }
    }

    private void updateTextStyles() {
        if (timeView != null) {
            timeView.setTextSize(timeFontSize);
            timeView.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
            applyTextEffect(timeView, timeEffect, getTimeEffectColorValue());
        }
        if (dateView != null) {
            dateView.setTextSize(dateFontSize);
            dateView.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
            applyTextEffect(dateView, dateEffect, getDateEffectColorValue());
        }
        for (LinearLayout slot : appSlots) {
            if (slot != null) {
                TextView tv = getSlotTextView(slot);
                if (tv != null) {
                    tv.setTextSize(textSize);
                    tv.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
                    applyTextEffect(tv);
                }
                ImageView iv = getSlotImageView(slot);
                if (iv != null) {
                    int iconSizePx = (int) (iconSize * getResources().getDisplayMetrics().scaledDensity);
                    iv.getLayoutParams().width = iconSizePx;
                    iv.getLayoutParams().height = iconSizePx;
                    iv.requestLayout();

                    applyIconEffect(iv);
                }
            }
        }
        if (settingsButton != null) {
            applyIconEffect(settingsButton, settingsButtonEffect, getSettingsButtonEffectColorValue());
            int sizePx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                    settingsButtonSize, getResources().getDisplayMetrics());
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) settingsButton.getLayoutParams();
            params.width = sizePx;
            params.height = sizePx;
            settingsButton.setLayoutParams(params);

            if (timeView != null && timeHorizontalPosition == 2) {
                int maxBtnSize = Math.max(settingsButtonSize, showSearchButton == 1 ? searchButtonSize : 0);
                int buttonSizePx = (int) android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                RelativeLayout.LayoutParams timeParams = (RelativeLayout.LayoutParams) timeView.getLayoutParams();
                timeParams.rightMargin = buttonSizePx + 16;
            }
        }
        if (searchButton != null) {
            applyIconEffect(searchButton, searchButtonEffect, getSearchButtonEffectColorValue());
        }
        if (settingsButton != null) {
            int sizePx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                    settingsButtonSize, getResources().getDisplayMetrics());
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) settingsButton.getLayoutParams();
            params.width = sizePx;
            params.height = sizePx;
            settingsButton.setLayoutParams(params);

            if (timeView != null && timeHorizontalPosition == 2) {
                int maxBtnSize = Math.max(settingsButtonSize, showSearchButton == 1 ? searchButtonSize : 0);
                int marginSizePx = (int) android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                RelativeLayout.LayoutParams timeParams = (RelativeLayout.LayoutParams) timeView.getLayoutParams();
                timeParams.rightMargin = marginSizePx + 16;
                timeView.setLayoutParams(timeParams);
            }
            if (dateView != null && dateHorizontalPosition == 2) {
                int maxBtnSize = Math.max(settingsButtonSize, showSearchButton == 1 ? searchButtonSize : 0);
                int marginSizePx = (int) android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                RelativeLayout.LayoutParams dateParams = (RelativeLayout.LayoutParams) dateView.getLayoutParams();
                dateParams.rightMargin = marginSizePx + 16;
                dateView.setLayoutParams(dateParams);
            }
        }
        if (searchButton != null) {
            int sizePx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                    searchButtonSize, getResources().getDisplayMetrics());
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) searchButton.getLayoutParams();
            params.width = sizePx;
            params.height = sizePx;
            searchButton.setLayoutParams(params);
            if (showSettingsButton == 0) {

                if (timeView != null && timeHorizontalPosition == 2) {
                    RelativeLayout.LayoutParams timeParams = (RelativeLayout.LayoutParams) timeView.getLayoutParams();
                    timeParams.rightMargin = sizePx + 16;
                    timeView.setLayoutParams(timeParams);
                }
                if (dateView != null && dateHorizontalPosition == 2) {
                    RelativeLayout.LayoutParams dateParams = (RelativeLayout.LayoutParams) dateView.getLayoutParams();
                    dateParams.rightMargin = sizePx + 16;
                    dateView.setLayoutParams(dateParams);
                }
            }
        }
    }

    private void updateVisibility() {
        for (LinearLayout slot : appSlots) {
            if (slot != null) {
                TextView tv = getSlotTextView(slot);
                if (tv != null)
                    tv.setVisibility(showAppNames ? View.VISIBLE : View.GONE);
                if (showAppNames) {
                    slot.setPadding(0, 0, 0, 0);
                    slot.setGravity(Gravity.CENTER_VERTICAL);
                } else {
                    int topBottom = homeColumns == 1 ? 20 : 16;

                    if (homeAlignment == 1) {
                        int symmetricPadding = homeColumns == 1 ? 20 : 12;
                        slot.setPadding(symmetricPadding, topBottom, symmetricPadding, topBottom);
                    } else {
                        int leftPadding = homeColumns == 1 ? (showIcons ? 16 : 32) : (showIcons ? 8 : 16);
                        slot.setPadding(leftPadding, topBottom, 32, topBottom);
                    }
                    slot.setGravity(Gravity.CENTER);
                }
            }
        }
    }

    private void updatePaddingPx() {
        homePaddingLeftPx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                homePaddingLeft, getResources().getDisplayMetrics());
        homePaddingRightPx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                homePaddingRight, getResources().getDisplayMetrics());
        homePaddingTopPx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                homePaddingTop, getResources().getDisplayMetrics());
        homePaddingBottomPx = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP,
                homePaddingBottom, getResources().getDisplayMetrics());
    }

    private void updateGravity() {
        mainLayout.setGravity(getHorizontalGravity(homeAlignment) | getVerticalGravity(homeVerticalAlignment));
        for (int i = 0; i < mainLayout.getChildCount(); i++) {
            View child = mainLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                ((LinearLayout) child).setGravity(getHorizontalGravity(homeAlignment) | Gravity.CENTER_VERTICAL);
            }
        }
        applyWindowInsetsToUI(statusBarInset, navBarInset);
    }

    private void recreateLayout() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        homePagesManager = new HomePagesManager(this, prefs, homePages, homeColumns, maxApps);
        TextView pageIndicator = findViewById(R.id.page_indicator);
        pageIndicator.setTextColor(getPaginationColorValue());
        homePagesManager.setPageIndicator(pageIndicator);
        homePagesManager.updatePageIndicator();
        homePagesManager.loadAppsForCurrentPage();
        appLabels.clear();
        appPackages.clear();
        appLabels.addAll(homePagesManager.getAppLabels());
        appPackages.addAll(homePagesManager.getAppPackages());
        if (timeView != null) {
            rootLayout.removeView(timeView);
            timeView = null;
        }
        if (dateView != null) {
            rootLayout.removeView(dateView);
            dateView = null;
        }
        if (settingsButton != null) {
            rootLayout.removeView(settingsButton);
            settingsButton = null;
        }
        if (searchButton != null) {
            rootLayout.removeView(searchButton);
            searchButton = null;
        }
        mainLayout.removeAllViews();
        int totalApps = homeColumns * maxApps;
        appSlots = new LinearLayout[totalApps];
        int bgColor = ThemeUtils.getBgColor(theme, this);
        this.textColor = ThemeUtils.getTextColor(theme, this);
        if (timePosition == 1 || datePosition != 0) {
            createTimeViews(bgColor, textColor);
        }
        createHomeLayout();
        createSettingsButton(bgColor, textColor);
        createSearchButton(bgColor, textColor);
        adjustMainLayoutPosition();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(homeButtonReceiver);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void showAppSelector(int position) {
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.putExtra(AppSelectorActivity.EXTRA_POSITION, position);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, position);
    }

    public void launchApp(String packageName) {
        if ("notification_panel".equals(packageName)) {
            try {
                Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel")
                        .invoke(getSystemService("statusbar"));
            } catch (Exception e) {
                // Log or ignore
            }
        } else if ("app_launcher".equals(packageName)) {
            Intent intent = new Intent(MainActivity.this, AppLauncherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        } else if ("koreader_history".equals(packageName)) {
            Intent intent = new Intent(MainActivity.this,
                    org.matiasdesu.thinklauncherv2.ui.KOReaderHistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        } else if ("launcher_settings".equals(packageName)) {
            try {
                Class<?> clazz = Class.forName("org.matiasdesu.thinklauncherv2.settings.SettingsActivity");
                Intent intent = new Intent(MainActivity.this, clazz);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        } else if ("next_home_page".equals(packageName)) {
            int current = homePagesManager.getCurrentPage();
            int nextPage = (current + 1) % homePages;
            homePagesManager.setCurrentPage(nextPage);
            recreateHome();
        } else if ("previous_home_page".equals(packageName)) {
            int current = homePagesManager.getCurrentPage();
            int prevPage = (current - 1 + homePages) % homePages;
            homePagesManager.setCurrentPage(prevPage);
            recreateHome();
        } else if ("blank".equals(packageName)) {
            // Do nothing for blank
        } else if (packageName != null && packageName.startsWith("webapp_")) {

            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            String url = prefs.getString(packageName + "_url", "");
            if (!url.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(url));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    // Log or ignore if no browser available
                }
            }
        } else if (packageName != null && packageName.startsWith("folder_")) {

            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            String folderName = prefs.getString(packageName + "_name", "Folder");
            Intent intent = new Intent(MainActivity.this, org.matiasdesu.thinklauncherv2.ui.FolderActivity.class);
            intent.putExtra(org.matiasdesu.thinklauncherv2.ui.FolderActivity.EXTRA_FOLDER_ID, packageName);
            intent.putExtra(org.matiasdesu.thinklauncherv2.ui.FolderActivity.EXTRA_FOLDER_NAME, folderName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityForResult(intent, 9999);
        } else if (!packageName.isEmpty()) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    public void updateSlot(int position) {
        LinearLayout slot = appSlots[position];
        TextView tv = getSlotTextView(slot);
        if (tv == null)
            return;
        tv.setText(appLabels.get(position));
        tv.setTextColor(getAppTextColorValue());
        tv.setTextSize(textSize);
        tv.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
        tv.setGravity(getHorizontalGravity(homeAlignment));
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        applyTextEffect(tv);
        tv.setVisibility(showAppNames ? View.VISIBLE : View.GONE);
        if (appPackages.get(position).equals("blank") || appPackages.get(position).isEmpty()) {
            tv.setText(appPackages.get(position).equals("blank") ? "" : appLabels.get(position));
            tv.setVisibility(appPackages.get(position).equals("blank") ? View.GONE : View.VISIBLE);
        }

        slot.setClipChildren(false);
        slot.setClipToPadding(false);
        if (showAppNames && (appNamePosition == AppNamePositionHelper.POSITION_TOP
                || appNamePosition == AppNamePositionHelper.POSITION_BOTTOM)) {
            slot.setOrientation(LinearLayout.VERTICAL);
            slot.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            slot.setOrientation(LinearLayout.HORIZONTAL);
            slot.setGravity(Gravity.CENTER_VERTICAL);
        }

        if (appNamePosition == AppNamePositionHelper.POSITION_LEFT && showIcons && showAppNames) {
            slot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            slot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        int vPadding = homeColumns == 1 ? 20 : 16;
        if (showAppNames) {
            slot.setPadding(0, 0, 0, 0);
            int leftOuterPadding = homeColumns == 1 ? (showIcons ? 16 : 32) : (showIcons ? 8 : 16);
            if (appNamePosition == AppNamePositionHelper.POSITION_TOP
                    || appNamePosition == AppNamePositionHelper.POSITION_BOTTOM) {
                tv.setPadding(8, appNamePosition == AppNamePositionHelper.POSITION_TOP ? vPadding : 8, 8,
                        appNamePosition == AppNamePositionHelper.POSITION_BOTTOM ? vPadding : 8);
            } else {
                tv.setPadding(leftOuterPadding, vPadding, 32, vPadding);
            }
        } else {

            if (homeAlignment == 1) {
                int symmetricPadding = homeColumns == 1 ? 20 : 12;
                slot.setPadding(symmetricPadding, vPadding, symmetricPadding, vPadding);
            } else {
                int leftPadding = homeColumns == 1 ? (showIcons ? 16 : 32) : (showIcons ? 8 : 16);
                slot.setPadding(leftPadding, vPadding, 32, vPadding);
            }
        }

        boolean shouldHaveIcon = showIcons || appPackages.get(position).equals("blank");
        boolean isBlankOrEmpty = appPackages.get(position).isEmpty() || appPackages.get(position).equals("blank");
        ImageView existingIcon = getSlotImageView(slot);
        boolean hasIcon = existingIcon != null;

        if (shouldHaveIcon) {
            if (!hasIcon) {

                ImageView iconView = new ImageView(this);
                int iconSizePx = (int) (iconSize * getResources().getDisplayMetrics().scaledDensity);
                iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSizePx, iconSizePx));

                if (isBlankOrEmpty) {
                    iconView.setVisibility(View.INVISIBLE);
                    iconView.setImageDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    if (showAppNames) {
                        iconView.setPadding(0, 0, 16, 0);
                    }
                    int addIndex = (appNamePosition == AppNamePositionHelper.POSITION_TOP
                            || appNamePosition == AppNamePositionHelper.POSITION_LEFT) ? 1 : 0;
                    slot.addView(iconView, Math.min(addIndex, slot.getChildCount()));
                    return;
                }
                if ("launcher_settings".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.settings, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.settings);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if ("app_launcher".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.search, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.search);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if ("notification_panel".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.notifications,
                                theme, iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.notifications);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if ("koreader_history".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.koreader,
                                theme, iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.koreader);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if (appPackages.get(position).startsWith("folder_")) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.folder, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.folder);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if (appPackages.get(position).startsWith("webapp_")) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.webapps, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.webapps);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else {
                    try {
                        Drawable drawable = DynamicIconHelper.getAppIcon(this, appPackages.get(position), dynamicIcons,
                                theme, iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(drawable);
                        if (monochromeIcons && !dynamicIcons) {
                            iconView.setColorFilter(IconMonochromeHelper.getMonochromeFilter());
                        } else {
                            iconView.clearColorFilter();
                        }
                    } catch (Exception e) {

                        return;
                    }
                }
                int addIndex = (appNamePosition == AppNamePositionHelper.POSITION_TOP
                        || appNamePosition == AppNamePositionHelper.POSITION_LEFT) ? 1 : 0;
                slot.addView(iconView, Math.min(addIndex, slot.getChildCount()));
                applyIconEffect(iconView);
            } else {

                ImageView iconView = existingIcon;

                if (isBlankOrEmpty) {
                    iconView.setVisibility(View.INVISIBLE);
                    iconView.setImageDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    if (showAppNames) {
                        iconView.setPadding(0, 0, 16, 0);
                    } else {
                        iconView.setPadding(0, 0, 0, 0);
                    }
                    return;
                }

                iconView.setVisibility(View.VISIBLE);

                if ("launcher_settings".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.settings, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.settings);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if ("app_launcher".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.search, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.search);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if ("notification_panel".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.notifications,
                                theme, iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.notifications);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if ("koreader_history".equals(appPackages.get(position))) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.koreader,
                                theme, iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.koreader);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if (appPackages.get(position).startsWith("folder_")) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.folder, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.folder);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else if (appPackages.get(position).startsWith("webapp_")) {
                    if (dynamicIcons || iconBackground) {
                        Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.webapps, theme,
                                iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(specialIcon);
                        iconView.clearColorFilter();
                    } else {
                        iconView.setImageResource(R.drawable.webapps);
                        iconView.setColorFilter(getSpecialIconColor());
                    }
                } else {
                    try {
                        Drawable drawable = DynamicIconHelper.getAppIcon(this, appPackages.get(position), dynamicIcons,
                                theme, iconBackground, dynamicColors, invertIconColors, iconShape);
                        iconView.setImageDrawable(drawable);
                        if (monochromeIcons && !dynamicIcons) {
                            iconView.setColorFilter(IconMonochromeHelper.getMonochromeFilter());
                        } else {
                            iconView.clearColorFilter();
                        }
                    } catch (Exception e) {
                        // Icon not found, skip
                    }
                }

                applyIconEffect(iconView);
            }
        } else if (!shouldHaveIcon && hasIcon) {

            slot.removeView(existingIcon);
        }

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit().putString("slot_label_" + position, appLabels.get(position))
                .putString("slot_pkg_" + position, appPackages.get(position)).apply();
    }

    private int getVerticalGravity(int vertical) {
        switch (vertical) {
            case 0:
                return Gravity.TOP;
            case 1:
                return Gravity.CENTER;
            case 2:
                return Gravity.BOTTOM;
            default:
                return Gravity.CENTER;
        }
    }

    private int getHorizontalGravity(int horizontal) {
        switch (horizontal) {
            case 0:
                return Gravity.LEFT;
            case 1:
                return Gravity.CENTER_HORIZONTAL;
            case 2:
                return Gravity.RIGHT;
            default:
                return Gravity.CENTER_HORIZONTAL;
        }
    }

    private int getRelativeHorizontalRule(int horizontal) {
        switch (horizontal) {
            case 0:
                return RelativeLayout.ALIGN_PARENT_LEFT;
            case 1:
                return RelativeLayout.CENTER_HORIZONTAL;
            case 2:
                return RelativeLayout.ALIGN_PARENT_RIGHT;
            default:
                return RelativeLayout.CENTER_HORIZONTAL;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {

            if (requestCode == 9999) {
                String folderId = data.getStringExtra(org.matiasdesu.thinklauncherv2.ui.FolderActivity.EXTRA_FOLDER_ID);
                String updatedName = data
                        .getStringExtra(org.matiasdesu.thinklauncherv2.ui.FolderActivity.EXTRA_UPDATED_FOLDER_NAME);

                if (folderId != null && updatedName != null) {

                    for (int i = 0; i < appPackages.size(); i++) {
                        if (folderId.equals(appPackages.get(i))) {
                            appLabels.set(i, updatedName);
                            homePagesManager.setAppLabel(i, updatedName);
                            updateSlot(i);
                            break;
                        }
                    }
                }
                return;
            }

            String label = data.getStringExtra(AppSelectorActivity.EXTRA_LABEL);
            String pkg = data.getStringExtra(AppSelectorActivity.EXTRA_PACKAGE);
            int position = data.getIntExtra(AppSelectorActivity.EXTRA_POSITION, -1);

            if (position >= 0 && position < appLabels.size()) {

                if (pkg != null && pkg.startsWith("folder_")) {
                    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                    prefs.edit().putString(pkg + "_name", label).apply();
                }

                appLabels.set(position, label);
                appPackages.set(position, pkg);
                homePagesManager.setAppLabel(position, label);
                homePagesManager.setAppPackage(position, pkg);

                updateSlot(position);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing - disable back button on home
    }

    private class GestureHandler {

        private String leftApp, rightApp, downApp, upApp;
        private GestureDetector gestureDetector;
        private Handler handler = new Handler(Looper.getMainLooper());
        private Runnable openSettingsRunnable;
        private boolean doubleTapDone = false;

        public GestureHandler() {
            loadApps();
            GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return handleFling(e1, e2, velocityX, velocityY);
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (doubleTapLock == 1) {
                        if (LockAccessibilityService.lockScreen()) {
                            doubleTapDone = true;

                            if (openSettingsRunnable != null) {
                                handler.removeCallbacks(openSettingsRunnable);
                                openSettingsRunnable = null;
                            }
                            return true;
                        } else {
                            Toast.makeText(MainActivity.this, "Please enable accessibility to use double tap to lock",
                                    Toast.LENGTH_SHORT).show();
                            doubleTapDone = false;
                            return false;
                        }
                    }
                    doubleTapDone = false;
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    if (doubleTapDone) {
                        doubleTapDone = false;
                        return;
                    }

                    try {
                        Class<?> clazz = Class.forName("org.matiasdesu.thinklauncherv2.settings.SettingsActivity");
                        Intent intent = new Intent(MainActivity.this, clazz);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }
                }
            };
            gestureDetector = new GestureDetector(getApplicationContext(), gestureListener);
        }

        public void loadApps() {
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            leftApp = prefs.getString("swipe_left_app", "");
            rightApp = prefs.getString("swipe_right_app", "");
            downApp = prefs.getString("swipe_down_app", "");
            upApp = prefs.getString("swipe_up_app", "");
        }

        public boolean onTouch(MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private boolean isPointInsideView(float x, float y, View view) {
            Rect rect = new Rect();
            view.getGlobalVisibleRect(rect);
            LinearLayout mainLayout = MainActivity.this.mainLayout;
            int[] mainLocation = new int[2];
            mainLayout.getLocationOnScreen(mainLocation);
            rect.offset(-mainLocation[0], -mainLocation[1]);
            return rect.contains((int) x, (int) y);
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void recreateHome() {
        mainLayout.removeAllViews();
        appLabels.clear();
        appPackages.clear();
        homePagesManager.loadAppsForCurrentPage();
        appLabels.addAll(homePagesManager.getAppLabels());
        appPackages.addAll(homePagesManager.getAppPackages());

        createHomeLayout();
        EinkRefreshHelper.refreshEink(getWindow(), getSharedPreferences("prefs", MODE_PRIVATE),
                getSharedPreferences("prefs", MODE_PRIVATE).getInt("eink_refresh_delay", 100));
    }

    private void loadWallpaper() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        int bgColor = ThemeUtils.getBgColor(theme, this);

        if (WallpaperHelper.hasWallpaper(this)) {
            int[] screenDimensions = WallpaperHelper.getScreenDimensions(this);
            Bitmap wallpaper = WallpaperHelper.getWallpaperForScreen(this, screenDimensions[0], screenDimensions[1]);
            if (wallpaper != null) {
                wallpaperView.setImageBitmap(wallpaper);
                wallpaperView.setVisibility(View.VISIBLE);
                mainLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                rootLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);

                WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                    getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
                }

                rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
                    int statusBarHeight = 0;
                    int navBarHeight = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        statusBarHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
                        navBarHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    } else {
                        statusBarHeight = insets.getSystemWindowInsetTop();
                        navBarHeight = insets.getSystemWindowInsetBottom();
                    }
                    statusBarInset = statusBarHeight;
                    navBarInset = navBarHeight;

                    applyWindowInsetsToUI(statusBarHeight, navBarHeight);

                    return insets;
                });
                rootLayout.requestApplyInsets();
            }
        } else {
            wallpaperView.setVisibility(View.GONE);
            mainLayout.setBackgroundColor(bgColor);
            rootLayout.setBackgroundColor(bgColor);
            statusBarInset = 0;
            navBarInset = 0;
            updateGravity();

            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(bgColor);
                getWindow().setNavigationBarColor(bgColor);
            }

            rootLayout.setOnApplyWindowInsetsListener(null);
            applyWindowInsetsToUI(0, 0);
        }
    }

    private void applyWindowInsetsToUI(int topInset, int bottomInset) {
        if (dateView != null && dateView.getParent() == rootLayout) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) dateView.getLayoutParams();
            boolean isTimeVisible = (timePosition == 1 && timeView != null && timeView.getParent() == rootLayout);
            if (dateVerticalPosition == 0 || !isTimeVisible) {
                params.topMargin = topInset + homePaddingTopPx;
            }
            if (dateHorizontalPosition == 0) {
                params.leftMargin = homePaddingLeftPx;
            } else if (dateHorizontalPosition == 2) {
                int rightMargin = homePaddingRightPx;
                if (showSettingsButton == 1 || showSearchButton == 1) {
                    int maxBtnSize = Math.max(showSettingsButton == 1 ? settingsButtonSize : 0,
                            showSearchButton == 1 ? searchButtonSize : 0);
                    int buttonSizePx = (int) android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                    rightMargin += buttonSizePx + 16;
                }
                params.rightMargin = rightMargin;
            }
            dateView.setLayoutParams(params);
        }

        if (timeView != null && timeView.getParent() == rootLayout) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) timeView.getLayoutParams();

            if (dateView == null || dateVerticalPosition != 0) {
                params.topMargin = topInset + homePaddingTopPx;
            }
            if (timeHorizontalPosition == 0) {
                params.leftMargin = homePaddingLeftPx;
            } else if (timeHorizontalPosition == 2) {
                int rightMargin = homePaddingRightPx;
                if (showSettingsButton == 1 || showSearchButton == 1) {
                    int maxBtnSize = Math.max(showSettingsButton == 1 ? settingsButtonSize : 0,
                            showSearchButton == 1 ? searchButtonSize : 0);
                    int buttonSizePx = (int) android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP, maxBtnSize, getResources().getDisplayMetrics());
                    rightMargin += buttonSizePx + 16;
                }
                params.rightMargin = rightMargin;
            }
            timeView.setLayoutParams(params);
        }

        if (settingsButton != null && settingsButton.getParent() == rootLayout) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) settingsButton.getLayoutParams();
            params.topMargin = topInset + homePaddingTopPx + 5;
            params.rightMargin = homePaddingRightPx + 16;
            settingsButton.setLayoutParams(params);
        }

        if (searchButton != null && searchButton.getParent() == rootLayout && showSettingsButton != 1) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) searchButton.getLayoutParams();
            params.topMargin = topInset + homePaddingTopPx + 5;
            params.rightMargin = homePaddingRightPx + 16;
            searchButton.setLayoutParams(params);
        } else if (searchButton != null && searchButton.getParent() == rootLayout) {

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) searchButton.getLayoutParams();
            params.rightMargin = homePaddingRightPx + 16;
            searchButton.setLayoutParams(params);
        }

        if (mainLayout != null) {
            boolean hasTopAnchor = (timePosition == 1 && timeView != null) ||
                    (datePosition != 0 && dateView != null) ||
                    (showSettingsButton == 1 && settingsButton != null) ||
                    (showSearchButton == 1 && searchButton != null);

            int effectiveTopPx = 0;
            if (!hasTopAnchor) {
                effectiveTopPx = homePaddingTopPx;
                if (homeVerticalAlignment == 0 && topInset > 0) {
                    effectiveTopPx += topInset;
                }
            }

            LinearLayout bottomBar = findViewById(R.id.bottom_bar);
            int bottomBarHeight = (bottomBar.getVisibility() == View.VISIBLE && homeVerticalAlignment == 2)
                    ? (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 48,
                            getResources().getDisplayMetrics())
                    : 0;
            int effectiveBottomPx = homePaddingBottomPx + bottomBarHeight + bottomInset;

            if (mainLayout.getPaddingLeft() != homePaddingLeftPx ||
                    mainLayout.getPaddingTop() != effectiveTopPx ||
                    mainLayout.getPaddingRight() != homePaddingRightPx ||
                    mainLayout.getPaddingBottom() != effectiveBottomPx) {
                mainLayout.setPadding(homePaddingLeftPx, effectiveTopPx, homePaddingRightPx, effectiveBottomPx);
            }
        }
    }

    private void createHomeLayout() {

        if (mainLayout != null) {
            mainLayout.setClipChildren(false);
            mainLayout.setClipToPadding(false);
        }

        if (homeColumns > 1) {
            mainLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < homeColumns; col++) {
                LinearLayout columnLayout = new LinearLayout(this);
                columnLayout.setOrientation(LinearLayout.VERTICAL);
                columnLayout.setGravity(getHorizontalGravity(homeAlignment) | Gravity.CENTER_VERTICAL);
                columnLayout
                        .setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                columnLayout.setClipChildren(false);
                columnLayout.setClipToPadding(false);
                for (int i = 0; i < maxApps; i++) {
                    int index = col * maxApps + i;
                    createAppSlot(columnLayout, index);
                }
                mainLayout.addView(columnLayout);
            }
        } else {
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            if (mainLayout != null) {
                mainLayout.setClipChildren(false);
                mainLayout.setClipToPadding(false);
            }
            for (int i = 0; i < maxApps; i++) {
                createAppSlot(mainLayout, i);
            }
        }
        updateGravity();
    }

    private void createAppSlot(LinearLayout parent, int index) {
        LinearLayout slotLayout = new LinearLayout(this);
        slotLayout.setClipChildren(false);
        slotLayout.setClipToPadding(false);

        if (showAppNames && (appNamePosition == AppNamePositionHelper.POSITION_TOP
                || appNamePosition == AppNamePositionHelper.POSITION_BOTTOM)) {
            slotLayout.setOrientation(LinearLayout.VERTICAL);
            slotLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            slotLayout.setOrientation(LinearLayout.HORIZONTAL);
            slotLayout.setGravity(Gravity.CENTER_VERTICAL);
        }

        if (appNamePosition == AppNamePositionHelper.POSITION_LEFT && showIcons && showAppNames) {
            slotLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            slotLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        TextView tv = new StrokeTextView(this);
        tv.setText(appLabels.get(index));
        tv.setTextColor(getAppTextColorValue());
        tv.setTextSize(textSize);
        tv.setTypeface(null, boldText ? Typeface.BOLD : Typeface.NORMAL);
        applyTextEffect(tv);
        tv.setBackgroundColor(0);

        if (appNamePosition == AppNamePositionHelper.POSITION_LEFT && showIcons && showAppNames) {
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(tvParams);
        } else {
            tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        tv.setGravity(getHorizontalGravity(homeAlignment));
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setVisibility(showAppNames ? View.VISIBLE : View.GONE);
        if (appPackages.get(index).equals("blank") || appPackages.get(index).isEmpty()) {
            tv.setText(appPackages.get(index).equals("blank") ? "" : appLabels.get(index));
            tv.setVisibility(appPackages.get(index).equals("blank") ? View.GONE : View.VISIBLE);
        }

        int vPadding = homeColumns == 1 ? 20 : 16;
        int leftOuterPadding = homeColumns == 1 ? (showIcons ? 16 : 32) : (showIcons ? 8 : 16);
        if (appNamePosition == AppNamePositionHelper.POSITION_TOP
                || appNamePosition == AppNamePositionHelper.POSITION_BOTTOM) {
            tv.setPadding(8, appNamePosition == AppNamePositionHelper.POSITION_TOP ? vPadding : 8, 8,
                    appNamePosition == AppNamePositionHelper.POSITION_BOTTOM ? vPadding : 8);
        } else {
            tv.setPadding(leftOuterPadding, vPadding, 32, vPadding);
        }

        if (appNamePosition == AppNamePositionHelper.POSITION_TOP) {
            slotLayout.addView(tv);
        } else if (appNamePosition == AppNamePositionHelper.POSITION_LEFT) {
            slotLayout.addView(tv);
        }

        if (showIcons || appPackages.get(index).equals("blank")) {
            ImageView iconView = new ImageView(this);
            String pkg = appPackages.get(index);
            boolean isSpecial = "launcher_settings".equals(pkg) || "app_launcher".equals(pkg)
                    || "notification_panel".equals(pkg) || "koreader_history".equals(pkg)
                    || (pkg != null && pkg.startsWith("folder_"))
                    || (pkg != null && pkg.startsWith("webapp_"));
            iconView.setTag(isSpecial ? "special" : "app");

            int iconSizePx = (int) (iconSize * getResources().getDisplayMetrics().scaledDensity);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSizePx, iconSizePx));

            int iconPaddingLeft = 0, iconPaddingRight = 0, iconPaddingTop = 0, iconPaddingBottom = 0;
            if (showAppNames) {
                if (appNamePosition == AppNamePositionHelper.POSITION_RIGHT) {
                    iconPaddingRight = 16;
                } else if (appNamePosition == AppNamePositionHelper.POSITION_LEFT) {
                    iconPaddingLeft = 16;
                } else if (appNamePosition == AppNamePositionHelper.POSITION_TOP) {
                    iconPaddingTop = 8;
                } else if (appNamePosition == AppNamePositionHelper.POSITION_BOTTOM) {
                    iconPaddingBottom = 8;
                }
            }

            if (appPackages.get(index).isEmpty() || appPackages.get(index).equals("blank")) {
                iconView.setImageDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                iconView.setVisibility(View.INVISIBLE);
                iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                slotLayout.addView(iconView);
            } else if ("launcher_settings".equals(appPackages.get(index))) {
                if (dynamicIcons || iconBackground) {
                    Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.settings, theme,
                            iconBackground, dynamicColors, invertIconColors, iconShape);
                    iconView.setImageDrawable(specialIcon);
                    iconView.clearColorFilter();
                } else {
                    iconView.setImageResource(R.drawable.settings);
                    iconView.setColorFilter(getSpecialIconColor());
                }
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                slotLayout.addView(iconView);
            } else if ("app_launcher".equals(appPackages.get(index))) {
                if (dynamicIcons || iconBackground) {
                    Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.search, theme,
                            iconBackground, dynamicColors, invertIconColors, iconShape);
                    iconView.setImageDrawable(specialIcon);
                    iconView.clearColorFilter();
                } else {
                    iconView.setImageResource(R.drawable.search);
                    iconView.setColorFilter(getSpecialIconColor());
                }
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                slotLayout.addView(iconView);
            } else if ("koreader_history".equals(appPackages.get(index))) {
                if (dynamicIcons || iconBackground) {
                    Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.koreader, theme,
                            iconBackground, dynamicColors, invertIconColors, iconShape);
                    iconView.setImageDrawable(specialIcon);
                    iconView.clearColorFilter();
                } else {
                    iconView.setImageResource(R.drawable.koreader);
                    iconView.setColorFilter(getSpecialIconColor());
                }
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                slotLayout.addView(iconView);
            } else if ("notification_panel".equals(appPackages.get(index))) {
                if (dynamicIcons || iconBackground) {
                    Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.notifications, theme,
                            iconBackground, dynamicColors, invertIconColors, iconShape);
                    iconView.setImageDrawable(specialIcon);
                    iconView.clearColorFilter();
                } else {
                    iconView.setImageResource(R.drawable.notifications);
                    iconView.setColorFilter(getSpecialIconColor());
                }
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                slotLayout.addView(iconView);
            } else if (appPackages.get(index).startsWith("folder_")) {
                if (dynamicIcons || iconBackground) {
                    Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.folder, theme,
                            iconBackground, dynamicColors, invertIconColors, iconShape);
                    iconView.setImageDrawable(specialIcon);
                    iconView.clearColorFilter();
                } else {
                    iconView.setImageResource(R.drawable.folder);
                    iconView.setColorFilter(getSpecialIconColor());
                }
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                slotLayout.addView(iconView);
            } else if (appPackages.get(index).startsWith("webapp_")) {
                if (dynamicIcons || iconBackground) {
                    Drawable specialIcon = DynamicIconHelper.createSpecialIcon(this, R.drawable.webapps, theme,
                            iconBackground, dynamicColors, invertIconColors, iconShape);
                    iconView.setImageDrawable(specialIcon);
                    iconView.clearColorFilter();
                } else {
                    iconView.setImageResource(R.drawable.webapps);
                    iconView.setColorFilter(getSpecialIconColor());
                }
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                slotLayout.addView(iconView);
            } else {
                try {
                    Drawable drawable = DynamicIconHelper.getAppIcon(this, appPackages.get(index), dynamicIcons, theme,
                            iconBackground, dynamicColors, invertIconColors, iconShape);
                    iconView.setImageDrawable(drawable);
                    if (monochromeIcons && !dynamicIcons) {
                        iconView.setColorFilter(IconMonochromeHelper.getMonochromeFilter());
                    } else {
                        iconView.clearColorFilter();
                    }
                    iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    iconView.setPadding(iconPaddingLeft, iconPaddingTop, iconPaddingRight, iconPaddingBottom);
                    slotLayout.addView(iconView);
                } catch (Exception e) {
                    return;
                }
            }
            applyIconEffect(iconView);
        }

        if (appNamePosition == AppNamePositionHelper.POSITION_RIGHT) {
            slotLayout.addView(tv);
        } else if (appNamePosition == AppNamePositionHelper.POSITION_BOTTOM) {
            slotLayout.addView(tv);
        }

        if (!showAppNames) {
            int vPaddingFull = homeColumns == 1 ? 20 : 16;
            if (homeAlignment == 1) {
                int symmetricPadding = homeColumns == 1 ? 20 : 12;
                slotLayout.setPadding(symmetricPadding, vPaddingFull, symmetricPadding, vPaddingFull);
            } else {
                int leftPaddingIconOnly = homeColumns == 1 ? (showIcons ? 16 : 32) : (showIcons ? 8 : 16);
                slotLayout.setPadding(leftPaddingIconOnly, vPaddingFull, 32, vPaddingFull);
            }
        }

        final int pos = index;

        GestureDetector.SimpleOnGestureListener slotGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                String packageName = appPackages.get(pos);
                if (!packageName.isEmpty()) {
                    launchApp(packageName);
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                showAppSelector(pos);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return handleFling(e1, e2, velocityX, velocityY);
            }
        };
        GestureDetector slotGestureDetector = new GestureDetector(this, slotGestureListener);
        slotLayout.setOnTouchListener((v, event) -> slotGestureDetector.onTouchEvent(event));

        parent.addView(slotLayout);
        appSlots[index] = slotLayout;
    }

    private void adjustSlotsForMaxAppsChange(int oldMaxApps, int newMaxApps, int homePages, int homeColumns) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (int page = 0; page < homePages; page++) {
            List<String> newLabels = new ArrayList<>();
            List<String> newPackages = new ArrayList<>();

            for (int col = 0; col < homeColumns; col++) {

                List<String> colLabels = new ArrayList<>();
                List<String> colPackages = new ArrayList<>();
                for (int i = 0; i < oldMaxApps; i++) {
                    int index = col * oldMaxApps + i;
                    String label = prefs.getString("slot_label_page_" + page + "_" + index, "Empty");
                    String pkg = prefs.getString("slot_pkg_page_" + page + "_" + index, "");
                    colLabels.add(label);
                    colPackages.add(pkg);
                }

                if (newMaxApps > oldMaxApps) {

                    int diff = newMaxApps - oldMaxApps;
                    for (int i = 0; i < diff; i++) {
                        colLabels.add("Empty");
                        colPackages.add("");
                    }
                } else if (newMaxApps < oldMaxApps) {

                    int diff = oldMaxApps - newMaxApps;
                    for (int i = 0; i < diff; i++) {
                        colLabels.remove(colLabels.size() - 1);
                        colPackages.remove(colPackages.size() - 1);
                    }
                }

                newLabels.addAll(colLabels);
                newPackages.addAll(colPackages);
            }

            int totalApps = homeColumns * newMaxApps;
            for (int i = 0; i < totalApps; i++) {
                editor.putString("slot_label_page_" + page + "_" + i, newLabels.get(i));
                editor.putString("slot_pkg_page_" + page + "_" + i, newPackages.get(i));
            }
        }

        editor.apply();
    }

    private int getSpecialIconColor() {
        int[] dynamicColorPair = DynamicIconHelper.getDynamicColors(this, theme, iconBackground, invertIconColors,
                dynamicColors);
        return dynamicColorPair[0];
    }

    private TextView getSlotTextView(LinearLayout slot) {
        if (slot == null)
            return null;
        for (int i = 0; i < slot.getChildCount(); i++) {
            if (slot.getChildAt(i) instanceof TextView) {
                return (TextView) slot.getChildAt(i);
            }
        }
        return null;
    }

    private ImageView getSlotImageView(LinearLayout slot) {
        if (slot == null)
            return null;
        for (int i = 0; i < slot.getChildCount(); i++) {
            if (slot.getChildAt(i) instanceof ImageView) {
                return (ImageView) slot.getChildAt(i);
            }
        }
        return null;
    }
}
