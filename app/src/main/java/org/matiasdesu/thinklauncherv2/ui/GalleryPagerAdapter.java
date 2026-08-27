package org.matiasdesu.thinklauncherv2.ui;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Handler;
import android.os.Looper;
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

    private final ArrayList<Long> imageIds;
    private final ArrayList<Integer> mediaTypes;
    private final ExecutorService loadExecutor;

    public GalleryPagerAdapter(ArrayList<Long> imageIds, ExecutorService loadExecutor) {
        this.imageIds = imageIds;
        this.mediaTypes = null;
        this.loadExecutor = loadExecutor;
    }

    public GalleryPagerAdapter(ArrayList<Long> imageIds, ArrayList<Integer> mediaTypes, ExecutorService loadExecutor) {
        this.imageIds = imageIds;
        this.mediaTypes = mediaTypes;
        this.loadExecutor = loadExecutor;
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

    static class PageViewHolder extends RecyclerView.ViewHolder {

        private final ZoomableImageView imageView;
        private final VideoView videoView;
        private final ImageView playButton;
        private final View videoControls;
        private final View seekContainer;
        private final View progressView;
        private final TextView currentTime;
        private final TextView durationTime;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable updateRunnable;
        private boolean isDragging;

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
            videoView.setVisibility(View.GONE);
            playButton.setVisibility(View.GONE);
            if (videoControls != null) videoControls.setVisibility(View.GONE);
            if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
            videoView.stopPlayback();
            imageView.setVisibility(View.VISIBLE);
            executor.execute(() -> {
                try {
                    Uri uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId);
                    Bitmap rawBitmap = BitmapFactory.decodeStream(
                            itemView.getContext().getContentResolver().openInputStream(uri));
                    final Bitmap bitmap = (rawBitmap != null)
                            ? applyExifOrientation(itemView.getContext(), uri, rawBitmap)
                            : null;
                    if (bitmap != null) {
                        imageView.post(() -> {
                            imageView.setImageBitmap(bitmap);
                            imageView.resetZoom();
                        });
                    }
                } catch (Exception e) {
                }
            });
        }

        void loadVideo(long videoId) {
            imageView.setVisibility(View.GONE);
            imageView.setImageBitmap(null);
            videoView.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.VISIBLE);
            if (videoControls != null) videoControls.setVisibility(View.VISIBLE);
            if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
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
                if (videoControls != null) {
                    android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
                    d.setColor(bg);
                    d.setStroke((int) (1 * ctx.getResources().getDisplayMetrics().density), txt);
                    org.matiasdesu.thinklauncherv2.utils.DialogEffectHelper.applyCornerRadius(d, ctx);
                    videoControls.setBackground(d);
                }
                if (seekContainer != null) {
                    android.graphics.drawable.GradientDrawable sd = new android.graphics.drawable.GradientDrawable();
                    sd.setColor(bg);
                    sd.setStroke((int) (1 * ctx.getResources().getDisplayMetrics().density), txt);
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
                    if (action == MotionEvent.ACTION_DOWN) isDragging = true;
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
                        return true;
                    }
                    return false;
                });
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
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);
                videoView.setVideoURI(uri);
                videoView.seekTo(1);
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
                    if (currentTime != null) currentTime.setText(formatTime(videoView.getCurrentPosition()));
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
