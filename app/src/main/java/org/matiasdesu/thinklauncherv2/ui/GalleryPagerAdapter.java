package org.matiasdesu.thinklauncherv2.ui;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.matiasdesu.thinklauncherv2.R;
import org.matiasdesu.thinklauncherv2.views.ZoomableImageView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class GalleryPagerAdapter extends RecyclerView.Adapter<GalleryPagerAdapter.PageViewHolder> {

    private final ArrayList<Long> imageIds;
    private final ExecutorService loadExecutor;

    public GalleryPagerAdapter(ArrayList<Long> imageIds, ExecutorService loadExecutor) {
        this.imageIds = imageIds;
        this.loadExecutor = loadExecutor;
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
        holder.loadImage(imageId, loadExecutor);
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

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.page_image_view);
        }

        void loadImage(long imageId, ExecutorService executor) {
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
                    // Silently fail - image couldn't be loaded
                }
            });
        }

        void recycle() {
            imageView.setImageBitmap(null);
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
