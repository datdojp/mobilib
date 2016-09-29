package com.datdo.mobilib.imageinput;

import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.datdo.mobilib.imageinput.MblImagePickingScanEngine.CmScanCallback;
import com.datdo.mobilib.imageinput.MblTakeImageActivity.MblTakeImageCallback;
import com.datdo.mobilib.util.MblUtils;

/**
 * Activity to pick images. Also support cropping.
 */
public class MblPickImageActivity extends MblDataInputActivity {

    private static final String TAG = MblUtils.getTag(MblPickImageActivity.class);

    private static final String EXTRA_PHOTO_NUMBER_LIMIT        = "photo_number_limit";
    private static final String EXTRA_CROP_SIZE_WIDTH_IN_PX     = "crop_size_width_in_px";
    private static final String EXTRA_CROP_SIZE_HEIGHT_IN_PX    = "crop_size_height_in_px";
    private static final String FILE_PROVIDER_AUTHORITY         = "file_provider_authority";

    private static final int IMAGE_LOADER_ID = 193;

    private MblPickImageGridViewAdapter mAdapter;
    private int     mCropSizeWidthInPx;
    private int     mCropSizeHeightInPx;
    private boolean mShouldRescanMediaFilesOnResume = true;
    private String  mFileProviderAuthority;

