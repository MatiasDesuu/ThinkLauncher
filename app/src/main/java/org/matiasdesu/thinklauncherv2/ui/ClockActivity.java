package org.matiasdesu.thinklauncherv2.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.AppListSizeHelper;
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClockActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 5001;

    private int textSize;
    private boolean boldText;
    private List<ClockAlarmHelper.Alarm> alarms;
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private ClockAdapter clockAdapter;
    private SharedPreferences prefs;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private boolean appLauncherAnimations;
    private boolean showWallpaperBackdrop;
    private int clockSurfaceColor;
    private SwipePageNavigator pageNavigator;

    private final BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(ClockActivity.this, MainActivity.class);
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
        opacityEnabled = prefs.getInt("app_launcher_bg_opacity_enabled", 0) == 1;
        appLauncherAnimations = prefs.getInt("screen_animations", 0) == 1;
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock);

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
        clockSurfaceColor = backdrop.surfaceColor;
        showWallpaperBackdrop = backdrop.showWallpaperBackdrop;

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        TextView titleView = findViewById(R.id.clock_title);
        ThemeUtils.applyTextColor(titleView, theme, this);

        titleView.setOnClickListener(v -> openOptionsDialog());
        titleView.setOnLongClickListener(v -> {
            if (scrollAppList) {
                RecyclerView rv = findViewById(R.id.clock_list);
                if (rv != null) rv.scrollToPosition(0);
                EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            }
            return true;
        });

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        ImageView addButton = findViewById(R.id.add_alarm_button);
        addButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        addButton.setOnClickListener(v -> openAddDialog());

        RecyclerView recyclerView = findViewById(R.id.clock_list);
        View topLayout = findViewById(R.id.top_layout);
        View container = findViewById(R.id.app_list_container);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, clockSurfaceColor,
                topLayout, recyclerView, container);

        textSize = prefs.getInt("clock_font_size", 32);

        if (prefs.contains("calendar_font_size") && !prefs.contains("clock_font_size")) {
            textSize = prefs.getInt("calendar_font_size", 32);
        }
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;

        itemsPerPage = calculateClockItemsPerPage();

        recyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return scrollAppList;
            }
        });
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        alarms = new ArrayList<>();
        clockAdapter = new ClockAdapter(alarms, this, theme);
        recyclerView.setAdapter(clockAdapter);

        if (!scrollAppList) {
            pageNavigator = new SwipePageNavigator(this, recyclerView, container,
                    new SwipePageNavigator.PageChangeCallback() {
                        @Override
                        public void onPageChanged(int newPage) {
                            currentPage = newPage;
                            recyclerView.getAdapter().notifyDataSetChanged();
                            updatePageIndicator();
                            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                        }

                        @Override
                        public int getTotalPages() {
                            return (int) Math.ceil((double) alarms.size() / itemsPerPage);
                        }

                        @Override
                        public void updatePageIndicator() {
                            ClockActivity.this.updatePageIndicator();
                        }
                    }, theme);
        } else {
            pageNavigator = null;
        }

        updatePageIndicator();
        loadAlarms();
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {

                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadAlarms() {
        int previousPage = currentPage;
        alarms.clear();
        List<ClockAlarmHelper.Alarm> loaded = ClockAlarmHelper.loadAlarms(this);

        Collections.sort(loaded, (a, b) -> {
            if (a.hour != b.hour) return a.hour - b.hour;
            return a.minute - b.minute;
        });
        alarms.addAll(loaded);

        int totalPages = (int) Math.ceil((double) alarms.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (previousPage >= totalPages) previousPage = totalPages - 1;
        if (previousPage < 0) previousPage = 0;
        currentPage = previousPage;
        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(currentPage);
            pageNavigator.setTotalItems(alarms.size());
        }
        if (clockAdapter != null) clockAdapter.notifyDataSetChanged();
        updatePageIndicator();

    }

    private int calculateClockItemsPerPage() {

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        float scaledDensity = dm.scaledDensity;
        float screenHeightDp = dm.heightPixels / density;

        int navBarHeightPx = 0;
        try {
            navBarHeightPx = getResources().getDimensionPixelSize(
                    getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        } catch (Exception ignored) {}
        screenHeightDp -= navBarHeightPx / density;

        float recyclerHeightDp = screenHeightDp - 48 - 4 - 48;

        android.graphics.Paint timePaint = new android.graphics.Paint();
        timePaint.setTextSize(textSize * scaledDensity);
        android.graphics.Typeface custom = FontHelper.getTypeface(this);
        if (custom != null) timePaint.setTypeface(custom);
        float timeHeightDp = (timePaint.getFontMetrics().bottom - timePaint.getFontMetrics().top) / density;

        int repeatSp = Math.max(12, textSize - 16);
        android.graphics.Paint repeatPaint = new android.graphics.Paint();
        repeatPaint.setTextSize(repeatSp * scaledDensity);
        if (custom != null) repeatPaint.setTypeface(custom);
        float repeatHeightDp = (repeatPaint.getFontMetrics().bottom - repeatPaint.getFontMetrics().top) / density;

        float itemHeightDp = timeHeightDp + repeatHeightDp + 28;
        int count = (int) (recyclerHeightDp / itemHeightDp);
        return Math.max(1, count);
    }

    private void updatePageIndicator() {
        TextView pageIndicator = findViewById(R.id.page_indicator);
        View bottomDivider = findViewById(R.id.bottom_divider);
        View bottomBar = findViewById(R.id.bottom_bar);
        if (scrollAppList) {
            pageIndicator.setVisibility(View.GONE);
            bottomDivider.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            return;
        }
        pageIndicator.setVisibility(View.VISIBLE);
        bottomDivider.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        int totalPages = (int) Math.ceil((double) alarms.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private void openOptionsDialog() {
        boolean disableAlarmWallpaper = prefs.getBoolean("alarm_disable_wallpaper", true);
        new ClockOptionsDialog(this, disableAlarmWallpaper, () -> {
            reloadWallpaperPreference();
            if (showWallpaperBackdrop) {
                LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
                clockSurfaceColor = backdrop.surfaceColor;
                showWallpaperBackdrop = backdrop.showWallpaperBackdrop;
                View topLayout = findViewById(R.id.top_layout);
                View recyclerView = findViewById(R.id.clock_list);
                View container = findViewById(R.id.app_list_container);
                LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, clockSurfaceColor,
                        topLayout, recyclerView, container);
            }
        }).show();
    }

    private void reloadWallpaperPreference() {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
    }

    private void openAddDialog() {
        new ClockAlarmDialog(this, null, alarm -> {
            ClockAlarmHelper.addOrUpdate(ClockActivity.this, alarm);
            loadAlarms();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }, null).show();
    }

    private void openEditDialog(ClockAlarmHelper.Alarm alarm) {
        new ClockAlarmDialog(this, alarm, updated -> {
            ClockAlarmHelper.addOrUpdate(ClockActivity.this, updated);
            loadAlarms();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }, deletedId -> {
            ClockAlarmHelper.delete(ClockActivity.this, deletedId);
            loadAlarms();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            Toast.makeText(ClockActivity.this, "Alarm deleted", Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void toggleAlarm(ClockAlarmHelper.Alarm alarm) {
        boolean newEnabled = !alarm.enabled;

        if (newEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Toast.makeText(this, "Enable exact alarm permission", Toast.LENGTH_SHORT).show();
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignored) {}
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && newEnabled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
                Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        ClockAlarmHelper.setEnabled(this, alarm.id, newEnabled);
        alarm.enabled = newEnabled;
        clockAdapter.notifyDataSetChanged();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontHelper.applyToViewTree(this, findViewById(android.R.id.content));
        loadAlarms();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications disabled - alarms will not show", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(homeButtonReceiver); } catch (Exception ignored) {}
    }

    private class ClockAdapter extends RecyclerView.Adapter<ClockAdapter.ViewHolder> {
        private final List<ClockAlarmHelper.Alarm> items;
        private final ClockActivity activity;
        private final int theme;

        ClockAdapter(List<ClockAlarmHelper.Alarm> items, ClockActivity activity, int theme) {
            this.items = items;
            this.activity = activity;
            this.theme = theme;
        }

        @Override
        public int getItemViewType(int position) {
            return items.isEmpty() ? 0 : 1;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0) {
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setPadding(32, 48, 32, 48);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setTextSize(18);
                return new ViewHolder(tv, true);
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clock_alarm, parent, false);
            return new ViewHolder(view, false);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getItemViewType(position) == 0) {
                TextView tv = (TextView) holder.itemView;
                tv.setText("No alarms\nTap + to add");
                tv.setTextSize(18);
                ThemeUtils.applyTextColor(tv, theme, activity);
                FontHelper.applyToViewTree(activity, holder.itemView);
                LauncherBackdropHelper.applySurfaceBackground(holder.itemView, activity.showWallpaperBackdrop, activity.clockSurfaceColor);
                holder.itemView.setOnClickListener(v -> activity.openAddDialog());
                return;
            }
            int globalPosition = activity.scrollAppList ? position : currentPage * itemsPerPage + position;
            if (globalPosition >= items.size()) return;
            ClockAlarmHelper.Alarm alarm = items.get(globalPosition);

            holder.timeView.setText(alarm.getTimeText());
            holder.timeView.setTextSize(activity.textSize);
            holder.timeView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);

            String label = alarm.label != null ? alarm.label.trim() : "";
            String repeat = alarm.getRepeatText();

            if (!label.isEmpty()) {
                holder.labelView.setText(label);
                holder.labelView.setVisibility(View.VISIBLE);
                holder.labelView.setTextSize(activity.textSize);
                holder.labelView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);
            } else {
                holder.labelView.setVisibility(View.GONE);
            }
            holder.repeatView.setText(repeat);
            holder.repeatView.setVisibility(View.VISIBLE);
            holder.repeatView.setTextSize(Math.max(12, activity.textSize - 16));

            boolean snoozed = ClockAlarmHelper.isSnoozed(activity, alarm.id);
            if (snoozed) {
                holder.snoozedView.setText("Snoozed");
                holder.snoozedView.setVisibility(View.VISIBLE);
                holder.snoozedView.setTextSize(12);
            } else {
                holder.snoozedView.setVisibility(View.GONE);
            }

            ThemeUtils.applyTextColor(holder.timeView, theme, activity);
            ThemeUtils.applyTextColor(holder.labelView, theme, activity);
            ThemeUtils.applyTextColor(holder.repeatView, theme, activity);
            if (snoozed) {
                int sTxt = ThemeUtils.getTextColor(theme, activity);
                int sBg = ThemeUtils.getBgColor(theme, activity);
                android.graphics.drawable.GradientDrawable sd = new android.graphics.drawable.GradientDrawable();
                sd.setColor(sTxt);
                sd.setStroke((int)(2 * activity.getResources().getDisplayMetrics().density), sTxt);
                int sr = org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.getCornerRadiusPx(activity);
                if (sr > 0) sd.setCornerRadius(sr);
                holder.snoozedView.setBackground(sd);
                holder.snoozedView.setTextColor(sBg);
                holder.snoozedView.setTypeface(null, Typeface.BOLD);
            } else {
                ThemeUtils.applyTextColor(holder.snoozedView, theme, activity);
            }
            FontHelper.applyToViewTree(activity, holder.itemView);
            LauncherBackdropHelper.applySurfaceBackground(holder.itemView, activity.showWallpaperBackdrop, activity.clockSurfaceColor);

            holder.toggleView.setText(alarm.enabled ? "ON" : "OFF");

            holder.toggleView.setTypeface(null, alarm.enabled ? Typeface.BOLD : Typeface.NORMAL);

            int txt = ThemeUtils.getTextColor(theme, activity);
            int bg = ThemeUtils.getBgColor(theme, activity);

            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(alarm.enabled ? txt : bg);
            drawable.setStroke((int)(2 * activity.getResources().getDisplayMetrics().density), txt);

            int radius = org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.getCornerRadiusPx(activity);
            if (radius > 0) drawable.setCornerRadius(radius);
            holder.toggleView.setBackground(drawable);
            holder.toggleView.setTextColor(alarm.enabled ? bg : txt);

            holder.toggleView.setOnClickListener(v -> activity.toggleAlarm(alarm));
            holder.itemView.setOnClickListener(v -> activity.openEditDialog(alarm));
            holder.snoozedView.setOnClickListener(v -> {
                ClockAlarmHelper.clearSnoozed(activity, alarm.id);
                if (!alarm.hasRepeat()) {
                    ClockAlarmHelper.setEnabled(activity, alarm.id, false);
                }
                activity.loadAlarms();
                EinkRefreshHelper.refreshEink(activity.getWindow(), activity.prefs, activity.prefs.getInt("eink_refresh_delay", 100));
                Toast.makeText(activity, "Snooze cancelled", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            if (items.isEmpty()) return 1;
            if (activity.scrollAppList) return items.size();
            int start = currentPage * itemsPerPage;
            return Math.min(itemsPerPage, items.size() - start);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView timeView;
            TextView labelView;
            TextView repeatView;
            TextView snoozedView;
            TextView toggleView;
            ViewHolder(View itemView, boolean isEmpty) {
                super(itemView);
                if (isEmpty) return;
                timeView = itemView.findViewById(R.id.alarm_time);
                labelView = itemView.findViewById(R.id.alarm_label);
                repeatView = itemView.findViewById(R.id.alarm_repeat);
                snoozedView = itemView.findViewById(R.id.alarm_snoozed);
                toggleView = itemView.findViewById(R.id.alarm_toggle);
            }
        }
    }
}