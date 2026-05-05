package org.matiasdesu.thinklauncherv2.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
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
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CALENDAR = 3001;
    private int textSize;
    private boolean boldText;
    private List<CalendarEvent> events;
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private CalendarAdapter calendarAdapter;
    private SharedPreferences prefs;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private boolean showWallpaperBackdrop;
    private boolean showAccount;
    private boolean highlightToday;
    private boolean showMonthSeparators;
    private boolean highlightEventTimes;
    private int highlightStyle; // 0: Bold, 1: Underscore, 2: Bold & Underscore
    private int eventLimit;
    private int calendarSurfaceColor;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, MMM d - HH:mm", Locale.getDefault());
    private final SimpleDateFormat allDayDateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
    private final SimpleDateFormat monthSeparatorFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(CalendarActivity.this, MainActivity.class);
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
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        allDayDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        monthSeparatorFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        LauncherBackdropHelper.Result backdrop = LauncherBackdropHelper.setup(this, theme, opacityEnabled);
        calendarSurfaceColor = backdrop.surfaceColor;
        showWallpaperBackdrop = backdrop.showWallpaperBackdrop;

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        TextView titleView = findViewById(R.id.calendar_title);
        ThemeUtils.applyTextColor(titleView, theme, this);
        titleView.setOnLongClickListener(v -> {
            new CalendarOptionsDialog(this, showAccount, eventLimit, highlightToday, showMonthSeparators,
                    highlightEventTimes, highlightStyle, () -> {
                loadCalendarOptions();
                itemsPerPage = calculateCalendarItemsPerPage();
                loadEvents();
            }).show();
            return true;
        });

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        ImageView openCalendarButton = findViewById(R.id.open_calendar_button);
        openCalendarButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        openCalendarButton.setOnClickListener(v -> openDefaultCalendarApp());

        RecyclerView recyclerView = findViewById(R.id.calendar_list);
        View topLayout = findViewById(R.id.top_layout);
        View container = findViewById(R.id.app_list_container);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, calendarSurfaceColor,
                topLayout, recyclerView, container);

        textSize = prefs.getInt("text_size", 24);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;
        loadCalendarOptions();
        itemsPerPage = calculateCalendarItemsPerPage();

        recyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return scrollAppList;
            }
        });
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        events = new ArrayList<>();
        calendarAdapter = new CalendarAdapter(events, this, theme);
        recyclerView.setAdapter(calendarAdapter);

        if (!scrollAppList) {
            new SwipePageNavigator(this, recyclerView, container,
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
                            return (int) Math.ceil((double) events.size() / itemsPerPage);
                        }

                        @Override
                        public void updatePageIndicator() {
                            CalendarActivity.this.updatePageIndicator();
                        }
                    }, theme);
        }

        updatePageIndicator();
        signIn();
    }

    private void signIn() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_CALENDAR },
                    REQUEST_READ_CALENDAR);
            return;
        }
        loadEvents();
    }

    private void loadCalendarOptions() {
        showAccount = prefs.getBoolean("calendar_show_account", false);
        eventLimit = prefs.getInt("calendar_event_limit", 10);
        if (eventLimit != 10 && eventLimit != 25 && eventLimit != 50) {
            eventLimit = 10;
        }
        highlightToday = prefs.getBoolean("calendar_highlight_today", false);
        showMonthSeparators = prefs.getBoolean("calendar_month_separators", false);
        highlightEventTimes = prefs.getBoolean("calendar_highlight_event_times", false);
        highlightStyle = prefs.getInt("calendar_highlight_style", 0);
    }

    private void loadEvents() {
        events.clear();
        long now = System.currentTimeMillis();
        long end = now + 365L * 24L * 60L * 60L * 1000L;
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, now);
        ContentUris.appendId(builder, end);

        String[] projection = {
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
                CalendarContract.Instances.OWNER_ACCOUNT
        };

        String selection = CalendarContract.Instances.BEGIN + ">=?";
        String[] args = { String.valueOf(now) };
        String sortOrder = CalendarContract.Instances.BEGIN + " ASC";

        try (Cursor cursor = getContentResolver().query(builder.build(), projection, selection, args, sortOrder)) {
            if (cursor != null) {
                while (cursor.moveToNext() && events.size() < eventLimit) {
                    long id = cursor.getLong(0);
                    String title = cursor.getString(1);
                    long begin = cursor.getLong(2);
                    long eventEnd = cursor.getLong(3);
                    boolean allDay = cursor.getInt(4) == 1;
                    String calendarName = cursor.getString(5);
                    String ownerAccount = cursor.getString(6);
                    if (title == null || title.trim().isEmpty()) {
                        title = "Untitled event";
                    }
                    events.add(new CalendarEvent(id, title, begin, eventEnd, allDay, calendarName, ownerAccount));
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Calendar permission is required", Toast.LENGTH_SHORT).show();
        }

        if (events.isEmpty()) {
            events.add(CalendarEvent.message("No upcoming events"));
        }
        currentPage = 0;
        calendarAdapter.notifyDataSetChanged();
        updatePageIndicator();
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
        int totalPages = (int) Math.ceil((double) events.size() / itemsPerPage);
        if (totalPages == 0) {
            totalPages = 1;
        }
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    private int calculateCalendarItemsPerPage() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        float scaledDensity = dm.scaledDensity;
        float screenHeightDp = dm.heightPixels / density;

        int navBarHeightPx = 0;
        try {
            navBarHeightPx = getResources().getDimensionPixelSize(
                    getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        } catch (Exception e) {
        }
        screenHeightDp -= navBarHeightPx / density;

        float recyclerHeightDp = screenHeightDp - 48 - 4 - 48;
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(textSize * scaledDensity);
        float titleHeightDp = (titlePaint.getFontMetrics().bottom - titlePaint.getFontMetrics().top) / density;

        Paint timePaint = new Paint();
        timePaint.setTextSize(Math.max(12, textSize - 8) * scaledDensity);
        float timeHeightDp = (timePaint.getFontMetrics().bottom - timePaint.getFontMetrics().top) / density;

        float separatorHeightDp = showMonthSeparators ? timeHeightDp + 8 : 0;
        float itemHeightDp = titleHeightDp + timeHeightDp + separatorHeightDp + 26;
        int count = (int) (recyclerHeightDp / itemHeightDp);
        return Math.max(1, count);
    }

    private boolean isToday(CalendarEvent event) {
        Calendar eventCalendar = Calendar.getInstance();
        if (event.allDay) {
            eventCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        eventCalendar.setTimeInMillis(event.begin);
        Calendar todayCalendar = Calendar.getInstance();
        return eventCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR)
                && eventCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR);
    }

    private boolean shouldShowMonthSeparator(int position) {
        if (!showMonthSeparators || position < 0 || position >= events.size()) {
            return false;
        }
        CalendarEvent event = events.get(position);
        if (event.messageOnly) {
            return false;
        }
        if (position == 0) {
            return true;
        }
        CalendarEvent previous = events.get(position - 1);
        return previous.messageOnly || !getMonthKey(event).equals(getMonthKey(previous));
    }

    private String getMonthKey(CalendarEvent event) {
        Calendar calendar = Calendar.getInstance();
        if (event.allDay) {
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        calendar.setTimeInMillis(event.begin);
        return calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH);
    }

    private String formatMonthSeparator(CalendarEvent event) {
        if (event.allDay) {
            return monthSeparatorFormat.format(new Date(event.begin));
        }
        SimpleDateFormat localMonthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return localMonthFormat.format(new Date(event.begin));
    }

    private void openEvent(CalendarEvent event) {
        if (event.messageOnly) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id));
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Calendar app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDefaultCalendarApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Calendar app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private CharSequence formatEventTime(CalendarEvent event) {
        if (event.messageOnly) {
            return "Grant calendar permission to load events";
        }
        String account = "";
        if (showAccount) {
            String value = event.ownerAccount;
            if (value == null || value.trim().isEmpty()) {
                value = event.calendarName;
            }
            account = value == null || value.trim().isEmpty() ? "" : " - " + value;
        }
        if (event.allDay) {
            return allDayDateFormat.format(new Date(event.begin)) + " - All day" + account;
        }
        String start = dateTimeFormat.format(new Date(event.begin));
        String end = timeFormat.format(new Date(event.end));
        String text = start + " - " + end + account;
        if (!highlightEventTimes) {
            return text;
        }

        String startTime = timeFormat.format(new Date(event.begin));
        String timeRange = startTime + " - " + end;
        int startIndex = text.indexOf(timeRange);
        if (startIndex < 0) {
            return text;
        }
        SpannableString spannable = new SpannableString(text);
        if (highlightStyle == 0 || highlightStyle == 2) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + timeRange.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (highlightStyle == 1 || highlightStyle == 2) {
            spannable.setSpan(new UnderlineSpan(), startIndex, startIndex + timeRange.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED && calendarAdapter != null) {
            loadEvents();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CALENDAR) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadEvents();
            } else {
                events.clear();
                events.add(CalendarEvent.message("Calendar permission is required"));
                calendarAdapter.notifyDataSetChanged();
                updatePageIndicator();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(homeButtonReceiver);
        } catch (Exception e) {
        }
    }

    private static class CalendarEvent {
        long id;
        String title;
        long begin;
        long end;
        boolean allDay;
        String calendarName;
        String ownerAccount;
        boolean messageOnly;

        CalendarEvent(long id, String title, long begin, long end, boolean allDay, String calendarName,
                String ownerAccount) {
            this.id = id;
            this.title = title;
            this.begin = begin;
            this.end = end;
            this.allDay = allDay;
            this.calendarName = calendarName;
            this.ownerAccount = ownerAccount;
        }

        static CalendarEvent message(String title) {
            CalendarEvent event = new CalendarEvent(-1, title, 0, 0, false, "", "");
            event.messageOnly = true;
            return event;
        }
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private List<CalendarEvent> items;
        private CalendarActivity activity;
        private int theme;

        CalendarAdapter(List<CalendarEvent> items, CalendarActivity activity, int theme) {
            this.items = items;
            this.activity = activity;
            this.theme = theme;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_event, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            int globalPosition = activity.scrollAppList ? position : currentPage * itemsPerPage + position;
            if (globalPosition >= items.size()) {
                return;
            }

            CalendarEvent event = items.get(globalPosition);
            boolean showSeparator = activity.shouldShowMonthSeparator(globalPosition);
            holder.monthSeparatorView.setVisibility(showSeparator ? View.VISIBLE : View.GONE);
            if (showSeparator) {
                holder.monthSeparatorView.setText(activity.formatMonthSeparator(event));
                holder.monthSeparatorView.setTextSize(Math.max(12, activity.textSize - 10));
                holder.monthSeparatorView.setTypeface(null, Typeface.BOLD);
                ThemeUtils.applyTextColor(holder.monthSeparatorView, theme, activity);
            }
            holder.titleView.setText(event.title);
            holder.timeView.setText(activity.formatEventTime(event));
            holder.titleView.setTextSize(activity.textSize);
            holder.timeView.setTextSize(Math.max(12, activity.textSize - 8));
            holder.titleView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);
            holder.timeView.setTypeface(null, Typeface.NORMAL);
            holder.todayDot.setVisibility(activity.highlightToday && activity.isToday(event) && !event.messageOnly
                    ? View.VISIBLE : View.GONE);
            LauncherBackdropHelper.applySurfaceBackground(holder.itemView, activity.showWallpaperBackdrop,
                    activity.calendarSurfaceColor);
            ThemeUtils.applyTextColor(holder.titleView, theme, activity);
            ThemeUtils.applyTextColor(holder.timeView, theme, activity);
            holder.todayDot.getBackground().setTint(ThemeUtils.getTextColor(theme, activity));
            holder.itemView.setOnClickListener(v -> activity.openEvent(event));
        }

        @Override
        public int getItemCount() {
            if (activity.scrollAppList) {
                return items.size();
            }
            int start = currentPage * itemsPerPage;
            return Math.min(itemsPerPage, items.size() - start);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;
            TextView timeView;
            TextView monthSeparatorView;
            View todayDot;

            ViewHolder(View itemView) {
                super(itemView);
                monthSeparatorView = itemView.findViewById(R.id.month_separator);
                titleView = itemView.findViewById(R.id.event_title);
                timeView = itemView.findViewById(R.id.event_time);
                todayDot = itemView.findViewById(R.id.today_dot);
            }
        }
    }
}
