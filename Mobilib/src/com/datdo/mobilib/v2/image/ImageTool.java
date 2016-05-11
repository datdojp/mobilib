package com.datdo.mobilib.v2.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import com.datdo.mobilib.util.MblUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by dat on 2016/04/28.
 */
public class ImageTool {

    private static final String TAG = ImageTool.class.getSimpleName();

    static Bitmap loadBitmap(final int targetW, final int targetH, final byte[] bytes, FittingType fittingType) {
        return new LoadBitmapMatchSpecifiedSizeTemplate<byte[]>() {

            @Override
            public int[] getBitmapSizes(byte[] bmData) {
                return MblUtils.getBitmapSizes(bmData);
            }

            @Override
            public Bitmap decodeBitmap(byte[] bmData, BitmapFactory.Options options) {
                return BitmapFactory.decodeByteArray(bmData, 0, bmData.length, options);
            }

        }.load(targetW, targetH, bytes, fittingType);
    }

    static Bitmap loadBitmap(int targetWidth,
                             int targetHeight,
                             File file,
                             FittingType fittingType,
                             boolean autoCorrectOrientation) {
        try {
            int angle = 0;
            if (autoCorrectOrientation) {
                angle = getImageRotateAngle(file);
                if (angle == 90 || angle == 270) {
                    int temp = targetWidth;
                    targetWidth = targetHeight;
                    targetHeight = temp;
                }
            }

            Bitmap bitmap = new LoadBitmapMatchSpecifiedSizeTemplate<File>() {

                @Override
                public int[] getBitmapSizes(File file) {
                    try {
                        return ImageTool.getBitmapSizes(file);
                    } catch (IOException e) {
                        return new int[] {0, 0};
                    }
                }

                @Override
                public Bitmap decodeBitmap(File file, BitmapFactory.Options options) {
                    return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                }

            }.load(targetWidth, targetHeight, file, fittingType);

            if (autoCorrectOrientation) {
                if (angle != 0) {
                    bitmap = correctBitmapOrientation(file, bitmap);
                }
            }

            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load bitmap: targetW=" + targetWidth + ", targetW=" + targetWidth + ", path=" + file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * <pre>
     * Get rotation angle of an image.
     * This information is stored in image file. Therefore, this method needs a file, not a {@link Bitmap} object or byte array.
     * </pre>
     * @return one of 0, 90, 180, 270
     */
    public static int getImageRotateAngle(File file) throws IOException {
        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int angle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            angle = 90;
        }
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            angle = 180;
        }
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            angle = 270;
        }
        return angle;
    }

    /**
     * <pre>
     * Rotate bitmap to its correct orientation if needed.
     * WARNING: {@link Bitmap} is immutable. Therefore, when new {@link Bitmap} is created, old {@link Bitmap} object is recycled to prevent {@link OutOfMemoryError}.
     * </pre>
     * @param file file from which bitmap was loaded
     * @param bm {@link Bitmap} object
     * @return rotated {@link Bitmap} object if angel != 0, otherwise return original {@link Bitmap} object
     */
    public static Bitmap correctBitmapOrientation(File file, Bitmap bm) {
        if (file != null && bm != null) {
            int angle = 0;
            try {
                angle = getImageRotateAngle(file);
                if (angle != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(angle);
                    Bitmap rotatedBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);

                    bm.recycle();
                    bm = rotatedBm;
                }
            } catch (IOException e) {
                Log.e(TAG, "Can not rotate bitmap file: " + file.getAbsolutePath() + ", angle:" + angle, e);
            }
        }
        return bm;
    }

    /**
     * <pre>
     * Get width and height of bitmap from byte array.
     * </pre>
     * @param bytes bitmap binary data
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(byte[] bytes) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bmOptions);
        return new int[]{ bmOptions.outWidth, bmOptions.outHeight };
    }

    /**
     * <pre>
     * Get width and height of bitmap from resource.
     * </pre>
     * @param resId resource id of bitmap
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(Context context, int resId) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, bmOptions);
        return new int[]{ bmOptions.outWidth, bmOptions.outHeight };
    }

    /**
     * <pre>
     * Get width and height of bitmap from file.
     * </pre>
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(File file) throws IOException {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        FileInputStream is = new FileInputStream(file);
        BitmapFactory.decodeStream(is, null, bmOptions);
        is.close();
        return new int[] { bmOptions.outWidth, bmOptions.outHeight };
    }

    /**
     * <pre>
     * Get width and height of bitmap from InputStream.
     * </pre>
     * @param is the stream
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(InputStream is) throws IOException {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, bmOptions);
        is.close();
        return new int[] { bmOptions.outWidth, bmOptions.outHeight };
    }
}
