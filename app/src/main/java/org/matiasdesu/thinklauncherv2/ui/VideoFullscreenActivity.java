package org.matiasdesu.thinklauncherv2.ui;

import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper;
import org.matiasdesu.thinklauncherv2.utils.FontHelper;
import org.matiasdesu.thinklauncherv2.utils.LauncherBackdropHelper;
import org.matiasdesu.thinklauncherv2.utils.ThemeUtils;

public class VideoFullscreenActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageView playButton;
    private View controls;
    private View seekContainer;
    private View progressView;
    private TextView currentTime;
    private TextView durationTime;
    private ImageView exitButton;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private boolean isDragging;

    private long videoId;
    private int startPosition;
    private boolean startWasPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0);
        int themeRes = LauncherBackdropHelper.resolveThemeResId(this, theme, false);
        setTheme(themeRes);
        super.onCreate(savedInstanceState);
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } catch (Exception ignored) {}
        try {
            Window window = getWindow();
            if (window != null) {
                window.setWindowAnimations(0);
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(false);
                    WindowInsetsController controller = window.getInsetsController();
                    if (controller != null) {
                        controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } else {
                    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    View decor = window.getDecorView();
                    int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                    decor.setSystemUiVisibility(flags);
                }
            }
        } catch (Exception ignored) {}
        setContentView(R.layout.activity_video_fullscreen);
        videoId = getIntent().getLongExtra("video_id", -1);
        startPosition = getIntent().getIntExtra("position", 0);
        startWasPlaying = getIntent().getBooleanExtra("is_playing", false);
        videoView = findViewById(R.id.fullscreen_video_view);
        playButton = findViewById(R.id.fullscreen_play);
        controls = findViewById(R.id.fullscreen_controls);
        seekContainer = findViewById(R.id.fullscreen_seek_container);
        progressView = findViewById(R.id.fullscreen_progress);
        currentTime = findViewById(R.id.fullscreen_current_time);
        durationTime = findViewById(R.id.fullscreen_duration);
        exitButton = findViewById(R.id.fullscreen_exit_button);
        View root = findViewById(R.id.root_layout);
        int bg = ThemeUtils.getBgColor(theme, this);
        int txt = ThemeUtils.getTextColor(theme, this);
        if (root != null) root.setBackgroundColor(bg);
        // Avoid fullscreen controls being cut by bottom navigation bar / gesture inset.
        // We draw edge-to-edge (decorFitsSystemWindows=false), so we must offset the seekbar
        // container by the system bars insets. Listener handles both 3-button and gesture nav.
        try {
            if (root != null) {
                View controlsRef = controls; // capture for lambda (fields are mutable)
                ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                    Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
                    Insets cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
                    // Also consider status bar for landscape cutout, but bottom is primary
                    int base = (int) (8 * getResources().getDisplayMetrics().density);
                    if (controlsRef != null) {
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) controlsRef.getLayoutParams();
                        int bottomExtra = Math.max(navInsets.bottom, cutoutInsets.bottom);
                        int leftExtra = Math.max(navInsets.left, cutoutInsets.left);
                        int rightExtra = Math.max(navInsets.right, cutoutInsets.right);
                        lp.bottomMargin = base + bottomExtra;
                        lp.leftMargin = base + leftExtra;
                        lp.rightMargin = base + rightExtra;
                        controlsRef.setLayoutParams(lp);
                        // Ensure controls are above nav when transient bars appear
                        controlsRef.setTranslationZ(2f);
                    }
                    return windowInsets;
                });
                // Trigger initial inset dispatch
                ViewCompat.requestApplyInsets(root);
            }
        } catch (Exception ignored) {}
        try {
            ThemeUtils.applyButtonBorder(playButton, txt, bg, this);
            playButton.setColorFilter(txt);
        } catch (Exception e) {}
        if (controls != null) {
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setColor(bg);
            d.setStroke((int) (2 * getResources().getDisplayMetrics().density), txt);
            DialogEffectHelper.applyCornerRadius(d, this);
            controls.setBackground(d);
        }
        if (seekContainer != null) {
            android.graphics.drawable.GradientDrawable sd = new android.graphics.drawable.GradientDrawable();
            sd.setColor(bg);
            sd.setStroke((int) (2 * getResources().getDisplayMetrics().density), txt);
            DialogEffectHelper.applyCornerRadius(sd, this);
            seekContainer.setBackground(sd);
            seekContainer.setClipToOutline(true);
        }
        if (progressView != null) progressView.setBackgroundColor(txt);
        if (currentTime != null) currentTime.setTextColor(txt);
        if (durationTime != null) durationTime.setTextColor(txt);
        if (exitButton != null) {
            exitButton.setBackground(null);
            exitButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            exitButton.setColorFilter(txt);
            exitButton.setOnClickListener(v -> finishNoAnim());
        }
        if (seekContainer != null && progressView != null) {
            ViewGroup.LayoutParams lp = progressView.getLayoutParams();
            lp.width = 0;
            progressView.setLayoutParams(lp);
            if (currentTime != null) currentTime.setText("0:00");
            if (durationTime != null) durationTime.setText("0:00");
            seekContainer.setOnTouchListener((v, ev) -> {
                if (videoView.getDuration() <= 0) return false;
                int action = ev.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    isDragging = true;
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    float x = ev.getX();
                    int w = v.getWidth();
                    if (w <= 0) return true;
                    float ratio = Math.max(0f, Math.min(1f, x / w));
                    int pos = (int) (ratio * videoView.getDuration());
                    ViewGroup.LayoutParams p = progressView.getLayoutParams();
                    p.width = (int) (ratio * w);
                    progressView.setLayoutParams(p);
                    if (currentTime != null) currentTime.setText(formatTime(pos));
                    if (action == MotionEvent.ACTION_MOVE) videoView.seekTo(pos);
                    return true;
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    float x = ev.getX();
                    int w = v.getWidth();
                    float ratio = w > 0 ? Math.max(0f, Math.min(1f, x / w)) : 0;
                    int pos = (int) (ratio * videoView.getDuration());
                    videoView.seekTo(pos);
                    if (currentTime != null) currentTime.setText(formatTime(pos));
                    isDragging = false;
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return false;
            });
            if (controls != null) {
                controls.setOnTouchListener((v, ev) -> {
                    int a = ev.getAction();
                    if (a == MotionEvent.ACTION_DOWN) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return false;
                });
            }
        }
        updateRunnable = new Runnable() {
            @Override public void run() {
                if (videoView.isPlaying() && !isDragging && videoView.getDuration() > 0 && seekContainer != null && progressView != null) {
                    int pos = videoView.getCurrentPosition();
                    int dur = videoView.getDuration();
                    float ratio = pos / (float) dur;
                    int w = seekContainer.getWidth();
                    ViewGroup.LayoutParams p = progressView.getLayoutParams();
                    p.width = (int) (ratio * w);
                    progressView.setLayoutParams(p);
                    if (currentTime != null) currentTime.setText(formatTime(pos));
                }
                handler.postDelayed(this, 200);
            }
        };
        handler.post(updateRunnable);
        if (videoId == -1) {
            finishNoAnim();
            return;
        }
        try {
            View.OnClickListener toggle = v -> {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    playButton.setVisibility(View.VISIBLE);
                } else {
                    videoView.start();
                    playButton.setVisibility(View.GONE);
                }
            };
            playButton.setOnClickListener(toggle);
            videoView.setOnClickListener(toggle);
            videoView.setOnPreparedListener(mp -> {
                int dur = videoView.getDuration();
                if (durationTime != null) durationTime.setText(formatTime(dur));
                if (startPosition > 0 && startPosition < dur) {
                    videoView.seekTo(startPosition);
                    if (currentTime != null) currentTime.setText(formatTime(startPosition));
                } else {
                    if (currentTime != null) currentTime.setText(formatTime(videoView.getCurrentPosition()));
                }
                if (startWasPlaying) {
                    videoView.start();
                    playButton.setVisibility(View.GONE);
                } else {
                    videoView.pause();
                    playButton.setVisibility(View.VISIBLE);
                    if (startPosition == 0) videoView.seekTo(1);
                }
            });
            videoView.setOnCompletionListener(mp -> {
                playButton.setVisibility(View.VISIBLE);
                if (seekContainer != null && progressView != null) {
                    ViewGroup.LayoutParams p = progressView.getLayoutParams();
                    p.width = seekContainer.getWidth();
                    progressView.setLayoutParams(p);
                }
                if (currentTime != null && durationTime != null) currentTime.setText(durationTime.getText());
            });
            Uri uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);
            videoView.setVideoURI(uri);
        } catch (Exception e) {
            finishNoAnim();
        }
    }

    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        if (h > 0) return String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        return String.format(java.util.Locale.getDefault(), "%d:%02d", m, s);
    }

    private void finishNoAnim() {
        try {
            Intent data = new Intent();
            data.putExtra("video_id", videoId);
            int pos = startPosition;
            boolean playing = startWasPlaying;
            if (videoView != null) {
                try { pos = videoView.getCurrentPosition(); } catch (Exception ignored) {}
                try { playing = videoView.isPlaying(); } catch (Exception ignored) {}
            }
            data.putExtra("position", pos);
            data.putExtra("is_playing", playing);
            setResult(RESULT_OK, data);
        } catch (Exception ignored) {}
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        finishNoAnim();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FontHelper.applyToViewTree(this, findViewById(android.R.id.content));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            View decor = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decor.setSystemUiVisibility(flags);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (videoView != null && videoView.isPlaying()) videoView.pause();
        } catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
        try {
            if (videoView != null) videoView.stopPlayback();
        } catch (Exception e) {}
    }
}
