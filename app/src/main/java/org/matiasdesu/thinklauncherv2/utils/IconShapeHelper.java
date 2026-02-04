package org.matiasdesu.thinklauncherv2.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.content.Context;

/**
 * Helper class to create icon shapes for adaptive icons.
 * Provides various shape masks that can be applied to icons.
 */
public class IconShapeHelper {

    // Shape constants
    public static final int SHAPE_SYSTEM = 0;      // Use system default shape
    public static final int SHAPE_CIRCLE = 1;      // Circle
    public static final int SHAPE_SQUARE = 2;      // Square with rounded corners
    public static final int SHAPE_SQUIRCLE = 3;    // Squircle (superellipse)
    public static final int SHAPE_TEARDROP = 4;    // Teardrop
    public static final int SHAPE_CYLINDER = 5;    // Cylinder/Rounded rectangle
    public static final int SHAPE_SHARP_SQUARE = 6; // Perfect square (no rounding)

    public static final String[] SHAPE_NAMES = {
        "System",
        "Circle", 
        "Rounded",
        "Squircle",
        "Teardrop",
        "Cylinder",
        "Square"
    };

    public static final int SHAPE_COUNT = SHAPE_NAMES.length;

    /**
     * Get the name of a shape by its index
     */
    public static String getShapeName(int shapeIndex) {
        if (shapeIndex >= 0 && shapeIndex < SHAPE_NAMES.length) {
            return SHAPE_NAMES[shapeIndex];
        }
        return SHAPE_NAMES[0];
    }

    /**
     * Get the next shape index (cycling)
     */
    public static int getNextShape(int currentShape) {
        return (currentShape + 1) % SHAPE_COUNT;
    }

    /**
     * Get the previous shape index (cycling)
     */
    public static int getPreviousShape(int currentShape) {
        return (currentShape - 1 + SHAPE_COUNT) % SHAPE_COUNT;
    }

    /**
     * Create a path for the specified shape
     * @param shape The shape constant
     * @param size The size of the icon (width and height)
     * @return Path for the shape
     */
    public static Path createShapePath(int shape, float size) {
        Path path = new Path();
        float centerX = size / 2f;
        float centerY = size / 2f;
        float radius = size / 2f;
        
        switch (shape) {
            case SHAPE_CIRCLE:
                path.addCircle(centerX, centerY, radius, Path.Direction.CW);
                break;
                
            case SHAPE_SQUARE:
                // Square with rounded corners (8% radius)
                float cornerRadius = size * 0.08f;
                RectF squareRect = new RectF(0, 0, size, size);
                path.addRoundRect(squareRect, cornerRadius, cornerRadius, Path.Direction.CW);
                break;
                
            case SHAPE_SQUIRCLE:
                // Squircle (superellipse approximation)
                createSquirclePath(path, centerX, centerY, radius);
                break;
                
            case SHAPE_TEARDROP:
                // Teardrop shape (rounded on 3 corners, pointed on one)
                createTeardropPath(path, size);
                break;
                
            case SHAPE_CYLINDER:
                // Cylinder/Rounded rectangle (horizontal)
                float cylinderCornerRadius = size * 0.25f;
                RectF cylinderRect = new RectF(0, size * 0.1f, size, size * 0.9f);
                path.addRoundRect(cylinderRect, cylinderCornerRadius, cylinderCornerRadius, Path.Direction.CW);
                break;
                
            case SHAPE_SHARP_SQUARE:
                // Perfect square with no rounding
                path.addRect(0, 0, size, size, Path.Direction.CW);
                break;
                
            case SHAPE_SYSTEM:
            default:
                // Full square - will use system mask
                path.addRect(0, 0, size, size, Path.Direction.CW);
                break;
        }
        
        return path;
    }

    /**
     * Create a squircle path (superellipse with n=4)
     */
    private static void createSquirclePath(Path path, float centerX, float centerY, float radius) {
        // Squircle approximation using cubic bezier curves
        // This creates a shape between a circle and a square
        float n = 0.85f; // Control point factor for squircle appearance
        float controlDistance = radius * n;
        
        path.moveTo(centerX, centerY - radius); // Top
        
        // Top-right curve
        path.cubicTo(
            centerX + controlDistance, centerY - radius,
            centerX + radius, centerY - controlDistance,
            centerX + radius, centerY
        );
        
        // Bottom-right curve
        path.cubicTo(
            centerX + radius, centerY + controlDistance,
            centerX + controlDistance, centerY + radius,
            centerX, centerY + radius
        );
        
        // Bottom-left curve
        path.cubicTo(
            centerX - controlDistance, centerY + radius,
            centerX - radius, centerY + controlDistance,
            centerX - radius, centerY
        );
        
        // Top-left curve
        path.cubicTo(
            centerX - radius, centerY - controlDistance,
            centerX - controlDistance, centerY - radius,
            centerX, centerY - radius
        );
        
        path.close();
    }

