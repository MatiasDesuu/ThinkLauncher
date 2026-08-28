package org.matiasdesu.thinklauncherv2.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.utils.GalleryTrashHelper;
import org.matiasdesu.thinklauncherv2.views.ZoomableImageView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class GalleryPagerAdapter extends RecyclerView.Adapter<GalleryPagerAdapter.PageViewHolder> {

    public interface FullscreenCallback {
        void openFullscreen(long videoId, int position, boolean isPlaying);
    }

    private final ArrayList<Long> imageIds;
    private final ArrayList<Integer> mediaTypes;
    private final ExecutorService loadExecutor;
    private final FullscreenCallback fullscreenCallback;
    private long pendingVideoId = -1;
    private int pendingPosition = 0;
    private boolean pendingIsPlaying = false;

    public GalleryPagerAdapter(ArrayList<Long> imageIds, ExecutorService loadExecutor) {
        this.imageIds = imageIds;
        this.mediaTypes = null;
        this.loadExecutor = loadExecutor;
        this.fullscreenCallback = null;
    }

    public GalleryPagerAdapter(ArrayList<Long> imageIds, ArrayList<Integer> mediaTypes, ExecutorService loadExecutor) {
        this.imageIds = imageIds;
        this.mediaTypes = mediaTypes;
        this.loadExecutor = loadExecutor;
        this.fullscreenCallback = null;
    }

    public GalleryPagerAdapter(ArrayList<Long> imageIds, ArrayList<Integer> mediaTypes, ExecutorService loadExecutor, FullscreenCallback callback) {
        this.imageIds = imageIds;
        this.mediaTypes = mediaTypes;
        this.loadExecutor = loadExecutor;
        this.fullscreenCallback = callback;
    }

    public void setPendingFullscreenResult(long videoId, int position, boolean isPlaying) {
        pendingVideoId = videoId;
        pendingPosition = position;
        pendingIsPlaying = isPlaying;
    }

    private int getMediaType(int position) {
        if (mediaTypes != null && position < mediaTypes.size()) return mediaTypes.get(position);
        return GalleryTrashHelper.TYPE_IMAGE;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        long imageId = imageIds.get(position);
        int type = getMediaType(position);
        if (type == GalleryTrashHelper.TYPE_VIDEO) holder.loadVideo(imageId);
        else holder.loadImage(imageId, loadExecutor);
    }

    @Override
    public int getItemCount() {
        return imageIds.size();
    }

    @Override
    public void onViewRecycled(@NonNull PageViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    class PageViewHolder extends RecyclerView.ViewHolder {

        private final ZoomableImageView imageView;
        private final VideoView videoView;
        private final ImageView playButton;
        private final View videoControls;
        private final View seekContainer;
        private final View progressView;
        private final TextView currentTime;
        private final TextView durationTime;
        private final ImageView fullscreenButton;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable updateRunnable;
        private boolean isDragging;
        long currentVideoId = -1;
        long currentImageId = -1;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.page_image_view);
            videoView = itemView.findViewById(R.id.page_video_view);
            playButton = itemView.findViewById(R.id.page_video_play);
            videoControls = itemView.findViewById(R.id.video_controls);
            seekContainer = itemView.findViewById(R.id.video_seek_container);
            progressView = itemView.findViewById(R.id.video_progress);
            currentTime = itemView.findViewById(R.id.video_current_time);
            durationTime = itemView.findViewById(R.id.video_duration);
            fullscreenButton = itemView.findViewById(R.id.video_fullscreen_button);
        }

        void restoreFromFullscreen(int position, boolean isPlaying) {
            if (videoView == null) return;
            try {
                int dur = 0;
                try { dur = videoView.getDuration(); } catch (Exception ignored) {}
                if (dur <= 0) {
                    videoView.postDelayed(() -> restoreFromFullscreen(position, isPlaying), 120);
                    return;
                }
                videoView.seekTo(position);
                if (currentTime != null) currentTime.setText(formatTime(position));
                if (seekContainer != null && progressView != null) {
                    if (dur > 0) {
                        int w = seekContainer.getWidth();
                        final int fPos = position;
                        final int fDur = dur;
                        if (w == 0) {
                            seekContainer.post(() -> {
                                int ww = seekContainer.getWidth();
                                if (ww > 0) {
                                    float ratio = fPos / (float) fDur;
                                    ViewGroup.LayoutParams p = progressView.getLayoutParams();
                                    p.width = (int) (ratio * ww);
                                    progressView.setLayoutParams(p);
                                }
                            });
                        } else {
                            float ratio = fPos / (float) fDur;
                            ViewGroup.LayoutParams p = progressView.getLayoutParams();
                            p.width = (int) (ratio * w);
                            progressView.setLayoutParams(p);
                        }
                    }
                    if (durationTime != null && videoView.getDuration() > 0) {
                        durationTime.setText(formatTime(videoView.getDuration()));
                    }
                }
                if (videoControls != null) videoControls.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.VISIBLE);
                final int fPos2 = position;
                if (isPlaying) {
                    videoView.start();
                    playButton.setVisibility(View.GONE);
                } else {
                    try { videoView.pause(); } catch (Exception ignored) {}
                    playButton.setVisibility(View.VISIBLE);
                    videoView.postDelayed(() -> {
                        try { videoView.seekTo(fPos2); } catch (Exception ignored) {}
                    }, 50);
                }
            } catch (Exception ignored) {}
        }

        private String formatTime(int ms) {
            int totalSec = ms / 1000;
            int h = totalSec / 3600;
            int m = (totalSec % 3600) / 60;
            int s = totalSec % 60;
            if (h > 0) return String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, m, s);
            return String.format(java.util.Locale.getDefault(), "%d:%02d", m, s);
        }

        void loadImage(long imageId, ExecutorService executor) {
            currentImageId = imageId;
            currentVideoId = -1;
            videoView.setVisibility(View.GONE);
            playButton.setVisibility(View.GONE);
            if (videoControls != null) videoControls.setVisibility(View.GONE);
            if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
            videoView.stopPlayback();
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(null);
            imageView.resetZoom();
            final long targetId = imageId;
            executor.execute(() -> {
                try {
                    Bitmap thumb = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        thumb = itemView.getContext().getContentResolver().loadThumbnail(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, targetId), new android.util.Size(512, 512), null);
                    } else {
                        thumb = MediaStore.Images.Thumbnails.getThumbnail(itemView.getContext().getContentResolver(), targetId, MediaStore.Images.Thumbnails.MINI_KIND, null);
                    }
                    if (thumb != null && currentImageId == targetId) {
                        Bitmap finalThumb = thumb;
                        imageView.post(() -> {
                            if (currentImageId == targetId) {
                                imageView.setImageBitmap(finalThumb);
                                imageView.resetZoom();
                            }
                        });
                    }
                } catch (Exception ignored) {}
                try {
                    Uri uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, targetId);
                    Bitmap rawBitmap = BitmapFactory.decodeStream(
                            itemView.getContext().getContentResolver().openInputStream(uri));
                    final Bitmap bitmap = (rawBitmap != null)
                            ? applyExifOrientation(itemView.getContext(), uri, rawBitmap)
                            : null;
                    if (bitmap != null && currentImageId == targetId) {
                        imageView.post(() -> {
                            if (currentImageId == targetId) {
                                imageView.setImageBitmap(bitmap);
                                imageView.resetZoom();
                            }
                        });
                    }
                } catch (Exception ignored) {}
            });
        }

        void loadVideo(long videoId) {
            currentVideoId = videoId;
            currentImageId = -1;
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(null);
            imageView.resetZoom();
            videoView.setVisibility(View.GONE);
            playButton.setVisibility(View.VISIBLE);
            if (videoControls != null) videoControls.setVisibility(View.VISIBLE);
            if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
            videoView.stopPlayback();
            final long thumbId = videoId;
            try {
                loadExecutor.execute(() -> {
                    try {
                        Bitmap thumb = null;
                        android.content.Context c = itemView.getContext();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            thumb = c.getContentResolver().loadThumbnail(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, thumbId), new android.util.Size(512, 512), null);
                        } else {
                            thumb = MediaStore.Video.Thumbnails.getThumbnail(c.getContentResolver(), thumbId, MediaStore.Video.Thumbnails.MINI_KIND, null);
                        }
                        if (thumb != null && currentVideoId == thumbId) {
                            Bitmap finalThumb = thumb;
                            imageView.post(() -> {
                                if (currentVideoId == thumbId) {
                                    imageView.setImageBitmap(finalThumb);
                                    imageView.resetZoom();
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
            try {
                android.content.Context ctx = itemView.getContext();
                android.content.SharedPreferences prefs = ctx.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE);
                int theme = prefs.getInt("theme", 0);
                int bg = org.matiasdesu.thinklauncherv2.utils.ThemeUtils.getBgColor(theme, ctx);
                int txt = org.matiasdesu.thinklauncherv2.utils.ThemeUtils.getTextColor(theme, ctx);
                org.matiasdesu.thinklauncherv2.utils.ThemeUtils.applyButtonBorder(playButton, txt, bg, ctx);
                playButton.setColorFilter(txt);
                if (currentTime != null) currentTime.setTextColor(txt);
                if (durationTime != null) durationTime.setTextColor(txt);
                if (fullscreenButton != null) {
                    fullscreenButton.setBackground(null);
                    fullscreenButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    fullscreenButton.setColorFilter(txt);
                }
                if (videoControls != null) {
                    android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
                    d.setColor(bg);
                    d.setStroke((int) (2 * ctx.getResources().getDisplayMetrics().density), txt);
                    org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.applyCornerRadius(d, ctx);
                    videoControls.setBackground(d);
                }
                if (seekContainer != null) {
                    android.graphics.drawable.GradientDrawable sd = new android.graphics.drawable.GradientDrawable();
                    sd.setColor(bg);
                    sd.setStroke((int) (2 * ctx.getResources().getDisplayMetrics().density), txt);
                    org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.applyCornerRadius(sd, ctx);
                    seekContainer.setBackground(sd);
                    seekContainer.setClipToOutline(true);
                }
                if (progressView != null) progressView.setBackgroundColor(txt);
            } catch (Exception e) {
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
                        if (itemView.getParent() != null) itemView.getParent().requestDisallowInterceptTouchEvent(true);
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
                        if (itemView.getParent() != null) itemView.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    }
                    return false;
                });
                if (videoControls != null) {
                    videoControls.setOnTouchListener((v, ev) -> {
                        int a = ev.getAction();
                        if (a == MotionEvent.ACTION_DOWN) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            if (itemView.getParent() != null) itemView.getParent().requestDisallowInterceptTouchEvent(true);
                        } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            if (itemView.getParent() != null) itemView.getParent().requestDisallowInterceptTouchEvent(false);
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
                    if (pendingVideoId == videoId) {
                        int pPos = pendingPosition;
                        boolean pPlay = pendingIsPlaying;
                        pendingVideoId = -1;
                        try {
                            if (pPos >= 0 && pPos < dur) {
                                videoView.seekTo(pPos);
                                if (currentTime != null) currentTime.setText(formatTime(pPos));
                            } else {
                                if (currentTime != null) currentTime.setText(formatTime(videoView.getCurrentPosition()));
                            }
                            if (pPlay) {
                                videoView.start();
                                playButton.setVisibility(View.GONE);
                            } else {
                                videoView.pause();
                                playButton.setVisibility(View.VISIBLE);
                            }
                            if (seekContainer != null && progressView != null && dur > 0) {
                                int w = seekContainer.getWidth();
                                if (w > 0) {
                                    float ratio = pPos / (float) dur;
                                    ViewGroup.LayoutParams pp = progressView.getLayoutParams();
                                    pp.width = (int) (ratio * w);
                                    progressView.setLayoutParams(pp);
                                }
                            }
                            if (videoControls != null) videoControls.setVisibility(View.VISIBLE);
                            playButton.setVisibility(pPlay ? View.GONE : View.VISIBLE);
                            imageView.postDelayed(() -> {
                                if (currentVideoId == videoId) {
                                    imageView.setVisibility(View.GONE);
                                    imageView.setImageBitmap(null);
                                    videoView.setVisibility(View.VISIBLE);
                                }
                            }, 80);
                        } catch (Exception ignored) {}
                    } else {
                        if (currentTime != null) currentTime.setText(formatTime(0));
                        try { videoView.seekTo(1); } catch (Exception ignored) {}
                        playButton.setVisibility(View.VISIBLE);
                        if (seekContainer != null && progressView != null) {
                            ViewGroup.LayoutParams p = progressView.getLayoutParams();
                            p.width = 0;
                            progressView.setLayoutParams(p);
                        }
                        if (videoControls != null) videoControls.setVisibility(View.VISIBLE);
                        imageView.postDelayed(() -> {
                            if (currentVideoId == videoId) {
                                imageView.setVisibility(View.GONE);
                                imageView.setImageBitmap(null);
                                videoView.setVisibility(View.VISIBLE);
                            }
                        }, 80);
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
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);
                videoView.setVideoURI(uri);
                if (fullscreenButton != null) {
                    fullscreenButton.setOnClickListener(v -> {
                        try {
                            int pos = 0;
                            boolean playing = false;
                            try { pos = videoView.getCurrentPosition(); } catch (Exception ignored) {}
                            try { playing = videoView.isPlaying(); } catch (Exception ignored) {}
                            if (fullscreenCallback != null) {
                                fullscreenCallback.openFullscreen(videoId, pos, playing);
                            } else {
                                android.content.Context c = itemView.getContext();
                                Intent intent = new Intent(c, VideoFullscreenActivity.class);
                                intent.putExtra("video_id", videoId);
                                intent.putExtra("position", pos);
                                intent.putExtra("is_playing", playing);
                                if (!(c instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                c.startActivity(intent);
                                if (c instanceof Activity) ((Activity) c).overridePendingTransition(0, 0);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("GalleryPager", "fullscreen launch failed", e);
                        }
                    });
                }
            } catch (Exception e) {
            }
        }

        void recycle() {
            imageView.setImageBitmap(null);
            if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
            videoView.stopPlayback();
            videoView.setVisibility(View.GONE);
            playButton.setVisibility(View.GONE);
            if (videoControls != null) videoControls.setVisibility(View.GONE);
            if (fullscreenButton != null) fullscreenButton.setOnClickListener(null);
            currentVideoId = -1;
            currentImageId = -1;
            imageView.setVisibility(View.VISIBLE);
        }

        private Bitmap applyExifOrientation(android.content.Context context, Uri uri, Bitmap bitmap) {
            try {
                android.content.ContentResolver resolver = context.getContentResolver();
                android.database.Cursor cursor = resolver.query(uri,
                        new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                String filePath = null;
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(0);
                    }
                    cursor.close();
                }
                if (filePath == null) return bitmap;
                ExifInterface exif = new ExifInterface(filePath);
                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotation = 0;
                boolean flip = false;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotation = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotation = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotation = 270;
                        break;
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        flip = true;
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        rotation = 180;
                        flip = true;
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        rotation = 90;
                        flip = true;
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        rotation = 270;
                        flip = true;
                        break;
                    default:
                        return bitmap;
                }
                Matrix matrix = new Matrix();
                if (rotation != 0) {
                    matrix.postRotate(rotation);
                }
                if (flip) {
                    matrix.postScale(-1, 1, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
                }
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                return rotated;
            } catch (Exception e) {
                return bitmap;
            }
        }
    }
}
