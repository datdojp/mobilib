package com.datdo.mobilib.imageinput;


import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.datdo.mobilib.util.MblSimpleImageLoader;
import com.datdo.mobilib.util.MblSimpleImageLoader.*;
import com.datdo.mobilib.util.MblUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MblPickImageGridViewAdapter extends CursorAdapter {

    private MblSimpleImageLoader<ImageMetaData> mImageLoader = new MblSimpleImageLoader<ImageMetaData>() {
        @Override
        protected ImageMetaData getItemBoundWithView(View view) {
            return ((Holder)view.getTag()).mImageData;
        }

        @Override
        protected ImageView getImageViewBoundWithView(View view) {
            return ((Holder)view.getTag()).mThumbnailImageView;
        }

        @Override
        protected String getItemId(ImageMetaData item) {
            return String.valueOf(item.mImageId);
        }

        @Override
        protected void retrieveImage(final ImageMetaData item, final MblRetrieveImageCallback cb) {
            MblUtils.executeOnAsyncThread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bm = null;
                    File file = new File(item.mImagePath);
                    if (file.exists() && file.length() > 0) {
                        bm = MediaStore.Images.Thumbnails.getThumbnail(
                                MblUtils.getCurrentContext().getContentResolver(),
                                item.mImageId,
                                MediaStore.Images.Thumbnails.MICRO_KIND,
                                null);
                    }
                    if (bm != null) {
                        bm = MblUtils.correctBitmapOrientation(item.mImagePath, bm);
                    }
                    cb.onRetrievedBitmap(bm);

                    MblUtils.getMainThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            cb.onRetrievedFile(item.mImagePath);
                        }
                    });
                }
            });
        }

        @Override
        protected void onError(ImageView imageView, ImageMetaData item) {
            imageView.setImageBitmap(null);
        }
    }.setOptions(new MblOptions()
            .setSerializeImageLoading(false)
            .setDelayedDurationInParallelMode(0)
            .setEnableProgressView(false)
            .setEnableFadingAnimation(false));

    private final Set<Integer>  mThumbnailsSelection = new HashSet<Integer>();
    private int                 mPhotoNumberLimit;
    private GridView            mGridView;

    public MblPickImageGridViewAdapter(Context context, int photoNumberLimit, GridView gridView) {
        super(context, null, false);
        mPhotoNumberLimit   = photoNumberLimit;
        mGridView           = gridView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Holder holder = (Holder) view.getTag();
        int holderId = cursor.getPosition();
        holder.mId = holderId;

        int imageId = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
        String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        holder.mImageData = new ImageMetaData(imageId, imagePath);
        mImageLoader.loadImage(view);

        boolean checkOn = mThumbnailsSelection.contains(holderId);
        setItemCheckedStatus(holder, checkOn);
    }

    @SuppressLint("InflateParams")
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View view = MblUtils.getLayoutInflater().inflate(R.layout.mbl_image_picker_item, null);

        final Holder holder = new Holder();
        holder.mThumbnailImageView = (MblAutoResizeSquareImageView) view.findViewById(R.id.image_picker_imageview);
        holder.mCheckView = view.findViewById(R.id.image_picker_checkview);
        holder.mHiddenLayer = view.findViewById(R.id.image_picker_hidden_layer);
        view.setTag(holder);

        holder.mCheckView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        holder.mThumbnailImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleItemSelect(holder);
            }
        });

        return view;
    }

    private void setItemCheckedStatus(Holder holder, boolean checkOn) {
        holder.mCheckView.setSelected(checkOn);
        holder.mHiddenLayer.setVisibility(checkOn ? View.VISIBLE : View.GONE);
    }

    private void toggleItemSelect(Holder holder) {
        int holderId = holder.mId;
        boolean isChecked = mThumbnailsSelection.contains(holderId);

        if(!isChecked) {
            if(mThumbnailsSelection.size() < mPhotoNumberLimit) {
                mThumbnailsSelection.add(holderId);
                setItemCheckedStatus(holder, true);
            } else {
                if (mPhotoNumberLimit == 1) {
                    for (int i = 0; i < mGridView.getChildCount(); i++) {
                        View view = mGridView.getChildAt(i);
                        Holder temp = (Holder) view.getTag();
                        if (mThumbnailsSelection.contains(temp.mId)){
                            setItemCheckedStatus(temp, false);
                        }
                    }
                    mThumbnailsSelection.clear();
                    mThumbnailsSelection.add(holderId);
                    setItemCheckedStatus(holder, true);
                } else {
                    MblUtils.showToast(
                            MblUtils.getCurrentContext().getString(R.string.mbl_select_at_most_x_photos_at_once, mPhotoNumberLimit),
                            Toast.LENGTH_SHORT);
                }
            }
        } else{
            mThumbnailsSelection.remove(holderId);
            setItemCheckedStatus(holder, false);
        }
    }

    public List<String> getSelectedImageUri() {
        Cursor cursor = getCursor();
        List<String> selectedImageUris = new ArrayList<String>();
        int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int dateModifiedColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);

        final Map<String, Long> mapOfPathAndDate = new HashMap<String, Long>();
        for(Integer position : mThumbnailsSelection) {
            cursor.moveToPosition(position);
            String path = cursor.getString(dataColumnIndex);
            long dateModified = cursor.getLong(dateModifiedColumnIndex);

            selectedImageUris.add(path);
            mapOfPathAndDate.put(path, dateModified);
        }

        // sort paths by date modified DESC
        Collections.sort(selectedImageUris, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                long leftDateModified   = mapOfPathAndDate.get(lhs);
                long rightDateModified  = mapOfPathAndDate.get(rhs);
                if (leftDateModified > rightDateModified) {
                    return -1;
                }
                if (leftDateModified < rightDateModified) {
                    return 1;
                }
                return 0;
            }
        });

        return selectedImageUris;
    }

    private class Holder {
        MblAutoResizeSquareImageView mThumbnailImageView;
        View    mHiddenLayer;
        View    mCheckView;
        int     mId;
        ImageMetaData mImageData;
    }

    private class ImageMetaData {
        int     mImageId;
        String  mImagePath;

        public ImageMetaData(int imageId, String imagePath) {
            mImageId = imageId;
            mImagePath = imagePath;
        }
    }

    public void clearCache() {
        mImageLoader.invalidate(ImageMetaData.class);
    }
}