    private Button mSelectBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mbl_image_picker_layout);

        // get data from extra
        int photoNumberLimit = 1;
        if (getIntent().getExtras() != null) {
            photoNumberLimit    = getIntent().getExtras().getInt(EXTRA_PHOTO_NUMBER_LIMIT);
            mCropSizeWidthInPx  = getIntent().getExtras().getInt(EXTRA_CROP_SIZE_WIDTH_IN_PX);
            mCropSizeHeightInPx = getIntent().getExtras().getInt(EXTRA_CROP_SIZE_HEIGHT_IN_PX);
            mFileProviderAuthority = getIntent().getExtras().getString(FILE_PROVIDER_AUTHORITY);
        }

        // back button
        Button backButton = (Button) findViewById(R.id.nav_bar_left_button);
        backButton.setVisibility(View.VISIBLE);
        backButton.setText(R.string.mbl_back);
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MblPickImageActivity.this.cancelInput();
            }
        });

        // camera button
        Button cameraButton = (Button) findViewById(R.id.nav_bar_right_button);
        cameraButton.setVisibility(View.VISIBLE);
        cameraButton.setBackgroundResource(R.drawable.mbl_green_camera_button_state);
        cameraButton.setText(null);
        cameraButton.getLayoutParams().width = cameraButton.getLayoutParams().height;
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        // title
        TextView textView= (TextView) findViewById(R.id.nav_bar_text);
        textView.setVisibility(View.VISIBLE);
        textView.setText(R.string.mbl_select_photo);

        // init grid view
        GridView imageGrid = (GridView) findViewById(R.id.image_gridview);
        mAdapter = new MblPickImageGridViewAdapter(this, photoNumberLimit, imageGrid);
        imageGrid.setAdapter(mAdapter);

        // button to select
        mSelectBtn = (Button) findViewById(R.id.image_picker_done_button);
        mSelectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                List<String> selectedImagePaths = mAdapter.getSelectedImageUri();
                if (MblUtils.isEmpty(selectedImagePaths)) {
                    return;
                }

                mSelectBtn.setEnabled(false);
                if (needCrop()) {
                    cropImage(selectedImagePaths.get(0));
                } else {
                    finishPickingImage(selectedImagePaths);
                }
            }
        });
    }

    private void cropImage(String imagePath) {
        MblTakeImageActivity.start(
                imagePath,
                mCropSizeWidthInPx,
                mCropSizeHeightInPx,
                mFileProviderAuthority,
                new MblTakeImageCallback() {
                    @Override
                    public void onFinish(String path) {
                        MblPickImageActivity.this.finishInput(path);
                    }

                    @Override
                    public void onCancel() {
                        mSelectBtn.setEnabled(true);
                    }
                });
    }

    private void finishPickingImage(List<String> selectedImagePaths) {
        if(!MblUtils.isEmpty(selectedImagePaths)) {
            String[] outputData = new String[selectedImagePaths.size()];
            selectedImagePaths.toArray(outputData);
            finishInput((Object[])outputData);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.clearCache();
    }

    @Override
    protected void onDestroy() {
        try {
            getSupportLoaderManager().destroyLoader(IMAGE_LOADER_ID);
            if (mAdapter != null) {
                mAdapter.changeCursor(null);
                mAdapter = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to release loader", e);
        }

        super.onDestroy();
    }

    private void takePhoto() {
        MblTakeImageActivity.start(
                null,
                mCropSizeWidthInPx,
                mCropSizeHeightInPx,
                mFileProviderAuthority,
                new MblTakeImageCallback() {

                    @Override
                    public void onFinish(String path) {
                        // should not rescan because this activity is going be closed.
                        mShouldRescanMediaFilesOnResume = false;

                        MblPickImageActivity.this.finishInput(path);
                    }

                    @Override
                    public void onCancel() {
                        // do nothing
                    }
                });
    }

    /**
     * <pre>
     * Start activity to pick images.
     * Also support cropping by passing maxNumberOfImages=1, and cropSizeWidthInPx > 0, cropSizeWidthInPx > 0.
     * </pre>
     * @param maxNumberOfImages max number of images to pick
     * @param cropSizeWidthInPx crop image to specific width (in pixel). Pass -1 if you don't want to crop
     * @param cropSizeHeightInPx crop image to specific height (in pixel). Pass -1 if you don't want to crop
     * @param callback callback to receive result
     */
    public static void start(
            int maxNumberOfImages,
            int cropSizeWidthInPx,
            int cropSizeHeightInPx,
            String fileProviderAuthority,
            final MblPickImageCallback callback) {

        if (cropSizeWidthInPx > 0 && cropSizeHeightInPx > 0 && maxNumberOfImages != 1) {
            throw new RuntimeException("maxNumberOfImages must be 1 for cropping");
        }

        Intent intent = createIntent(MblPickImageActivity.class, new CmDataInputActivityCallback() {

            @Override
            public void onFinish(Object... outputData) {
                if (callback != null) {
                    String[] paths = new String[outputData.length];
                    for (int i = 0; i < outputData.length; i++) {
                        paths[i] = (String) outputData[i];
                    }
                    callback.onFinish(paths);
                }
            }

            @Override
            public void onCancel() {
                if (callback != null) {
                    callback.onCancel();
                }
            }
        }, null);
        intent.putExtra(EXTRA_PHOTO_NUMBER_LIMIT,       maxNumberOfImages);
        intent.putExtra(EXTRA_CROP_SIZE_WIDTH_IN_PX,    cropSizeWidthInPx);
        intent.putExtra(EXTRA_CROP_SIZE_HEIGHT_IN_PX,   cropSizeHeightInPx);
        intent.putExtra(FILE_PROVIDER_AUTHORITY, fileProviderAuthority);
        MblUtils.getCurrentContext().startActivity(intent);
    }

    public static interface MblPickImageCallback {
        public void onFinish(String[] paths);
        public void onCancel();
    }

    private boolean needCrop() {
        return mCropSizeWidthInPx > 0 && mCropSizeHeightInPx > 0;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mShouldRescanMediaFilesOnResume) {

            MblUtils.showProgressDialog(R.string.mbl_scanning, false);
            final String[] imageFolders = MblImageFolderScanner.getAllImageFolders();
            MblImagePickingScanEngine.scan(imageFolders, new CmScanCallback() {
                @Override
                public void onFinish(int nUpdatedFiles) {
                    MblUtils.hideProgressDialog();
                    initLoader(imageFolders);
                }

                @Override
                public void onFailure() {
                    MblUtils.hideProgressDialog();
                }
            });
        }
    }

    private void initLoader(final String[] imageFolder) {
        getSupportLoaderManager().initLoader(IMAGE_LOADER_ID, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

                if (loaderID == IMAGE_LOADER_ID) {

                    return new CursorLoader(
                            MblPickImageActivity.this,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[] {
                                    MediaStore.Images.Media.DATA,
                                    MediaStore.Images.Media._ID,
                                    MediaStore.Images.Media.DATE_MODIFIED },
                                    MblImagePickingScanEngine.buildMediaQuerySelection(imageFolder),
                                    MblImagePickingScanEngine.buildMediaQuerySelectionArgs(imageFolder),
                                    MediaStore.Images.Media.DATE_MODIFIED + " DESC");
                } else {
                    return null;
                }
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor returnCursor) {
                mAdapter.changeCursor(returnCursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mAdapter.changeCursor(null);
            }
        });
    }
}
