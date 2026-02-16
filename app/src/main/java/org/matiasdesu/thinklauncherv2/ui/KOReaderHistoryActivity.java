package org.matiasdesu.thinklauncherv2.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.AppListSizeHelper;
import org.matiasdesu.thinklauncherv2.utils.EinkRefreshHelper;
import org.matiasdesu.thinklauncherv2.utils.KOReaderHistoryHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;
import org.matiasdesu.thinklauncherv2.ui.SwipePageNavigator;

import java.io.File;
import android.os.StrictMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KOReaderHistoryActivity extends AppCompatActivity {

    private int textSize;
    private boolean boldText;
    private List<KOReaderHistoryHelper.BookItem> books;
    private int itemsPerPage;
    private int currentPage = 0;
    private int theme;
    private HistoryAdapter historyAdapter;
    private LinearLayout rootLayout;
    private SharedPreferences prefs;
    private boolean scrollAppList;
    private boolean folderPickerRequested = false;

    private BroadcastReceiver homeButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                String reason = intent.getStringExtra("reason");
                if ("homekey".equals(reason)) {
                    Intent mainIntent = new Intent(KOReaderHistoryActivity.this, MainActivity.class);
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
        if (ThemeUtils.isDarkTheme(theme, this)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_koreader_history);

        int bgColor = ThemeUtils.getBgColor(theme, this);
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
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(bgColor);
        }

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        registerReceiver(homeButtonReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"),
                Context.RECEIVER_NOT_EXPORTED);

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));
        View bottomDivider = findViewById(R.id.bottom_divider);
        bottomDivider.setBackgroundColor(ThemeUtils.getTextColor(theme, this));

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ThemeUtils.applyDialogBackground(root, theme, this);
        }

        TextView titleView = findViewById(R.id.history_title);
        ThemeUtils.applyTextColor(titleView, theme, this);
        titleView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, 1002);
        });

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        ImageView openKOReaderButton = findViewById(R.id.open_koreader_button);
        openKOReaderButton.setColorFilter(ThemeUtils.getTextColor(theme, this));
        openKOReaderButton.setOnClickListener(v -> {
            String pkg = "org.koreader.launcher";
            Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null) {
                pkg = "org.koreader.launcher.fdroid";
                intent = getPackageManager().getLaunchIntentForPackage(pkg);
            }

            if (intent != null) {
                startActivity(intent);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 500);
            } else {
                Toast.makeText(this, "KOReader not found", Toast.LENGTH_SHORT).show();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.history_list);
        ThemeUtils.applyBackgroundColor(recyclerView, theme, this);

        textSize = prefs.getInt("text_size", 24);
        boldText = prefs.getBoolean("bold_text", true);
        scrollAppList = prefs.getInt("scroll_app_list", 0) == 1;

        itemsPerPage = AppListSizeHelper.calculateItemsPerPage(this, textSize);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        books = new ArrayList<>();

        historyAdapter = new HistoryAdapter(books, this, theme);
        recyclerView.setAdapter(historyAdapter);

        View container = findViewById(R.id.app_list_container);
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
                            return (int) Math.ceil((double) books.size() / itemsPerPage);
                        }

                        @Override
                        public void updatePageIndicator() {
                            KOReaderHistoryActivity.this.updatePageIndicator();
                        }
                    }, theme);
        }

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
        int totalPages = (int) Math.ceil((double) books.size() / itemsPerPage);
        if (totalPages == 0)
            totalPages = 1;
        pageIndicator.setText((currentPage + 1) + " / " + totalPages);
        ThemeUtils.applyTextColor(pageIndicator, theme, this);
    }

    public void openBook(KOReaderHistoryHelper.BookItem book) {
        File file = new File(book.path);
        if (!file.exists()) {
            Toast.makeText(this, "File not found: " + book.path, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            uri = Uri.fromFile(file);
        } else {
            uri = Uri.fromFile(file);
        }

        String mimeType = "*/*";
        String lowerPath = book.path.toLowerCase();
        if (lowerPath.endsWith(".pdf"))
            mimeType = "application/pdf";
        else if (lowerPath.endsWith(".epub"))
            mimeType = "application/epub+zip";
        else if (lowerPath.endsWith(".mobi"))
            mimeType = "application/x-mobipocket-ebook";
        else if (lowerPath.endsWith(".fb2"))
            mimeType = "application/x-fictionbook+xml";
        else if (lowerPath.endsWith(".cbz"))
            mimeType = "application/x-cbz";

        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String pkg = "org.koreader.launcher";
        intent.setPackage(pkg);

        if (intent.resolveActivity(getPackageManager()) == null) {
            pkg = "org.koreader.launcher.fdroid";
            intent.setPackage(pkg);
        }

        try {
            startActivity(intent);
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 500);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open KOReader: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
                return;
            }
        }

        loadHistory();
        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }
        updatePageIndicator();
    }

    private void requestManageStoragePermission() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
        Toast.makeText(this, "Grant permission to read KOReader history", Toast.LENGTH_LONG).show();
    }

    private void loadHistory() {
        if (books == null)
            books = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return;
            }
        }

        String customPath = prefs.getString("koreader_path", null);

        if (customPath == null || customPath.isEmpty()) {
            books.clear();
            if (!folderPickerRequested) {
                folderPickerRequested = true;
                Toast.makeText(this, "Select KOReader directory", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, 1002);
            }
            return;
        }

        books.clear();
        books.addAll(KOReaderHistoryHelper.getRecentBooks(customPath));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = getPathFromUri(uri);
                if (path != null) {
                    prefs.edit().putString("koreader_path", path).apply();
                    loadHistory();
                    historyAdapter.notifyDataSetChanged();
                    updatePageIndicator();
                    Toast.makeText(this, "KOReader path updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Could not resolve folder path", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        if (uri == null)
            return null;
        try {
            String docId;
            if (DocumentsContract.isTreeUri(uri)) {
                docId = DocumentsContract.getTreeDocumentId(uri);
            } else {
                docId = DocumentsContract.getDocumentId(uri);
            }

            String[] split = docId.split(":");
            String type = split[0];
            String relativePath = split.length > 1 ? split[1] : "";

            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + "/" + relativePath;
            } else {

                File file = new File("/storage/" + type + "/" + relativePath);
                if (file.exists()) {
                    return file.getAbsolutePath();
                }

                File storage = new File("/storage");
                File[] files = storage.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().equalsIgnoreCase(type)) {
                            return new File(f, relativePath).getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Error resolving path from URI
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(homeButtonReceiver);
        } catch (Exception e) {
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<KOReaderHistoryHelper.BookItem> items;
        private KOReaderHistoryActivity activity;
        private int theme;

        public HistoryAdapter(List<KOReaderHistoryHelper.BookItem> items, KOReaderHistoryActivity activity, int theme) {
            this.items = items;
            this.activity = activity;
            this.theme = theme;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            int globalPosition = activity.scrollAppList ? position : currentPage * itemsPerPage + position;
            if (globalPosition >= items.size())
                return;

            KOReaderHistoryHelper.BookItem book = items.get(globalPosition);
            holder.textView.setText(book.title);
            holder.textView.setTextSize(activity.textSize);
            holder.textView.setTypeface(null, activity.boldText ? Typeface.BOLD : Typeface.NORMAL);
            ThemeUtils.applyBackgroundColor(holder.itemView, theme, activity);
            ThemeUtils.applyTextColor(holder.textView, theme, activity);
            holder.itemView.setOnClickListener(v -> activity.openBook(book));
        }

        @Override
        public int getItemCount() {
            if (activity.scrollAppList)
                return items.size();
            int start = currentPage * itemsPerPage;
            return Math.min(itemsPerPage, items.size() - start);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.app_name);
            }
        }
    }
}
