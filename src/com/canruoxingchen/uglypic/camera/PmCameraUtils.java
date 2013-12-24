package com.canruoxingchen.uglypic.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import com.canruoxingchen.uglypic.util.FileUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

public class PmCameraUtils {
    public static final int MAX_BITMAP_MEMORY = 1024 * 1024 * 8;

    public static Bitmap loadBitmapFromAssets(Context context, String path) {
        if (context == null || path == null) {
            return null;
        }

        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = context.getAssets().open(path);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
        }

        return bitmap;
    }

    public static Bitmap loadBitmapFromRawResource(Context context, int rawResourceId) {
        if (context == null || rawResourceId < 0) {
            return null;
        }

        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(rawResourceId);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
        }

        return bitmap;
    }

    public static String loadRawString(Context context, int rawId) {
        String result = null;
        InputStream is = context.getResources().openRawResource(rawId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            result = baos.toString();

            is.close();
            baos.close();
        } catch (IOException e) {
        }

        return result;
    }

    public static String getImagePathFromUri(Context context, Uri uri) {
        if (uri.toString().startsWith("content://com.android.gallery3d.provider")) {
            uri = Uri.parse(uri.toString().replace("com.android.gallery3d", "com.google.android.gallery3d"));
        }

        String imageFilePath = null;
        String schema = uri.getScheme();
        if (schema != null) {
            if (schema.equals("file")) {
                imageFilePath = uri.getPath();
            } else {
                imageFilePath = saveImageByUrl(context, uri, "picasa_temp_file");
            }
        }

        return imageFilePath;
    }

    public static String getUriData(Context context, Uri uri) {
        String data = null;
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            data = cursor.getString(column_index);
            cursor.close();
        }
        return data;
    }

    public static String saveImageByUrl(Context context, Uri uri, String tempFileName) {
        String savePath = FileUtils.createSdCardFile(tempFileName);
        if (TextUtils.isEmpty(savePath)) {
            return null;
        }

        File f = new File(savePath);
        if(f.exists()) {
        	f.delete();
        }
        String path = getUriData(context, uri);
        try {
            InputStream is = null;
            f.createNewFile();
            // if (path != null && (path.startsWith("http://") ||
            // path.startsWith("https://"))) {
            // is = new URL(path).openStream();
            // } else {
            // is = context.getContentResolver().openInputStream(uri);
            // }
            is = context.getContentResolver().openInputStream(uri);

            OutputStream os = new FileOutputStream(f);
            byte buf[] = new byte[8192];
            int length = 0;
            int totalCount = 0;
            while ((length = is.read(buf)) > 0) {
                os.write(buf, 0, length);
                totalCount += length;
            }
            os.close();
            is.close();

            path = totalCount > 0 ? f.getAbsolutePath() : null;
        } catch (MalformedURLException e) {
            path = null;
        } catch (IOException e) {
            path = null;
        }

        return path;
    }

    public static int getExifOrientation(String filePath) {
        if (filePath == null) {
            return 0;
        }

        int orientation = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            switch (Integer.valueOf(exif.getAttribute(ExifInterface.TAG_ORIENTATION))) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientation = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = 270;
                break;
            default:
                break;
            }
        } catch (IOException e) {
        }

        return orientation;
    }

    public static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }


    public static long getFileSize(String filePath) {
        if (filePath == null) {
            return 0L;
        }

        File file = new File(filePath);
        return file.length();
    }
}
