package com.funguscow.pixelart;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;

public class Utils {

    public static final float LOG2 = (float) Math.log(0.5);

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

    public static int HSV_to_ARGB(float h, float s, float v) {
        return HSVA_to_ARGB(h, s, v, 1f);
    }

    public static float bias(float t, float bias) {
        return (float) Math.pow(t, Math.log(bias) / LOG2);
    }

    public static float gain(float t, float gain) {
        if (t <= 0.5f) {
            return 0.5f * bias(t * 2, 1f - gain);
        }
        return 1f - 0.5f * bias(2f - t * 2, 1f - gain);
    }

    public static float lerp(float a, float b, float z) {
        return a + (b - a) * z;
    }

    public static float clamp(float x, float min, float max) {
        return Math.min(Math.max(x, min), max);
    }

    private static ContentValues freshValuesForMIME(String mime) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, mime);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        }
        return values;
    }

    public static void saveBitmap(Bitmap img, Context context, String folder) {
        boolean success = false;
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = freshValuesForMIME("image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/");
            values.put(MediaStore.Images.Media.IS_PENDING, true);

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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
            File directory = new File(Environment.getExternalStorageDirectory(), folder);
            Log.d("Pixelart", "Pre exists? " + directory.exists() + "");
            directory.mkdirs();
            Log.d("Pixelart", "Post exists? " + directory.exists() + "");
            String fileName = System.currentTimeMillis() + ".png";
            File imgFile = new File(directory, fileName);
            try {
                FileOutputStream fos = new FileOutputStream(imgFile);
                img.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                ContentValues values = freshValuesForMIME("image/png");
                values.put(MediaStore.Images.Media.DATA, imgFile.getAbsolutePath());
                context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                success = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!success) {
            Toast.makeText(context, ":c Failed to save image", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Saved image!", Toast.LENGTH_LONG).show();
        }
    }

}
