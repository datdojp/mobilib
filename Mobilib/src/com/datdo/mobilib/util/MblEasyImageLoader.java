package com.datdo.mobilib.util;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.datdo.mobilib.api.MblApi;
import com.datdo.mobilib.api.MblRequest;
import com.datdo.mobilib.api.MblResponse;

import junit.framework.Assert;

import java.io.File;

/**
 * Created by dat on 2016/04/25.
 *
 */
public class MblEasyImageLoader extends MblSimpleImageLoader<MblEasyImageLoader.LoadingInfo> {

    public static class LoadingInfo {
        MblEasyImageLoader imageLoader;
        String url;
        File file;
        int errorResId;
        MblCallback callback;

        public LoadingInfo error(int errorResId) {
            this.errorResId = errorResId;
            return this;
        }

        public LoadingInfo callback(MblCallback callback) {
            this.callback = callback;
            return this;
        }

        public void into(ImageView image) {
            Assert.assertTrue(image.getTag() == null || image.getTag() instanceof LoadingInfo);
            image.setTag(this);
            imageLoader.loadImage(image);
            imageLoader = null;
        }
    }

    public interface MblCallback {
        void onSuccess(ImageView image, Bitmap bm);
        void onError(ImageView image);
    }

    static MblEasyImageLoader instance;

    public synchronized static MblEasyImageLoader getInstance() {
        if (instance == null) {
            instance = new MblEasyImageLoader();
        }
        return instance;
    }

    public MblEasyImageLoader() {
        setOptions(new MblOptions().setSerializeImageLoading(false));
    }

    public LoadingInfo load(String url) {
        LoadingInfo info = new LoadingInfo();
        info.imageLoader = this;
        info.url = url;
        return info;
    }

    public LoadingInfo load(File file) {
        LoadingInfo info = new LoadingInfo();
        info.imageLoader = this;
        info.file = file;
        return info;
    }

    @Override
    protected LoadingInfo getItemBoundWithView(View image) {
        return (LoadingInfo) image.getTag();
    }

    @Override
    protected ImageView getImageViewBoundWithView(View image) {
        return (ImageView) image;
    }

    @Override
    protected String getItemId(LoadingInfo data) {
        if (data.url != null) {
            return data.url;
        }
        else if (data.file != null) {
            return data.file.getAbsolutePath();
        }
        else {
            return null;
        }
    }

    @Override
    protected void retrieveImage(final LoadingInfo data, final MblRetrieveImageCallback cb) {
        // load from server
        if (data.url != null) {
            MblApi.run(new MblRequest()
                    .setMethod(MblApi.Method.GET)
                    .setUrl(data.url)
                    .setCacheDuration(Long.MAX_VALUE)
                    .setCallback(new MblApi.MblApiCallback() {
                        @Override
                        public void onSuccess(MblResponse response) {
                            String path = MblApi.getCacheFilePath(data.url, null);
                            cb.onRetrievedFile(path);
                        }

                        @Override
                        public void onFailure(MblResponse response) {
                            cb.onRetrievedError();
                        }
                    }));
        }

        // ... or load from local file
        else if (data.file != null){
            cb.onRetrievedFile(data.file.getAbsolutePath());
        }
        // ???
        else {
            cb.onRetrievedError();
        }
    }

    @Override
    protected void onSuccess(ImageView imageView, LoadingInfo data, Bitmap bm) {
        if (data.callback != null) {
            data.callback.onSuccess(imageView, bm);
        }
    }

    @Override
    protected void onError(ImageView image, LoadingInfo data) {
        if (data.errorResId > 0) {
            image.setImageResource(data.errorResId);
        } else {
            image.setImageBitmap(null);
        }
        if (data.callback != null) {
            data.callback.onError(image);
        }
    }

    @Deprecated
    @Override
    public void loadImage(View view) {
        super.loadImage(view);
    }
}
