package com.datdo.mobilib.imageinput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;

import com.datdo.mobilib.util.MblUtils;
import com.datdo.mobilib.v2.image.FittingType;
import com.datdo.mobilib.v2.image.ImageTool;
import com.datdo.mobilib.widget.MblTouchImageView;

/**
 * Activity to take image by camera. Also support cropping.
 */
public class MblTakeImageActivity extends MblDataInputActivity {

    private static final String TAG = MblUtils.getTag(MblTakeImageActivity.class);

    private static final int REQUEST_CODE = 268;

    private static final String EXTRA_INPUT_IMAGE_PATH          = "input_image_path";
    private static final String EXTRA_CROP_SIZE_WIDTH_IN_PX     = "crop_size_width_in_px";
    private static final String EXTRA_CROP_SIZE_HEIGHT_IN_PX    = "crop_size_height_in_px";
    private static final String EXTRA_NATIVE_CAMERA_RETURN_DATA = "return-data";

    private MblTouchImageView mPreviewImageView;
    private Uri     mTakenPhotoUri;
    private String  mInputImagePath;
    private int     mCropSizeWidthInPx;
    private int     mCropSizeHeightInPx;
    private View    mCropFrame;
    private View    mCropFrameMid;
    private boolean mStartTakePhotoOnResume;
    private boolean mIsPortrait;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mbl_photo_preview_layout);

        // get data from extra
        if (getIntent().getExtras() != null) {
            mInputImagePath     = getIntent().getExtras().getString(EXTRA_INPUT_IMAGE_PATH);
            mCropSizeWidthInPx  = getIntent().getExtras().getInt(EXTRA_CROP_SIZE_WIDTH_IN_PX);
            mCropSizeHeightInPx = getIntent().getExtras().getInt(EXTRA_CROP_SIZE_HEIGHT_IN_PX);
        }

        // init UI
        mPreviewImageView   = (MblTouchImageView) findViewById(R.id.image);
        Button leftButton   = (Button) findViewById(R.id.left_button);
        mCropFrame          = findViewById(R.id.crop_frame);
        mCropFrameMid       = mCropFrame.findViewById(R.id.mid);

        if (needCrop()) {
            // set sizes for transparent area in middle of crop frame
            ViewGroup.LayoutParams lpOfMid = mCropFrameMid.getLayoutParams();
            lpOfMid.width = mCropSizeWidthInPx;
            lpOfMid.height = mCropSizeHeightInPx;
            mCropFrameMid.setLayoutParams(lpOfMid);

            // set sizes frame surrounding middle view of crop frame
            View frameOfMid = mCropFrame.findViewById(R.id.frame);
            ViewGroup.LayoutParams lpOfMidFrame = frameOfMid.getLayoutParams();
            lpOfMidFrame.width = mCropSizeWidthInPx + MblUtils.pxFromDp(2);
            lpOfMidFrame.height = mCropSizeHeightInPx + MblUtils.pxFromDp(2);
            frameOfMid.setLayoutParams(lpOfMidFrame);
        }

        if (mInputImagePath == null) { // take photo

            // left button
            leftButton.setText(R.string.mbl_retake_photo);
            leftButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    takePhoto();
                }
            });

            //  show native camera right away
            mStartTakePhotoOnResume = true;

        } else { // load photo from external storage

            // left button
            leftButton.setText(R.string.mbl_cancel);
            leftButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelInput();
                }
            });

            // show crop frame
            mCropFrame.setVisibility(View.VISIBLE);

            // load photo from storage
            loadPhotoFromExternal(mInputImagePath);
        }

        // right button
        final Button rightButton = (Button) findViewById(R.id.right_button);
        rightButton.setText(R.string.mbl_use_photo);
        rightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rightButton.setEnabled(false);
                if (needCrop()) {
                    cropPhoto();
                } else {
                    usePhoto();
                }
            }
        });

        // store orientation
        mIsPortrait = MblUtils.isPortraitDisplay();
    }

    @Override
    protected void finishInput(Object... outputData) {
        deallocatePreviewImageView();
        super.finishInput(outputData);
    }

    @Override
    protected void cancelInput() {
        deallocatePreviewImageView();
        super.cancelInput();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mStartTakePhotoOnResume) {
            takePhoto();
            mStartTakePhotoOnResume = false;
        }
    }

    private File getTempFile(String name) {
        // get external storage folder to store image
        File externalDir = new File(MblImageInput.sFolderToSaveTakenImages);
        if (!externalDir.exists()) {
            externalDir.mkdirs();
        }

        // create file to store image
        File f = new File(externalDir, name);
        if (f.exists()) {
            f.delete();
        }
        try {
            f.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "Can not create temp file at: " + f.getAbsolutePath(), e);
            return null;
        }
        return f;
    }

    private void takePhoto() {

        deallocatePreviewImageView();

        File tempFile = getTempFile(UUID.randomUUID().toString() + ".jpg");
        if (tempFile != null) {
            mTakenPhotoUri = Uri.fromFile(tempFile);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTakenPhotoUri);
            try {
                cameraIntent.putExtra(EXTRA_NATIVE_CAMERA_RETURN_DATA, true);
                startActivityForResult(cameraIntent, REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Camera is not available", e);
                // TODO: show alert
            }
        } else {
            // TODO: show alert
        }
    }

    private void usePhoto() {
        if (mTakenPhotoUri != null) {
            finishInput(mTakenPhotoUri.getPath());
        }
    }

    private void cropPhoto() {

        try {

            // get image being manipulated
            Bitmap photo = MblUtils.extractBitmap(mPreviewImageView);
            if (photo == null || photo.isRecycled()) {
                cancelInput();
                return;
            }

            // calculate crop area x,y,w,h
            float[] matrixValues    =  mPreviewImageView.getMatrixValues();
            float   transX          = matrixValues[Matrix.MTRANS_X];
            float   transY          = matrixValues[Matrix.MTRANS_Y];
            float   scaleX          = matrixValues[Matrix.MSCALE_X];
            float   scaleY          = matrixValues[Matrix.MSCALE_Y];
            int     cropAreaX       = Math.round((mCropFrameMid.getLeft() - transX) / scaleX);
            int     cropAreaY       = Math.round((mCropFrameMid.getTop() - transY) / scaleY);
            int     cropAreaWidth   = Math.round(mCropFrameMid.getWidth() / scaleX);
            int     cropAreaHeight  = Math.round(mCropFrameMid.getHeight() / scaleY);

            // make crop area be inside photo
            cropAreaWidth   = Math.min(cropAreaWidth, photo.getWidth());
            cropAreaHeight  = Math.min(cropAreaHeight, photo.getHeight());
            cropAreaX       = Math.min(Math.max(0, cropAreaX), photo.getWidth() - cropAreaWidth);
            cropAreaY       = Math.min(Math.max(0, cropAreaY), photo.getHeight() - cropAreaHeight);

            // calculate crop matrix
            Matrix matrix = new Matrix();
            matrix.postScale(1.0f * mCropSizeWidthInPx / cropAreaWidth, 1.0f * mCropSizeHeightInPx / cropAreaHeight);

            // crop bitmap
            Bitmap croppedPhoto = Bitmap.createBitmap(photo, cropAreaX, cropAreaY, cropAreaWidth, cropAreaHeight, matrix, true);

            // save bitmap to cache folder
            String cacheImagePath = MblUtils.getCacheAsbPath(UUID.randomUUID().toString() + ".jpg");
            OutputStream os = new FileOutputStream(cacheImagePath);
            croppedPhoto.compress(CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
            finishInput(cacheImagePath);

        } catch (Exception e) {
            Log.e(TAG, "Can not crop photo", e);
            cancelInput();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE) {
            resetDefaultMaxAllowedTrasitionBetweenActivity();
            if (resultCode == RESULT_OK) {
                loadPhotoFromExternal(mTakenPhotoUri.getPath());
                if (needCrop()) {
                    mCropFrame.setVisibility(View.VISIBLE);
                }
            } else if (resultCode == RESULT_CANCELED) {
                // delete temp file
                new File(mTakenPhotoUri.getPath()).delete();

                // cancel input
                cancelInput();
            }
        }
    }

    private boolean needCrop() {
        return mCropSizeWidthInPx > 0 && mCropSizeHeightInPx > 0;
    }

    private void loadPhotoFromExternal(final String imagePath) {

        if (mPreviewImageView.getWidth() == 0 || mPreviewImageView.getHeight() == 0 || mIsPortrait != MblUtils.isPortraitDisplay()) {
            mPreviewImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    MblUtils.removeOnGlobalLayoutListener(mPreviewImageView, this);
                    loadPhotoFromExternal(imagePath);
                }
            });
            return;
        }

        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                Bitmap temp = null;
                try {
                    temp = ImageTool.with(MblTakeImageActivity.this)
                            .load(new File(imagePath))
                            .toWidth(mPreviewImageView.getWidth())
                            .toHeight(mPreviewImageView.getHeight())
                            .fittingType(FittingType.GTE)
                            .now();
                } catch (OutOfMemoryError e) {

                    System.gc();

                    try {
                        Log.e(TAG, "Image too big -> scale to match ImageView size", e);
                        temp = ImageTool.with(MblTakeImageActivity.this)
                                .load(new File(imagePath))
                                .toWidth(mPreviewImageView.getWidth())
                                .toHeight(mPreviewImageView.getHeight())
                                .fittingType(FittingType.LTE)
                                .now();
                    } catch (OutOfMemoryError e2) {
                        Log.e(TAG, "Still too big --> cancel", e);
                    }
                }

                final Bitmap bm = temp;
                if (bm == null) {
                    cancelInput();
                    return;
                }

                MblUtils.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        // display bitmap
                        mPreviewImageView.setImageBitmap(bm);

                        // set min and max for zoom
                        float minZoom = MblImageInput.sCropMinZoom;
                        float maxZoom = MblImageInput.sCropMaxZoom;
                        if (needCrop()) {
                            float minBmWidth = minZoom * bm.getWidth();
                            float minBmHeight = minZoom * bm.getHeight();
                            boolean needJustify = false;
                            if (minBmWidth < mCropSizeWidthInPx) {
                                minBmWidth = mCropSizeWidthInPx;
                                needJustify = true;
                            }
                            if (minBmHeight < mCropSizeHeightInPx) {
                                minBmHeight = mCropSizeHeightInPx;
                                needJustify = true;
                            }
                            if (needJustify) {
                                float maxPerMin = maxZoom / minZoom;
                                minZoom = Math.max(
                                        minBmWidth / bm.getWidth(),
                                        minBmHeight / bm.getHeight());
                                maxZoom = maxPerMin * minZoom;
                            }
                        }
                        if (needCrop()) {
                            mPreviewImageView.setOptions(
                                    minZoom, maxZoom, -1,
                                    mCropFrame.findViewById(R.id.left).getWidth(),
                                    mCropFrame.findViewById(R.id.top).getHeight(),
                                    mCropFrame.findViewById(R.id.right).getWidth(),
                                    mCropFrame.findViewById(R.id.bottom).getHeight());
                        } else {
                            mPreviewImageView.setOptions(
                                    minZoom, maxZoom, -1,
                                    0, 0, 0, 0);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        deallocatePreviewImageView();
        super.onDestroy();
    }

    /**
     * <pre>
     * Start activity to take image by camera.
     * Support cropping by passing cropSizeWidthInPx > 0, cropSizeWidthInPx > 0.
     * Also support cropping a specific image (via inputImagePath) without capturing image by camera
     * </pre>
     * @param inputImagePath 
     * @param cropSizeWidthInPx crop image to specific width (in pixel). Pass -1 if you don't want to crop
     * @param cropSizeHeightInPx crop image to specific height (in pixel). Pass -1 if you don't want to crop
     * @param callback callback to receive result
     */
    public static void start(
            Context context,
            String inputImagePath,
            int cropSizeWidthInPx,
            int cropSizeHeightInPx,
            final MblTakeImageCallback callback) {

        Intent intent = createIntent(MblTakeImageActivity.class, new CmDataInputActivityCallback() {
            @Override
            public void onFinish(Object... outputData) {
                if (callback != null) {
                    callback.onFinish((String)outputData[0]);
                }
            }

            @Override
            public void onCancel() {
                if (callback != null) {
                    callback.onCancel();
                }
            }
        }, null);
        intent.putExtra(EXTRA_INPUT_IMAGE_PATH,         inputImagePath);
        intent.putExtra(EXTRA_CROP_SIZE_WIDTH_IN_PX,    cropSizeWidthInPx);
        intent.putExtra(EXTRA_CROP_SIZE_HEIGHT_IN_PX,   cropSizeHeightInPx);
        context.startActivity(intent);
    }

    public static interface MblTakeImageCallback {
        public void onFinish(String path);
        public void onCancel();
    }

    private void deallocatePreviewImageView() {
        MblUtils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                MblUtils.recycleImageView(mPreviewImageView);
            }
        });
    }
}
