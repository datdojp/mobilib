package com.datdo.mobilib.imageinput;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.Toast;

import com.datdo.mobilib.util.MblUtils;

class MblPickImageGridViewAdapter extends CursorAdapter {

    private static final int IMAGE_CACHE_SIZE = 4*1024*1024; // 4Mb

    private static LruCache<Integer, Bitmap> sImageLruCache = new LruCache<Integer, Bitmap>(IMAGE_CACHE_SIZE);

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
        loadImage(holder, imageId, imagePath);

        boolean checkOn = mThumbnailsSelection.contains(holderId);
        setItemCheckedStatus(holder, checkOn);
    }

    @SuppressLint("InflateParams")
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.mbl_image_picker_item, null);

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

    private void loadImage(final Holder holder, final int imageId, final String imagePath) {
        final int id = holder.mId;
        final Bitmap bitmap = sImageLruCache.get(imageId);
        if (bitmap == null) {

            holder.mThumbnailImageView.setImageBitmap(null);
            holder.mThumbnailImageView.setEnabled(false);

            MblUtils.executeOnAsyncThread(new Runnable() {
                @Override
                public void run() {

                    if (holder.mId != id) {
                        return;
                    }

                    Bitmap bm = null;
                    File file = new File(imagePath);
                    if (file.exists() && file.length() > 0) {
                        bm = MediaStore.Images.Thumbnails.getThumbnail(
                                MblUtils.getCurrentContext().getContentResolver(),
                                imageId,
                                MediaStore.Images.Thumbnails.MICRO_KIND,
                                null);
                    }

                    if (bm != null) {

                        // rotate bitmap if needed
                        bm = MblUtils.correctBitmapOrientation(imagePath, bm);

                        sImageLruCache.put(imageId, bm);
                        final Bitmap finalBm = bm;
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if(holder.mId == id) {
                                    holder.mThumbnailImageView.setImageBitmap(finalBm);
                                    holder.mThumbnailImageView.setEnabled(true);
                                }
                            }
                        });
                    } else {
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if(holder.mId == id) {
                                    holder.mThumbnailImageView.setImageBitmap(null);
                                }
                            }
                        });
                    }
                }
            });
        }
        else{
            holder.mThumbnailImageView.setImageBitmap(bitmap);
            holder.mThumbnailImageView.setEnabled(true);
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
    }

    public void clearCache() {
        sImageLruCache.evictAll();
    }
}
