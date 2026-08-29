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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
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
    private List<CalendarEvent> allEvents;
    private List<CalendarEvent> agendaEvents;
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private CalendarAdapter agendaAdapter;
    private MonthGridAdapter monthAdapter;
    private SharedPreferences prefs;
    private boolean scrollAppList;
    private boolean opacityEnabled;
    private boolean appLauncherAnimations;
    private boolean showWallpaperBackdrop;
    private boolean showMonthGrid;
    private boolean showAccount;
    private boolean highlightToday;
    private boolean showMonthSeparators;
    private boolean showDaySeparators;
    private boolean highlightEventTimes;
    private int highlightStyle;
    private int eventLimit;
    private int calendarSurfaceColor;
    private Calendar currentMonth;
    private Calendar selectedDay;
    private List<Calendar> gridDays;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, MMM d - HH:mm", Locale.getDefault());
    private final SimpleDateFormat allDayDateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
    private final SimpleDateFormat monthTitleFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat monthSeparatorFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat daySeparatorFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
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
        appLauncherAnimations = prefs.getInt("screen_animations", 0) == 1;
        setTheme(LauncherBackdropHelper.resolveThemeResId(this, theme, opacityEnabled));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        allDayDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        monthSeparatorFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        daySeparatorFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

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
        titleView.setOnClickListener(v -> {
            new CalendarOptionsDialog(this, showAccount, eventLimit, highlightToday, showMonthSeparators,
                    showDaySeparators, highlightEventTimes, highlightStyle, () -> {
                loadCalendarOptions();
                applyMonthGridVisibility();
                itemsPerPage = calculateCalendarItemsPerPage();
                loadEvents();
                if (monthAdapter != null) monthAdapter.notifyDataSetChanged();
            }).show();
        });

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
        });

        ImageView openCalendarButton = findViewById(R.id.open_calendar_button);
        openCalendarButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        openCalendarButton.setOnClickListener(v -> openDefaultCalendarApp());

        TextView monthTitle = findViewById(R.id.month_title);
        ThemeUtils.applyTextColor(monthTitle, theme, this);
        monthTitle.setOnClickListener(v -> goToToday());
        ImageView prevMonth = findViewById(R.id.prev_month_button);
        ImageView nextMonth = findViewById(R.id.next_month_button);
        prevMonth.setColorFilter(ThemeUtils.getTextColor(theme, this));
        nextMonth.setColorFilter(ThemeUtils.getTextColor(theme, this));
        prevMonth.setOnClickListener(v -> shiftMonth(-1));
        nextMonth.setOnClickListener(v -> shiftMonth(1));

        View topLayout = findViewById(R.id.top_layout);
        View monthHeader = findViewById(R.id.month_header);
        View weekdayHeader = findViewById(R.id.weekday_header);
        RecyclerView monthGrid = findViewById(R.id.month_grid);
        RecyclerView agendaList = findViewById(R.id.calendar_list);
        View container = findViewById(R.id.app_list_container);
        LauncherBackdropHelper.applySurfaceBackgrounds(showWallpaperBackdrop, calendarSurfaceColor,
                topLayout, monthHeader, weekdayHeader, monthGrid, agendaList, container);
        for (int i = 0; i < 7; i++) {
            int id = getResources().getIdentifier("wd_" + i, "id", getPackageName());
            TextView wd = findViewById(id);
            if (wd != null) ThemeUtils.applyTextColor(wd, theme, this);
        }

        textSize = prefs.getInt("calendar_font_size", 32);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;
        loadCalendarOptions();
        applyMonthGridVisibility();
        itemsPerPage = calculateCalendarItemsPerPage();

        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedDay = Calendar.getInstance();
        gridDays = new ArrayList<>();
        allEvents = new ArrayList<>();
        agendaEvents = new ArrayList<>();

        monthGrid.setLayoutManager(new GridLayoutManager(this, 7));
        monthGrid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        monthAdapter = new MonthGridAdapter();
        monthGrid.setAdapter(monthAdapter);

        agendaList.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return scrollAppList;
            }
        });
        agendaList.setOverScrollMode(View.OVER_SCROLL_NEVER);
        agendaAdapter = new CalendarAdapter(agendaEvents, this, theme);
        agendaList.setAdapter(agendaAdapter);

        if (!scrollAppList) {
            new SwipePageNavigator(this, agendaList, container,
                    new SwipePageNavigator.PageChangeCallback() {
                        @Override
                        public void onPageChanged(int newPage) {
                            currentPage = newPage;
                            agendaList.getAdapter().notifyDataSetChanged();
                            updatePageIndicator();
                            EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
                        }

                        @Override
                        public int getTotalPages() {
                            return (int) Math.ceil((double) agendaEvents.size() / itemsPerPage);
                        }

                        @Override
                        public void updatePageIndicator() {
                            CalendarActivity.this.updatePageIndicator();
                        }
                    }, theme);
        }

        updateMonthHeader();
        updateWeekdayLabels();
        buildGridDays();
        updatePageIndicator();
        signIn();
    }

    private void shiftMonth(int delta) {
        currentMonth.add(Calendar.MONTH, delta);
        buildGridDays();
        monthAdapter.notifyDataSetChanged();
        updateMonthHeader();
        updateAgendaForSelectedDay();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

    private void updateMonthHeader() {
        TextView monthTitle = findViewById(R.id.month_title);
        if (monthTitle != null) {
            monthTitle.setText(monthTitleFormat.format(currentMonth.getTime()));
            ThemeUtils.applyTextColor(monthTitle, theme, this);
        }
    }

    private void updateWeekdayLabels() {
        String[] labels;
        Calendar cal = Calendar.getInstance();
        int first = cal.getFirstDayOfWeek();
        SimpleDateFormat wdFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        List<String> days = new ArrayList<>();
        Calendar tmp = Calendar.getInstance();
        tmp.set(Calendar.DAY_OF_WEEK, first);
        for (int i = 0; i < 7; i++) {
            String s = wdFormat.format(tmp.getTime());
            if (s.length() > 3) s = s.substring(0, 3);
            days.add(s);
            tmp.add(Calendar.DAY_OF_WEEK, 1);
        }
        for (int i = 0; i < 7; i++) {
            int id = getResources().getIdentifier("wd_" + i, "id", getPackageName());
            TextView wd = findViewById(id);
            if (wd != null) wd.setText(days.get(i));
        }
    }

    private void buildGridDays() {
        gridDays.clear();
        Calendar first = (Calendar) currentMonth.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = first.get(Calendar.DAY_OF_WEEK);
        int firstWeekDay = Calendar.getInstance().getFirstDayOfWeek();
        int offset = (firstDow - firstWeekDay + 7) % 7;
        Calendar start = (Calendar) first.clone();
        start.add(Calendar.DAY_OF_MONTH, -offset);
        for (int i = 0; i < 42; i++) {
            Calendar c = (Calendar) start.clone();
            c.add(Calendar.DAY_OF_MONTH, i);
            gridDays.add(c);
        }
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
        showMonthGrid = prefs.getBoolean("calendar_show_month_grid", false);
        showAccount = prefs.getBoolean("calendar_show_account", false);
        eventLimit = prefs.getInt("calendar_event_limit", 10);
        if (eventLimit != 10 && eventLimit != 25 && eventLimit != 50) {
            eventLimit = 10;
        }
        highlightToday = prefs.getBoolean("calendar_highlight_today", false);
        showMonthSeparators = prefs.getBoolean("calendar_month_separators", false);
        showDaySeparators = prefs.getBoolean("calendar_day_separators", false);
        highlightEventTimes = prefs.getBoolean("calendar_highlight_event_times", false);
        highlightStyle = prefs.getInt("calendar_highlight_style", 0);
    }

    private void applyMonthGridVisibility() {
        View monthHeader = findViewById(R.id.month_header);
        View weekdayHeader = findViewById(R.id.weekday_header);
        View monthGrid = findViewById(R.id.month_grid);
        int vis = showMonthGrid ? View.VISIBLE : View.GONE;
        if (monthHeader != null) monthHeader.setVisibility(vis);
        if (weekdayHeader != null) weekdayHeader.setVisibility(vis);
        if (monthGrid != null) monthGrid.setVisibility(vis);
    }

    private void goToToday() {
        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedDay = Calendar.getInstance();
        buildGridDays();
        if (monthAdapter != null) monthAdapter.notifyDataSetChanged();
        updateMonthHeader();
        updateAgendaForSelectedDay();
        EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
    }

    private void loadEvents() {
        allEvents.clear();
        long now = System.currentTimeMillis();
        long end = now + 365L * 24L * 60L * 60L * 1000L;
        Calendar startCal = (Calendar) currentMonth.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.add(Calendar.MONTH, -1);
        long rangeStart = startCal.getTimeInMillis();
        Calendar endCal = (Calendar) currentMonth.clone();
        endCal.set(Calendar.DAY_OF_MONTH, 1);
        endCal.add(Calendar.MONTH, 2);
        long rangeEnd = endCal.getTimeInMillis();
        long queryStart = Math.min(now, rangeStart);
        long queryEnd = Math.max(end, rangeEnd);
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, queryStart);
        ContentUris.appendId(builder, queryEnd);

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
        String[] args = { String.valueOf(queryStart) };
        String sortOrder = CalendarContract.Instances.BEGIN + " ASC";

        try (Cursor cursor = getContentResolver().query(builder.build(), projection, selection, args, sortOrder)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
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
                    allEvents.add(new CalendarEvent(id, title, begin, eventEnd, allDay, calendarName, ownerAccount));
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Calendar permission is required", Toast.LENGTH_SHORT).show();
        }

        updateAgendaForSelectedDay();
        if (monthAdapter != null) monthAdapter.notifyDataSetChanged();
    }

    private void updateAgendaForSelectedDay() {
        agendaEvents.clear();
        if (!showMonthGrid) {
            for (CalendarEvent e : allEvents) {
                if (agendaEvents.size() < eventLimit) agendaEvents.add(e);
            }
            if (agendaEvents.isEmpty()) agendaEvents.add(CalendarEvent.message("No upcoming events"));
        } else {
            if (selectedDay == null) selectedDay = Calendar.getInstance();
            for (CalendarEvent e : allEvents) {
                if (isSameDay(e, selectedDay) && agendaEvents.size() < eventLimit) {
                    agendaEvents.add(e);
                }
            }
            if (agendaEvents.isEmpty()) {
                boolean hasAny = false;
                for (CalendarEvent e : allEvents) {
                    if (isSameDay(e, selectedDay)) { hasAny = true; break; }
                }
                if (!hasAny) {
                    agendaEvents.add(CalendarEvent.message("No events"));
                }
            }
        }
        currentPage = 0;
        if (agendaAdapter != null) agendaAdapter.notifyDataSetChanged();
        updatePageIndicator();
    }

    private boolean isSameDay(CalendarEvent event, Calendar day) {
        Calendar c = Calendar.getInstance();
        if (event.allDay) c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(event.begin);
        return c.get(Calendar.YEAR) == day.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR);
    }

    private boolean hasEventsForDay(Calendar day) {
        for (CalendarEvent e : allEvents) {
            if (isSameDay(e, day)) return true;
        }
        return false;
    }

    private boolean isToday(Calendar day) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == day.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR);
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
        int totalPages = (int) Math.ceil((double) agendaEvents.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
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
        float recyclerHeightDp = screenHeightDp - 48 - 4 - 48 - 48 - 24 - 48;
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(textSize * scaledDensity);
        float titleHeightDp = (titlePaint.getFontMetrics().bottom - titlePaint.getFontMetrics().top) / density;
        Paint timePaint = new Paint();
        timePaint.setTextSize(Math.max(12, textSize - 8) * scaledDensity);
        float timeHeightDp = (timePaint.getFontMetrics().bottom - timePaint.getFontMetrics().top) / density;
        float separatorHeightDp = (showMonthSeparators || showDaySeparators) ? timeHeightDp + 8 : 0;
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
        if (!showMonthSeparators || position < 0 || position >= agendaEvents.size()) {
            return false;
        }
        CalendarEvent event = agendaEvents.get(position);
        if (event.messageOnly) {
            return false;
        }
        if (position == 0) {
            return true;
        }
        CalendarEvent previous = agendaEvents.get(position - 1);
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

    private boolean shouldShowDaySeparator(int position) {
        if (!showDaySeparators || position < 0 || position >= agendaEvents.size()) {
            return false;
        }
        CalendarEvent event = agendaEvents.get(position);
        if (event.messageOnly) {
            return false;
        }
        if (position == 0) {
            return true;
        }
        CalendarEvent previous = agendaEvents.get(position - 1);
        return previous.messageOnly || !getDayKey(event).equals(getDayKey(previous));
    }

    private String getDayKey(CalendarEvent event) {
        Calendar calendar = Calendar.getInstance();
        if (event.allDay) {
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        calendar.setTimeInMillis(event.begin);
        return calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
    }

    private String formatDaySeparator(CalendarEvent event) {
        if (event.allDay) {
            return daySeparatorFormat.format(new Date(event.begin));
        }
        SimpleDateFormat localDayFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
        return localDayFormat.format(new Date(event.begin));
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
            return "Select a date";
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
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, appLauncherAnimations ? R.anim.dialog_fade_out : 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontHelper.applyToViewTree(this, findViewById(android.R.id.content));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED && agendaAdapter != null) {
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
                allEvents.clear();
                agendaEvents.clear();
                agendaEvents.add(CalendarEvent.message("Calendar permission is required"));
                if (agendaAdapter != null) agendaAdapter.notifyDataSetChanged();
                if (monthAdapter != null) monthAdapter.notifyDataSetChanged();
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

    private class MonthGridAdapter extends RecyclerView.Adapter<MonthGridAdapter.DayHolder> {
        @NonNull
        @Override
        public DayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new DayHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DayHolder holder, int position) {
            Calendar day = gridDays.get(position);
            int dayNum = day.get(Calendar.DAY_OF_MONTH);
            boolean isCurrentMonth = day.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) && day.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR);
            boolean today = isToday(day);
            boolean selected = selectedDay != null && day.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR) == selectedDay.get(Calendar.DAY_OF_YEAR);
            boolean hasEvents = hasEventsForDay(day);
            holder.dayNumber.setText(String.valueOf(dayNum));
            holder.dayNumber.setTextSize(16);
            holder.dayNumber.setTypeface(null, selected || today ? Typeface.BOLD : Typeface.NORMAL);
            int txt = ThemeUtils.getTextColor(theme, CalendarActivity.this);
            int bg = ThemeUtils.getBgColor(theme, CalendarActivity.this);
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            if (selected) {
                d.setColor(txt);
                d.setStroke((int)(2 * getResources().getDisplayMetrics().density), txt);
                int r = DialogEffectHelper.getCornerRadiusPx(CalendarActivity.this);
                if (r > 0) d.setCornerRadius(r);
                holder.dayNumber.setBackground(d);
                holder.dayNumber.setTextColor(bg);
            } else {
                d.setColor(bg);
                d.setStroke((int)(2 * getResources().getDisplayMetrics().density), txt);
                int r = DialogEffectHelper.getCornerRadiusPx(CalendarActivity.this);
                if (r > 0) d.setCornerRadius(r);
                holder.dayNumber.setBackground(d);
                holder.dayNumber.setTextColor(txt);
                holder.dayNumber.setAlpha(1f);
                if (highlightToday && today) {
                    holder.dayNumber.setTypeface(null, Typeface.BOLD);
                }
            }
            holder.eventDot.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
            if (hasEvents) holder.eventDot.getBackground().setTint(txt);
            if (selected) holder.eventDot.getBackground().setTint(bg);
            LauncherBackdropHelper.applySurfaceBackground(holder.itemView, showWallpaperBackdrop, calendarSurfaceColor);
            FontHelper.applyToViewTree(CalendarActivity.this, holder.itemView);
            holder.itemView.setOnClickListener(v -> {
                selectedDay = (Calendar) day.clone();
                monthAdapter.notifyDataSetChanged();
                updateAgendaForSelectedDay();
                EinkRefreshHelper.refreshEink(getWindow(), prefs, prefs.getInt("eink_refresh_delay", 100));
            });
        }

        @Override
        public int getItemCount() {
            return gridDays.size();
        }

        class DayHolder extends RecyclerView.ViewHolder {
            TextView dayNumber;
            View eventDot;
            DayHolder(View itemView) {
                super(itemView);
                dayNumber = itemView.findViewById(R.id.day_number);
                eventDot = itemView.findViewById(R.id.event_dot);
            }
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
            boolean showMonthSep = activity.shouldShowMonthSeparator(globalPosition);
            holder.monthSeparatorView.setVisibility(showMonthSep ? View.VISIBLE : View.GONE);
            if (showMonthSep) {
                holder.monthSeparatorView.setText(activity.formatMonthSeparator(event));
                holder.monthSeparatorView.setTextSize(Math.max(12, activity.textSize - 10));
                holder.monthSeparatorView.setTypeface(null, Typeface.BOLD);
                ThemeUtils.applyTextColor(holder.monthSeparatorView, theme, activity);
            }
            boolean showDaySep = activity.shouldShowDaySeparator(globalPosition);
            holder.daySeparatorView.setVisibility(showDaySep ? View.VISIBLE : View.GONE);
            if (showDaySep) {
                holder.daySeparatorView.setText(activity.formatDaySeparator(event));
                holder.daySeparatorView.setTextSize(Math.max(12, activity.textSize - 10));
                holder.daySeparatorView.setTypeface(null, Typeface.BOLD);
                ThemeUtils.applyTextColor(holder.daySeparatorView, theme, activity);
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
            FontHelper.applyToViewTree(activity, holder.itemView);
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
            TextView daySeparatorView;
            View todayDot;

            ViewHolder(View itemView) {
                super(itemView);
                monthSeparatorView = itemView.findViewById(R.id.month_separator);
                daySeparatorView = itemView.findViewById(R.id.day_separator);
                titleView = itemView.findViewById(R.id.event_title);
                timeView = itemView.findViewById(R.id.event_time);
                todayDot = itemView.findViewById(R.id.today_dot);
            }
        }
    }
}
