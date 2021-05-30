package com.funguscow.pixelart;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Common functions, mostly math/IO
 */
public class Utils {

    public static final float LOG_0_5 = (float) Math.log(0.5);

    /**
     * Convert hue, saturation, value, and alpha to ARGB
     *
     * @param h Hue in [0, 1]
     * @param s Saturation in [0, 1]
     * @param v Value in [0, 1]
     * @param a Alpha in [0, 1]
     * @return Color as 0xAARRGGBB
     */
    public static int HSVA_to_ARGB(float h, float s, float v, float a) {
        h = (((h % 1) + 1) % 1) * 6; // Adjust possible negatives
        int sextant = (int) h;
        h %= 1;
        if ((sextant & 1) == 1) {
            h = 1f - h;
        }
        // hi = v
        float low = v * (1f - s);
        float medium = low + v * s * h;
        int r, g, b;
        switch (sextant) {
            case 0: // Red-Yellow
                r = (int) (v * 255);
                g = (int) (medium * 255);
                b = (int) (low * 255);
                break;
            case 1: // Yellow-Green
                r = (int) (medium * 255);
                g = (int) (v * 255);
                b = (int) (low * 255);
                break;
            case 2: // Green-Cyan
                r = (int) (low * 255);
                g = (int) (v * 255);
                b = (int) (medium * 255);
                break;
            case 3: // Cyan-Blue
                r = (int) (low * 255);
                g = (int) (medium * 255);
                b = (int) (v * 255);
                break;
            case 4: // Blue-Magenta
                r = (int) (medium * 255);
                g = (int) (low * 255);
                b = (int) (v * 255);
                break;
            default: // Magenta-Red
                r = (int) (v * 255);
                g = (int) (low * 255);
                b = (int) (medium * 255);
                break;
        }
        int alpha = (int) (a * 255);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Convert hue, saturation, and value to ARGB
     *
     * @param h Hue in [0, 1]
     * @param s Saturation in [0, 1]
     * @param v Value in [0, 1]
     * @return Color as 0xFFRRGGBB (alpha = 255)
     */
    public static int HSV_to_ARGB(float h, float s, float v) {
        return HSVA_to_ARGB(h, s, v, 1f);
    }

    /**
     * Perlin bias function
     *
     * @param t    Independent variable
     * @param bias Bias parameter
     * @return {@code t} ^ ( log({@code bias}) / log(0.5) )
     */
    public static float bias(float t, float bias) {
        return (float) Math.pow(t, Math.log(bias) / LOG_0_5);
    }

    /**
     * Perlin gain function
     *
     * @param t    Independent variable
     * @param gain Gain parameter
     * @return Scaled/mirrored bias function
     */
    public static float gain(float t, float gain) {
        if (t <= 0.5f) {
            return 0.5f * bias(t * 2, 1f - gain);
        }
        return 1f - 0.5f * bias(2f - t * 2, 1f - gain);
    }

    /**
     * Linear interpolation
     *
     * @param a Left-value
     * @param b Right-value
     * @param z Interpolation amount
     * @return {@code a} + ({@code b} - {@code a}) * {@code z}
     */
    public static float lerp(float a, float b, float z) {
        return a + (b - a) * z;
    }

    /**
     * Clamp {@code x} between {@code min} and {@code max}
     *
     * @param x   Value to clamp
     * @param min Minimum value
     * @param max Maximum value
     * @return {@code min} if {@code x} \< {@code min}. {@code max} if {@code x} \> {@code max}.
     * otherwise {@code x}
     */
    public static float clamp(float x, float min, float max) {
        return Math.min(Math.max(x, min), max);
    }

    /**
     * Clamp {@code x} between {@code min} and {@code max}
     *
     * @param x   Value to clamp
     * @param min Minimum value
     * @param max Maximum value
     * @return {@code min} if {@code x} \< {@code min}. {@code max} if {@code x} \> {@code max}.
     * otherwise {@code x}
     */
    public static int clamp(int x, int min, int max) {
        return Math.min(Math.max(x, min), max);
    }

    /**
     * Sample a clamped pixel
     *
     * @param image  Image from which to sample
     * @param width  Width in pixels of {@code image}
     * @param height Height in pixels of {@code image}
     * @param x      X index in pixels to sample
     * @param y      Y index in pixels to sample
     * @return Pixel in {@code image} at {@code y} * {@code width} + {@code x}, or the nearest
     * border pixel if out of bounds.
     */
    public static int clampedPixelAt(int[] image, int width, int height, int x, int y) {
        x = clamp(x, 0, width - 1);
        y = clamp(y, 0, height - 1);
        return image[y * width + x];
    }

    /**
     * Util function to generate content values for saving
     *
     * @param mime MIME type
     * @param name Name of object
     * @return ContentValues for saving
     */
    private static ContentValues freshValuesForMIME(String mime, String name) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, mime);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        }
        return values;
    }

    /**
     * Save a Bitmap to the device's gallery
     *
     * @param img     Bitmap to save
     * @param context Context of application
     * @param folder  Name of parent folder
     * @param name    Name of image to save
     * @param toast   Whether or not to display a toast
     * @return {@code true} iff the image was saved
     */
    public static boolean saveBitmap(Bitmap img,
                                     Activity context,
                                     String folder,
                                     String name,
                                     boolean toast) {
        boolean success = false;
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = freshValuesForMIME("image/png", name);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folder);
            values.put(MediaStore.Images.Media.IS_PENDING, true);

            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try {
                    OutputStream os = context.getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        img.compress(Bitmap.CompressFormat.PNG, 100, os);
                        success = true;
                        os.close();
                    }
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    context.getContentResolver().update(uri, values, null, null);
                } catch (IOException e) {
                    e.printStackTrace();
                    success = false;
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
            File directory = new File(Environment.getExternalStorageDirectory(), folder);
            boolean dirExist = true;
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    dirExist = false;
                }
            }
            if (dirExist) {
                String fileName = name + ".png";
                File imgFile = new File(directory, fileName);
                try {
                    FileOutputStream fos = new FileOutputStream(imgFile);
                    img.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    ContentValues values = freshValuesForMIME("image/png", name);
                    values.put(MediaStore.Images.Media.DATA, imgFile.getAbsolutePath());
                    context.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values);
                    success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (toast) {
            if (!success) {
                context.runOnUiThread(() ->
                        Toast.makeText(context,
                                ":c Failed to save image",
                                Toast.LENGTH_LONG).show());
            } else {
                context.runOnUiThread(() ->
                        Toast.makeText(context, "Saved image!", Toast.LENGTH_LONG).show());
            }
        }
        return success;
    }

}
