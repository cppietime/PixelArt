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
public class IOHelper {

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

    /**
     * Draw pixels to a new bitmap
     *
     * @param pixels Source of pixels
     * @param img    Filled bitmap
     */
    public static void pixelsToBitmap(int[] pixels, Bitmap img) {
        int width = img.getWidth();
        int height = img.getHeight();
        if (pixels.length < width * height) {
            throw new IllegalArgumentException("Pixel source is too small to fill bitmap");
        }
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setPixel(x, y, pixels[index++]);
            }
        }
    }

}
