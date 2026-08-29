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
import android.os.Handler;
import android.os.Looper;
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
import org.matiasdesu.thinklauncherv2.utils.ClockAlarmHelper;
import org.matiasdesu.thinklauncherv2.utils.ClockTimerHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClockActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 5001;
    public static final int FILTER_ALARMS = 0;
    public static final int FILTER_TIMERS = 1;

    private int textSize;
    private boolean boldText;
    private List<ClockAlarmHelper.Alarm> alarms;
    private List<ClockTimerHelper.Timer> timers;
    private List<Object> displayItems;
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
    private int clockFilterMode;
    private Handler timerHandler;
    private Runnable timerTickRunnable;

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
        clockFilterMode = prefs.getInt("clock_filter_by", FILTER_ALARMS);
        if (clockFilterMode == 2) {
            clockFilterMode = FILTER_TIMERS;
            prefs.edit().putInt("clock_filter_by", clockFilterMode).apply();
        } else if (clockFilterMode < FILTER_ALARMS || clockFilterMode > FILTER_TIMERS) {
            clockFilterMode = FILTER_ALARMS;
            prefs.edit().putInt("clock_filter_by", clockFilterMode).apply();
        }
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
        updateTitle();

        titleView.setOnClickListener(v -> openOptionsDialog());
        titleView.setOnLongClickListener(v -> {
            if (scrollAppList) {
                RecyclerView rv = findViewById(R.id.clock_list);
                if (rv != null) rv.scrollToPosition(0);
                EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                return true;
            }
            return false;
        });

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        ImageView addButton = findViewById(R.id.add_alarm_button);
        addButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        addButton.setOnClickListener(v -> openAddDialogForCurrentFilter());

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
        timers = new ArrayList<>();
        displayItems = new ArrayList<>();
        clockAdapter = new ClockAdapter(displayItems, this, theme);
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
                            return (int) Math.ceil((double) displayItems.size() / itemsPerPage);
                        }
                        @Override
                        public void updatePageIndicator() {
                            ClockActivity.this.updatePageIndicator();
                        }
                    }, theme);
        } else {
            pageNavigator = null;
        }

        timerHandler = new Handler(Looper.getMainLooper());
        timerTickRunnable = new Runnable() {
            @Override
            public void run() {
                boolean hasRunning = false;
                boolean hasExpired = false;
                for (ClockTimerHelper.Timer t : timers) {
                    long end = ClockTimerHelper.getEndMillis(ClockActivity.this, t.id);
                    if (end != 0 && System.currentTimeMillis() >= end) {
                        hasExpired = true;
                        ClockTimerHelper.handleExpired(ClockActivity.this, t.id);
                    } else if (ClockTimerHelper.isRunning(ClockActivity.this, t.id)) {
                        hasRunning = true;
                    }
                }
                if (hasExpired) {
                    loadAll();
                } else if (hasRunning && clockFilterMode == FILTER_TIMERS) {
                    if (clockAdapter != null) clockAdapter.notifyDataSetChanged();
                }
                long delay = hasRunning ? 250 : 1000;
                timerHandler.postDelayed(this, delay);
            }
        };

        updatePageIndicator();
        loadAll();
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

    private void loadAll() {
        int previousPage = currentPage;
        alarms.clear();
        List<ClockAlarmHelper.Alarm> loadedAlarms = ClockAlarmHelper.loadAlarms(this);
        Collections.sort(loadedAlarms, (a, b) -> {
            if (a.hour != b.hour) return a.hour - b.hour;
            return a.minute - b.minute;
        });
        alarms.addAll(loadedAlarms);

        timers.clear();
        List<ClockTimerHelper.Timer> loadedTimers = ClockTimerHelper.loadTimers(this);
        Collections.sort(loadedTimers, (a, b) -> Integer.compare(a.durationSec, b.durationSec));
        ClockTimerHelper.clearExpiredRunning(this);
        timers.addAll(loadedTimers);

        updateDisplayItems();

        int totalPages = (int) Math.ceil((double) displayItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (previousPage >= totalPages) previousPage = totalPages - 1;
        if (previousPage < 0) previousPage = 0;
        currentPage = previousPage;
        if (pageNavigator != null) {
            pageNavigator.setCurrentPage(currentPage);
            pageNavigator.setTotalItems(displayItems.size());
        }
        if (clockAdapter != null) clockAdapter.notifyDataSetChanged();
        updatePageIndicator();
        updateTitle();
    }

    private void updateDisplayItems() {
        displayItems.clear();
        if (clockFilterMode == FILTER_ALARMS) {
            displayItems.addAll(alarms);
        } else {
            displayItems.addAll(timers);
        }
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
        int totalPages = (int) Math.ceil((double) displayItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private void updateTitle() {
        TextView titleView = findViewById(R.id.clock_title);
        if (titleView == null) return;
        if (clockFilterMode == FILTER_ALARMS) titleView.setText("Alarms");
        else titleView.setText("Timers");
        ThemeUtils.applyTextColor(titleView, theme, this);
    }

    private void openOptionsDialog() {
        boolean disableAlarmWallpaper = prefs.getBoolean("alarm_disable_wallpaper", true);
        new ClockOptionsDialog(this, disableAlarmWallpaper, clockFilterMode, () -> {
            reloadWallpaperPreference();
            LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
            clockSurfaceColor = backdrop.surfaceColor;
            showWallpaperBackdrop = backdrop.showWallpaperBackdrop;
            View topLayout = findViewById(R.id.top_layout);
            View recyclerView = findViewById(R.id.clock_list);
            View container = findViewById(R.id.app_list_container);
            LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, clockSurfaceColor,
                    topLayout, recyclerView, container);
            if (clockAdapter != null) clockAdapter.notifyDataSetChanged();
        }, filter -> {
            clockFilterMode = filter;
            prefs.edit().putInt("clock_filter_by", filter).apply();
            currentPage = 0;
            updateDisplayItems();
            if (pageNavigator != null) {
                pageNavigator.setCurrentPage(0);
                pageNavigator.setTotalItems(displayItems.size());
            }
            if (clockAdapter != null) clockAdapter.notifyDataSetChanged();
            updatePageIndicator();
            updateTitle();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }).show();
    }

    private void reloadWallpaperPreference() {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
    }

    private void openAddDialogForCurrentFilter() {
        if (clockFilterMode == FILTER_TIMERS) openAddTimerDialog();
        else openAddDialog();
    }

    private void openAddDialog() {
        new ClockAlarmDialog(this, null, alarm -> {
            ClockAlarmHelper.addOrUpdate(ClockActivity.this, alarm);
            loadAll();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }, null).show();
    }

    private void openEditDialog(ClockAlarmHelper.Alarm alarm) {
        new ClockAlarmDialog(this, alarm, updated -> {
            ClockAlarmHelper.addOrUpdate(ClockActivity.this, updated);
            loadAll();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }, deletedId -> {
            ClockAlarmHelper.delete(ClockActivity.this, deletedId);
            loadAll();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            Toast.makeText(ClockActivity.this, "Alarm deleted", Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void openAddTimerDialog() {
        new ClockTimerDialog(this, null, timer -> {
            ClockTimerHelper.addOrUpdate(ClockActivity.this, timer);
            loadAll();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }, null).show();
    }

    private void openEditTimerDialog(ClockTimerHelper.Timer timer) {
        new ClockTimerDialog(this, timer, updated -> {
            boolean wasRunning = ClockTimerHelper.isRunning(this, timer.id);
            boolean wasPaused = ClockTimerHelper.isPaused(this, timer.id);
            if (wasRunning || wasPaused) {
                ClockTimerHelper.cancelRunning(this, timer.id);
                ClockTimerHelper.clearPaused(this, timer.id);
            }
            ClockTimerHelper.addOrUpdate(ClockActivity.this, updated);
            loadAll();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
        }, deletedId -> {
            ClockTimerHelper.delete(ClockActivity.this, deletedId);
            loadAll();
            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            Toast.makeText(ClockActivity.this, "Timer deleted", Toast.LENGTH_SHORT).show();
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

    private void toggleTimer(ClockTimerHelper.Timer timer) {
        if (ClockTimerHelper.isRunning(this, timer.id) || ClockTimerHelper.isPaused(this, timer.id)) {
            ClockTimerHelper.cancelRunning(this, timer.id);
            ClockTimerHelper.clearPaused(this, timer.id);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
                    Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
            ClockTimerHelper.start(this, timer.id);
        }
        clockAdapter.notifyDataSetChanged();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

    private void toggleTimerPause(ClockTimerHelper.Timer timer) {
        if (ClockTimerHelper.isRunning(this, timer.id)) {
            ClockTimerHelper.pause(this, timer.id);
        } else if (ClockTimerHelper.isPaused(this, timer.id)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
                    Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
            ClockTimerHelper.resume(this, timer.id);
        }
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
        loadAll();
        if (timerHandler != null && timerTickRunnable != null) timerHandler.post(timerTickRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerHandler != null) timerHandler.removeCallbacks(timerTickRunnable);
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
        if (timerHandler != null) timerHandler.removeCallbacks(timerTickRunnable);
    }

    private class ClockAdapter extends RecyclerView.Adapter<ClockAdapter.ViewHolder> {
        private final List<Object> items;
        private final ClockActivity activity;
        private final int theme;

        ClockAdapter(List<Object> items, ClockActivity activity, int theme) {
            this.items = items;
            this.activity = activity;
            this.theme = theme;
        }

        @Override
        public int getItemViewType(int position) {
            if (items.isEmpty()) return 0;
            Object obj = items.get(activity.scrollAppList ? position : currentPage * itemsPerPage + position);
            if (obj instanceof ClockTimerHelper.Timer) return 2;
            return 1;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (items.isEmpty()) {
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setPadding(32, 48, 32, 48);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setTextSize(18);
                return new ViewHolder(tv, true);
            }
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
            if (items.isEmpty()) {
                TextView tv = (TextView) holder.itemView;
                if (activity.clockFilterMode == FILTER_TIMERS) tv.setText("No timers\nTap + to add");
                else tv.setText("No alarms\nTap + to add");
                tv.setTextSize(18);
                ThemeUtils.applyTextColor(tv, theme, activity);
                FontHelper.applyToViewTree(activity, holder.itemView);
                LauncherBackdropHelper.applySurfaceBackground(holder.itemView, activity.showWallpaperBackdrop, activity.clockSurfaceColor);
                holder.itemView.setOnClickListener(v -> activity.openAddDialogForCurrentFilter());
                return;
            }
            int globalPosition = activity.scrollAppList ? position : currentPage * itemsPerPage + position;
            if (globalPosition >= items.size()) return;
            Object obj = items.get(globalPosition);
            if (obj instanceof ClockTimerHelper.Timer) {
                bindTimer(holder, (ClockTimerHelper.Timer) obj);
            } else if (obj instanceof ClockAlarmHelper.Alarm) {
                bindAlarm(holder, (ClockAlarmHelper.Alarm) obj);
            }
        }

        private void bindAlarm(ViewHolder holder, ClockAlarmHelper.Alarm alarm) {
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
            if (holder.pauseView != null) holder.pauseView.setVisibility(View.GONE);
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
                if (!alarm.hasRepeat()) ClockAlarmHelper.setEnabled(activity, alarm.id, false);
                activity.loadAll();
                EinkRefreshHelper.refreshEink(activity.getWindow(), activity.prefs, activity.prefs.getInt("eink_refresh_delay", 100));
                Toast.makeText(activity, "Snooze cancelled", Toast.LENGTH_SHORT).show();
            });
        }

        private void bindTimer(ViewHolder holder, ClockTimerHelper.Timer timer) {
            boolean running = ClockTimerHelper.isRunning(activity, timer.id);
            boolean paused = ClockTimerHelper.isPaused(activity, timer.id);
            String timeText;
            if (running) timeText = ClockTimerHelper.formatRemaining(ClockTimerHelper.getRemainingSec(activity, timer.id));
            else if (paused) timeText = ClockTimerHelper.formatRemaining(ClockTimerHelper.getPausedRemaining(activity, timer.id));
            else timeText = timer.getDurationText();
            holder.timeView.setText(timeText);
            holder.timeView.setTextSize(activity.textSize);
            holder.timeView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);
            String label = timer.label != null ? timer.label.trim() : "";
            if (!label.isEmpty()) {
                holder.labelView.setText(label);
                holder.labelView.setVisibility(View.VISIBLE);
                holder.labelView.setTextSize(activity.textSize);
                holder.labelView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);
            } else {
                holder.labelView.setVisibility(View.GONE);
            }
            holder.repeatView.setText("Timer");
            holder.repeatView.setVisibility(View.VISIBLE);
            holder.repeatView.setTextSize(Math.max(12, activity.textSize - 16));
            holder.snoozedView.setVisibility(View.GONE);
            ThemeUtils.applyTextColor(holder.timeView, theme, activity);
            ThemeUtils.applyTextColor(holder.labelView, theme, activity);
            ThemeUtils.applyTextColor(holder.repeatView, theme, activity);
            FontHelper.applyToViewTree(activity, holder.itemView);
            LauncherBackdropHelper.applySurfaceBackground(holder.itemView, activity.showWallpaperBackdrop, activity.clockSurfaceColor);
            boolean active = running || paused;
            holder.toggleView.setText(active ? "STOP" : "START");
            holder.toggleView.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            int txt = ThemeUtils.getTextColor(theme, activity);
            int bg = ThemeUtils.getBgColor(theme, activity);
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(active ? txt : bg);
            drawable.setStroke((int)(2 * activity.getResources().getDisplayMetrics().density), txt);
            int radius = org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.getCornerRadiusPx(activity);
            if (radius > 0) drawable.setCornerRadius(radius);
            holder.toggleView.setBackground(drawable);
            holder.toggleView.setTextColor(active ? bg : txt);
            holder.toggleView.setOnClickListener(v -> activity.toggleTimer(timer));
            if (holder.pauseView != null) {
                if (running) {
                    holder.pauseView.setVisibility(View.VISIBLE);
                    holder.pauseView.setText("PAUSE");
                    holder.pauseView.setTypeface(null, Typeface.BOLD);
                    android.graphics.drawable.GradientDrawable pd = new android.graphics.drawable.GradientDrawable();
                    pd.setColor(bg);
                    pd.setStroke((int)(2 * activity.getResources().getDisplayMetrics().density), txt);
                    if (radius > 0) pd.setCornerRadius(radius);
                    holder.pauseView.setBackground(pd);
                    holder.pauseView.setTextColor(txt);
                    holder.pauseView.setOnClickListener(v -> activity.toggleTimerPause(timer));
                } else if (paused) {
                    holder.pauseView.setVisibility(View.VISIBLE);
                    holder.pauseView.setText("RESUME");
                    holder.pauseView.setTypeface(null, Typeface.BOLD);
                    android.graphics.drawable.GradientDrawable pd = new android.graphics.drawable.GradientDrawable();
                    pd.setColor(txt);
                    pd.setStroke((int)(2 * activity.getResources().getDisplayMetrics().density), txt);
                    if (radius > 0) pd.setCornerRadius(radius);
                    holder.pauseView.setBackground(pd);
                    holder.pauseView.setTextColor(bg);
                    holder.pauseView.setOnClickListener(v -> activity.toggleTimerPause(timer));
                } else {
                    holder.pauseView.setVisibility(View.GONE);
                    holder.pauseView.setOnClickListener(null);
                }
            }
            holder.itemView.setOnClickListener(v -> activity.openEditTimerDialog(timer));
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
            TextView pauseView;
            ViewHolder(View itemView, boolean isEmpty) {
                super(itemView);
                if (isEmpty) return;
                timeView = itemView.findViewById(R.id.alarm_time);
                labelView = itemView.findViewById(R.id.alarm_label);
                repeatView = itemView.findViewById(R.id.alarm_repeat);
                snoozedView = itemView.findViewById(R.id.alarm_snoozed);
                toggleView = itemView.findViewById(R.id.alarm_toggle);
                pauseView = itemView.findViewById(R.id.timer_pause);
            }
        }
    }
}