    /**
     * Create a teardrop path (rounded corners except top-left)
     */
    private static void createTeardropPath(Path path, float size) {
        float cornerRadius = size * 0.35f;
        float smallCorner = size * 0.05f; // Very small corner for the pointed corner
        
        // Start from top-left (the pointed corner)
        path.moveTo(smallCorner, 0);
        
        // Top edge to top-right corner
        path.lineTo(size - cornerRadius, 0);
        path.quadTo(size, 0, size, cornerRadius);
        
        // Right edge to bottom-right corner
        path.lineTo(size, size - cornerRadius);
        path.quadTo(size, size, size - cornerRadius, size);
        
        // Bottom edge to bottom-left corner
        path.lineTo(cornerRadius, size);
        path.quadTo(0, size, 0, size - cornerRadius);
        
        // Left edge back to top-left (pointed corner)
        path.lineTo(0, smallCorner);
        path.quadTo(0, 0, smallCorner, 0);
        
        path.close();
    }

    /**
     * Apply a shape mask to a drawable
     * @param context Application context
     * @param drawable The drawable to mask
     * @param shape The shape to apply
     * @param size The output size
     * @return A new drawable with the shape applied
     */
    public static Drawable applyShapeMask(Context context, Drawable drawable, int shape, int size) {
        if (shape == SHAPE_SYSTEM || drawable == null || size <= 0) {
            return drawable;
        }
        
        // Create bitmap from drawable
        Bitmap sourceBitmap = drawableToBitmap(drawable, size);
        if (sourceBitmap == null) {
            return drawable;
        }
        
        // Create output bitmap with transparency
        Bitmap outputBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        
        // Create the shape path
        Path shapePath = createShapePath(shape, size);
        
        // Draw the shape as mask
        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(shapePath, maskPaint);
        
        // Apply source-in to clip the icon to the shape
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sourceBitmap, 0, 0, iconPaint);
        
        // Clean up
        sourceBitmap.recycle();
        
        return new BitmapDrawable(context.getResources(), outputBitmap);
    }

    /**
     * Create a shaped icon with foreground and background
     * This creates the icon from scratch with the specified shape, bypassing system mask
     * @param context Application context
     * @param foreground The foreground drawable (the icon itself)
     * @param backgroundColor The background color
     * @param shape The shape to apply
     * @param size The output size
     * @return A new drawable with the shape applied
     */
    public static Drawable createShapedIcon(Context context, Drawable foreground, int backgroundColor, int shape, int size) {
        return createShapedIcon(context, foreground, backgroundColor, shape, size, true);
    }

    /**
     * Create a shaped icon with foreground and background
     * This creates the icon from scratch with the specified shape, bypassing system mask
     * @param context Application context
     * @param foreground The foreground drawable (the icon itself)
     * @param backgroundColor The background color
     * @param shape The shape to apply
     * @param size The output size
     * @param applyInset Whether to apply inset to foreground (false for monochrome icons that already have inset)
     * @return A new drawable with the shape applied
     */
    public static Drawable createShapedIcon(Context context, Drawable foreground, int backgroundColor, int shape, int size, boolean applyInset) {
        if (foreground == null || size <= 0) {
            return foreground;
        }
        
        // If system shape, let the system handle it
        if (shape == SHAPE_SYSTEM) {
            return null; // Signal to use AdaptiveIconDrawable instead
        }
        
        // Create output bitmap with transparency
        Bitmap outputBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        
        // Create the shape path
        Path shapePath = createShapePath(shape, size);
        
        // Draw background with shape
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(backgroundColor);
        canvas.drawPath(shapePath, bgPaint);
        
        if (applyInset) {
            // For special icons (settings, search, notifications)
            // Use smaller inset (~18%) to match the visual size of app monochrome icons
            // App icons use 1.5x scale which results in similar visual fill
            float insetFraction = 0.18f;
            int inset = (int) (size * insetFraction);
            int foregroundSize = size - (inset * 2);
            
            // Draw foreground centered with inset
            foreground.setBounds(inset, inset, inset + foregroundSize, inset + foregroundSize);
        } else {
            // For monochrome icons: they are designed for 108dp canvas but displayed in ~72dp visible area
            // AdaptiveIconDrawable scales foreground to 1.5x and centers it, then masks
            // We need to draw larger than the canvas and let the shape clip it
            // Scale factor: foreground is drawn at ~1.5x to fill the shape better
            float scale = 1.5f;
            int scaledSize = (int) (size * scale);
            int offset = (size - scaledSize) / 2; // This will be negative, centering the larger drawable
            foreground.setBounds(offset, offset, offset + scaledSize, offset + scaledSize);
        }
        foreground.draw(canvas);
        
        return new BitmapDrawable(context.getResources(), outputBitmap);
    }

    /**
     * Convert a drawable to a bitmap
     */
    private static Bitmap drawableToBitmap(Drawable drawable, int size) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
            if (bmp != null && bmp.getWidth() == size && bmp.getHeight() == size) {
                return bmp.copy(Bitmap.Config.ARGB_8888, true);
            }
        }
        
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return bitmap;
    }
}
