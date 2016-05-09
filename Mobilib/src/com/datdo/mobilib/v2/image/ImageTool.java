package com.datdo.mobilib.v2.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.ImageView;

import com.datdo.mobilib.api.MblApi;
import com.datdo.mobilib.api.MblRequest;
import com.datdo.mobilib.api.MblResponse;
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

    public static class LoadRequest {
        private Context context;
        private String url;
        private File file;
        private byte[] bytes;
        private int toWidth;
        private int toHeight;
        private FittingType fittingType = FittingType.GTE;
        private int errorResId;
        private int placeHolderResId;

        public LoadRequest load(String url) {
            this.url = url;
            return this;
        }

        public LoadRequest load(File file) {
            this.file = file;
            return this;
        }

        public LoadRequest load(byte[] bytes) {
            this.bytes = bytes;
            return this;
        }

        public LoadRequest toWidth(int toWidth) {
            this.toWidth = toWidth;
            return this;
        }

        public LoadRequest toHeight(int toHeight) {
            this.toHeight = toHeight;
            return this;
        }

        public LoadRequest fittingType(FittingType fittingType) {
            this.fittingType = fittingType;
            return this;
        }

        public LoadRequest error(int errorResId) {
            this.errorResId = errorResId;
            return this;
        }

        public LoadRequest placeHolder(int placeHolderResId) {
            this.placeHolderResId = placeHolderResId;
            return this;
        }

        public Bitmap now() {
            validate();
            Bitmap bm = null;
            if (url != null) {
                if (MblUtils.isMainThread()) {
                    throw new IllegalStateException("Loading bitmap from url requires callback. Consider using later(...) method");
                }
            }
            try {
            if (file != null) {
                bm = ImageTool.loadBitmap(toWidth, toHeight, file, fittingType);
            } else if (bytes != null) {
                bm = ImageTool.loadBitmap(toWidth, toHeight, file, fittingType);
            }
            } catch (OutOfMemoryError e) {
                Log.e(TAG, null, e);
                // try to reload in LTE mode
                if (fittingType != FittingType.LTE) {
                    Log.d(TAG, "Try to load in lower fitting type");
                    fittingType = FittingType.LTE;
                    return now();
                }
            }
            return bm;
        }

        public void now(final ImageView imageView) {
            validate();
            final Bitmap bm = now();
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (bm != null) {
                        imageView.setImageBitmap(bm);
                    } else {
                        if (errorResId > 0) {
                            imageView.setImageResource(errorResId);
                        } else {
                            imageView.setImageBitmap(null);
                        }
                    }
                }
            });
        }

        public void later(final IntoBitmapCallback callback) {
            validate();
            if (url != null) {
                final String path = MblApi.getCacheFilePath(url, null);
                if (path != null) {
                    url = null;
                    file = new File(path);
                    later(callback);
                } else {
                    MblApi.run(new MblRequest()
                            .setMethod(MblApi.Method.GET)
                            .setUrl(url)
                            .setCacheDuration(Long.MAX_VALUE)
                            .setRedirectEnabled(true)
                            .setCallback(new MblApi.MblApiCallback() {
                                @Override
                                public void onSuccess(MblResponse response) {
                                    String path = MblApi.getCacheFilePath(url, null);
                                    if (path != null) {
                                        url = null;
                                        file = new File(path);
                                        later(callback);
                                    } else {
                                        onFailure(response);
                                    }
                                }

                                @Override
                                public void onFailure(MblResponse response) {
                                    if (callback != null) {
                                        callback.onError(new RuntimeException("Failed to download image from url: " + url));
                                    }
                                }
                            }));
                }
            } else {
                MblUtils.executeOnAsyncThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Bitmap bm = now();
                            if (callback != null) {
                                MblUtils.executeOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (bm != null) {
                                            callback.onSuccess(bm);
                                        } else {
                                            callback.onError(new RuntimeException("Bitmap is null"));
                                        }
                                    }
                                });
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Failed to load bitmap asynchronously", t);
                            if (callback != null) {
                                callback.onError(t);
                            }
                        }
                    }
                });
            }
        }

        public void later(final ImageView imageView, final IntoImageCallback callback) {
            validate();
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (placeHolderResId > 0) {
                        imageView.setImageResource(placeHolderResId);
                    } else {
                        imageView.setImageBitmap(null);
                    }
                }
            });
            later(new IntoBitmapCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                    if (callback != null) {
                        callback.onSuccess(imageView, bitmap);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (errorResId > 0) {
                        imageView.setImageResource(errorResId);
                    } else {
                        imageView.setImageBitmap(null);
                    }
                    if (callback != null) {
                        callback.onError(t, imageView);
                    }
                }
            });
        }

        private void validate() {
            if (!oneObjectNotNull(url, file, bytes)) {
                throw new IllegalStateException("You must call load(...) only once");
            }
        }

        private boolean oneObjectNotNull(Object... objects) {
            int count = 0;
            for (Object o : objects) {
                if (o != null) {
                    count++;
                }
            }
            return count == 1;
        }
    }

    public interface IntoImageCallback {
        void onSuccess(ImageView imageView, Bitmap bitmap);
        void onError(Throwable t, ImageView imageView);
    }

    public interface IntoBitmapCallback {
        void onSuccess(Bitmap bitmap);
        void onError(Throwable t);
    }

    public static LoadRequest with(Context context) {
        LoadRequest request = new LoadRequest();
        request.context = context;
        return request;
    }

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

    static Bitmap loadBitmap(int targetWidth, int targetHeight, File file, FittingType fittingType) {

        try {
            int angle = getImageRotateAngle(file);
            if (angle == 90 || angle == 270) {
                int temp = targetWidth;
                targetWidth = targetHeight;
                targetHeight = temp;
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

            if (angle != 0) {
                bitmap = correctBitmapOrientation(file, bitmap);
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap: targetW=" + targetWidth + ", targetW=" + targetWidth + ", path=" + file.getAbsolutePath());
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
