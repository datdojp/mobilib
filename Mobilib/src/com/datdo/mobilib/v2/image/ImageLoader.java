package com.datdo.mobilib.v2.image;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.datdo.mobilib.api.MblApi;
import com.datdo.mobilib.api.MblRequest;
import com.datdo.mobilib.api.MblResponse;
import com.datdo.mobilib.util.MblSerializer;
import com.datdo.mobilib.util.MblUtils;
import com.nineoldandroids.animation.ObjectAnimator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ImageLoader {

    private static final String TAG = ImageLoader.class.getSimpleName();

    public LoadRequest forOneImageView(Context context) {
        return with(context)
                .scaleToImageViewSizes(true)
                .cropBitmapToImageViewSizes(false)
                .serialized(false)
                .loadDelayed(0)
                .enableProgressView(false)
                .enableFadingAnimation(true);
    }

    public LoadRequest with(Context context) {
        LoadRequest info = new LoadRequest();
        info.context = context;
        info.imageLoader = this;
        return info;
    }

    public static class LoadRequest {
        private Context context;
        private ImageLoader imageLoader;
        private String url;
        private File file;
        private byte[] bytes;
        private CustomLoad customLoad;
        private int placeHolderResId;
        private int errorResId;
        private FittingType fittingType = FittingType.GTE;
        private boolean scaleToImageViewSizes = true;
        private int scaleToWidth;
        private int scaleToHeight;
        private boolean cropBitmapToImageViewSizes = true;
        private Transformation transformation;
        private boolean serialized;
        private long loadDelayed = 500;
        private boolean enableProgressView = true;
        private boolean enableFadingAnimation = true;
        private Callback callback;

        String key(int toWidth, int toHeight) {
            List<String> tokens = new ArrayList<>();
            if (url != null) {
                tokens.add("src=" + url);
            }
            else if (file != null) {
                tokens.add("src=" + file.getAbsolutePath());
            }
            else if (bytes != null) {
                tokens.add("src=" + Arrays.hashCode(bytes));
            }
            if (customLoad != null) {
                tokens.add("customLoad=" + customLoad.key());
            }
            tokens.add("scaleToImageViewSizes=" + scaleToImageViewSizes);
            tokens.add("toWidth=" + toWidth);
            tokens.add("toHeight=" + toHeight);
            tokens.add("fittingType=" + fittingType.name());
            tokens.add("cropBitmapToImageViewSizes=" + cropBitmapToImageViewSizes);
            if (transformation != null) {
                tokens.add("transformation=" + transformation.key());
            }
            return TextUtils.join(";", tokens);
        }

        public LoadRequest load(String url) {
            this.url = url;
            if (!MblUtils.isWebUrl(url)) {
                if (imageLoader != null) {
                    imageLoader.invalidUrls.add(url);
                }
            }
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

        public LoadRequest customLoad(CustomLoad customLoad) {
            this.customLoad = customLoad;
            return this;
        }

        public LoadRequest placeholder(int placeHolderResId) {
            this.placeHolderResId = placeHolderResId;
            return this;
        }

        public LoadRequest error(int errorResId) {
            this.errorResId = errorResId;
            return this;
        }

        public LoadRequest fittingType(FittingType fittingType) {
            this.fittingType = fittingType;
            return this;
        }

        public LoadRequest scaleToImageViewSizes(boolean scaleToImageViewSizes, int scaleToWidth, int scaleToHeight) {
            if (!scaleToImageViewSizes && (scaleToWidth == 0 || scaleToHeight == 0)) {
                throw new IllegalStateException("If you don't want to scale bitmap to ImageView 's sizes, please specify scaleToWidth and scaleToWidth. Specify -1 if you don't care about the size");
            }
            this.scaleToImageViewSizes = scaleToImageViewSizes;
            this.scaleToWidth = scaleToWidth;
            this.scaleToHeight = scaleToHeight;
            return this;
        }

        public LoadRequest scaleToImageViewSizes(boolean scaleToImageViewSizes) {
            return scaleToImageViewSizes(scaleToImageViewSizes, scaleToWidth, scaleToHeight);
        }

        public LoadRequest cropBitmapToImageViewSizes(boolean cropBitmapToImageViewSizes) {
            this.cropBitmapToImageViewSizes = cropBitmapToImageViewSizes;
            return this;
        }

        public LoadRequest transformation(Transformation transformation) {
            this.transformation = transformation;
            return this;
        }

        public LoadRequest serialized(boolean serialized) {
            this.serialized = serialized;
            return this;
        }

        public LoadRequest loadDelayed(long loadDelayed) {
            this.loadDelayed = loadDelayed;
            return this;
        }

        public LoadRequest enableProgressView(boolean enableProgressView) {
            this.enableProgressView = enableProgressView;
            return this;
        }

        public LoadRequest enableFadingAnimation(boolean enableFadingAnimation) {
            this.enableFadingAnimation = enableFadingAnimation;
            return this;
        }

        public LoadRequest callback(Callback callback) {
            this.callback = callback;
            return this;
        }

        public void into(ImageView imageView) {
            if (imageView.getTag() != null && !(imageView.getTag() instanceof LoadRequest)) {
                throw new IllegalStateException("ImageView 's tag is used by "
                        + ImageLoader.class.getSimpleName()
                        + ", you should not set any value for it.");
            }
            if (!oneObjectNotNull(url, file, bytes, customLoad)) {
                throw new IllegalStateException("You must specify one and only one of these options: url, file, bytes, customeLoad");
            }
            imageView.setTag(this);
            imageLoader.load(imageView, this);
            imageLoader = null;
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

    public interface Transformation {
        Bitmap transform(ImageView imageView, LoadRequest request, Bitmap source);
        String key();
    }

    public interface CustomLoad {
        /**
         * <pre>
         * This method is executed on async thread
         * </pre>
         */
        void load(ImageView imageView, LoadRequest request, CustomLoadCallback callback);
        String key();
    }

    public interface CustomLoadCallback {
        void onSuccess(byte[] bytes);
        void onSuccess(File file);
        void onError(Throwable t);
    }

    public interface Callback {
        void onSuccess(ImageView image, Bitmap bm, LoadRequest request);
        void onError(Throwable t, ImageView image, LoadRequest request);
    }

    public void clearMemmoryCache() {
        memoryCache.evictAll();
    }

    protected void onBeforeLoad(ImageView imageView, LoadRequest request) {
        if (request.placeHolderResId > 0) {
            imageView.setImageResource(request.placeHolderResId);
        } else {
            imageView.setImageBitmap(null);
        }
    }

    protected void onSuccess(ImageView imageView, LoadRequest request, Bitmap bm) {
        if (request.callback != null) {
            request.callback.onSuccess(imageView, bm, request);
        }
    }

    protected void onError(Throwable t, ImageView image, LoadRequest request) {
        if (request.errorResId > 0) {
            image.setImageResource(request.errorResId);
        } else {
            image.setImageBitmap(null);
        }
        if (request.callback != null) {
            request.callback.onError(t, image, request);
        }
    }

    private final int FRAME_ID = new Random().nextInt();
    private LruCache<String, Bitmap> memoryCache;
    private Set<String> memoryCacheKeySet;
    private Set<String> invalidUrls;
    private MblSerializer serializer;
    private Map<String, Set<Runnable>> pendingLoads;

    public ImageLoader(Context context) {

        // initialize memory cache
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        int cacheSize = memoryClassBytes / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                memoryCacheKeySet.remove(key);
            }
        };
        memoryCacheKeySet = Collections.synchronizedSet(new HashSet<String>());

        // ...others
        serializer = new MblSerializer();
        invalidUrls = Collections.synchronizedSet(new HashSet<String>());
        pendingLoads = Collections.synchronizedMap(new HashMap<String, Set<Runnable>>());
    }

    private void load(final ImageView imageView, final LoadRequest request) {

        // check if request bound with imageView
        if (!isStillBound(imageView, request)) {
            return;
        }

        // check if this method is executed on main thread
        if (!MblUtils.isMainThread()) {
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (isStillBound(imageView, request)) {
                        load(imageView, request);
                    }
                }
            });
            return;
        }

        // check if we have sizes. If we don't have, wait until ImageView is fully displayed
        final int w = request.scaleToImageViewSizes ? getImageViewWidth(imageView) : request.scaleToWidth;
        final int h = request.scaleToImageViewSizes ? getImageViewHeight(imageView) : request.scaleToHeight;
        if (!isValidSizes(w, h)) {
            final Runnable[] timeoutAction = new Runnable[]{null};
            final OnGlobalLayoutListener globalLayoutListener = new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    MblUtils.removeOnGlobalLayoutListener(imageView, this);
                    MblUtils.getMainThreadHandler().removeCallbacks(timeoutAction[0]);
                    if (isStillBound(imageView, request)) {
                        load(imageView, request);
                    }
                }
            };
            timeoutAction[0] = new Runnable() {
                @Override
                public void run() {
                    MblUtils.removeOnGlobalLayoutListener(imageView, globalLayoutListener);
                    if (isStillBound(imageView, request)) {
                        load(imageView, request);
                    }
                }
            };
            imageView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
            MblUtils.getMainThreadHandler().postDelayed(timeoutAction[0], 500l);
            return;
        }


        // get key for this request
        final String key = request.key(w, h);

        // check memory cache
        final Bitmap bm = memoryCache.get(key);
        if (isValidBitmap(bm)) {
            hideProgressBar(imageView, request);
            imageView.setImageBitmap(bm);
            onSuccess(imageView, request, bm);
            return;
        }

        // check disk cache
        File diskCacheFile = getDiskCachedFile(key);
        boolean hasValidDiskCache = MblUtils.isValidFile(diskCacheFile);

        // check if loading from an invalid url
        if (request.url != null && invalidUrls.contains(request.url)) {
            onError(new RuntimeException("Failed to download image from url: " + request.url),
                    imageView,
                    request);
            return;
        }

        // task to load bitmap. task may be executed serially or parallel
        onBeforeLoad(imageView, request);
        showProgressBar(imageView, request, w, h);
        final MblSerializer.Task task = new MblSerializer.Task() {
            @Override
            public void run(final Runnable finishCallback) {

                // check if view is still bound with original item
                if (!isStillBound(imageView, request)) {
                    finishCallback.run();
                    return;
                }

                // check if bitmap is in memory cache
                Bitmap bm = memoryCache.get(key);
                if (isValidBitmap(bm)) {
                    hideProgressBar(imageView, request);
                    imageView.setImageBitmap(bm);
                    onSuccess(imageView, request, bm);
                    finishCallback.run();
                    return;
                }

                // load bitmap from server/file
                // prevent duplicated loads by putting requests having the same key into pending
                if (pendingLoads.containsKey(key)) {
                    Set<Runnable> loads = pendingLoads.get(key);
                    if (loads != null) {
                        loads.add(new Runnable() {
                            @Override
                            public void run() {
                                if (isStillBound(imageView, request)) {
                                    load(imageView, request);
                                }
                            }
                        });
                        finishCallback.run();
                        return;
                    }
                }
                pendingLoads.put(key, new HashSet<Runnable>());
                final Runnable extendedFinishCallback = new Runnable() {
                    @Override
                    public void run() {
                        finishCallback.run();
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Set<Runnable> loads = pendingLoads.remove(key);
                                if (loads != null) {
                                    for (Runnable r : loads) {
                                        r.run();
                                    }
                                }
                            }
                        });
                    }
                };
                loadBitmapFromSource(imageView, request, key, new LoadBitmapFromSourceCallback() {

                    private void handleSuccess(final Object data, final boolean fromDiskCache) {

                        if (!isStillBound(imageView, request)) {
                            extendedFinishCallback.run();
                            return;
                        }

                        MblUtils.executeOnAsyncThread(new Runnable() {

                            @Override
                            public void run() {

                                if (!isStillBound(imageView, request)) {
                                    extendedFinishCallback.run();
                                    return;
                                }

                                try {
                                    final Bitmap[] bm = new Bitmap[] { null };
                                    if (data instanceof File) {
                                        if (!fromDiskCache) {
                                            bm[0] = ImageTool.loadBitmap(w, h, (File) data, request.fittingType);
                                        } else {
                                            bm[0] = ImageTool.loadBitmap(-1, -1, (File) data, request.fittingType);
                                        }
                                    } else if (data instanceof byte[]) {
                                        bm[0] = ImageTool.loadBitmap(w, h, (byte[]) data, request.fittingType);
                                    } else {
                                        bm[0] = null;
                                    }
                                    if (isValidBitmap(bm[0])) {
                                        if (!fromDiskCache) {
                                            if (request.cropBitmapToImageViewSizes) {
                                                bm[0] = cropBitmapToImageViewSizes(bm[0], w, h);
                                            }
                                            if (request.transformation != null) {
                                                Bitmap oldBm = bm[0];
                                                bm[0] = request.transformation.transform(imageView, request, bm[0]);
                                                if (oldBm != bm[0] && !oldBm.isRecycled()) {
                                                    throw new RuntimeException("You must recycle bitmap after transforming.");
                                                }
                                            }

                                            // save to disk cache
                                            bm[0].compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(getDiskCachedFile(key)));
                                        }
                                        memoryCache.put(key, bm[0]);
                                        memoryCacheKeySet.add(key);

                                        MblUtils.executeOnMainThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isStillBound(imageView, request)) {
                                                    hideProgressBar(imageView, request);
                                                    imageView.setImageBitmap(bm[0]);
                                                    ImageLoader.this.onSuccess(imageView, request, bm[0]);
                                                    animateImageView(imageView, request);
                                                }
                                                extendedFinishCallback.run();
                                            }
                                        });
                                    } else {
                                        onError(new IllegalStateException("Bitmap is null, recycled, or having 0 width/height"));
                                    }
                                } catch (OutOfMemoryError e) {
                                    Log.e(TAG, "OutOfMemoryError", e);

                                    // release 1/2 of cache size for memory
                                    memoryCache.trimToSize(memoryCache.size() / 2);
                                    System.gc();

                                    // try to load bitmap in lower fitting type
                                    if (request.fittingType != FittingType.LTE) {
                                        request.fittingType = FittingType.LTE;
                                        MblUtils.getMainThreadHandler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isStillBound(imageView, request)) {
                                                    load(imageView, request);
                                                }
                                            }
                                        }, 500);
                                    } else {
                                        onError(e);
                                    }
                                } catch (Throwable t) {
                                    Log.e(TAG, "", t);
                                    onError(t);
                                }
                            }
                        });
                    }

                    @Override
                    public void onSuccess(byte[] bytes) {
                        handleSuccess(bytes, false);
                    }

                    @Override
                    public void onSuccess(File file, boolean fromDiskCache) {
                        handleSuccess(file, fromDiskCache);
                    }

                    @Override
                    public void onError(final Throwable t) {
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isStillBound(imageView, request)) {
                                    hideProgressBar(imageView, request);
                                    ImageLoader.this.onError(t, imageView, request);
                                }
                                extendedFinishCallback.run();
                            }
                        });
                    }
                });
            }
        };
        if (request.serialized) {
            serializer.run(task);
        } else {
            if (request.url != null && !hasValidDiskCache) {
                MblUtils.getMainThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        task.run(emptyRunnable);
                    }
                }, request.loadDelayed);
            } else {
                task.run(emptyRunnable);
            }
        }
    }

    private Runnable emptyRunnable = new Runnable() {
        @Override
        public void run() {}
    };

    private interface LoadBitmapFromSourceCallback {
        void onSuccess(byte[] bytes);
        void onSuccess(File file, boolean fromDiskCache);
        void onError(Throwable t);
    }

    private void loadBitmapFromSource(final ImageView imageView, final LoadRequest request, String key, final LoadBitmapFromSourceCallback cb) {
        // check if we have disk cache
        File diskCacheFile = getDiskCachedFile(key);
        if (MblUtils.isValidFile(diskCacheFile)) {
            cb.onSuccess(diskCacheFile, true);
            return;
        }

        // check if use wants to use custom load
        if (request.customLoad != null) {
            MblUtils.executeOnAsyncThread(new Runnable() {
                @Override
                public void run() {
                    request.customLoad.load(imageView, request, new CustomLoadCallback() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            cb.onSuccess(bytes);
                        }

                        @Override
                        public void onSuccess(File file) {
                            cb.onSuccess(file, false);
                        }

                        @Override
                        public void onError(Throwable t) {
                            cb.onError(t);
                        }
                    });
                }
            });
            return;
        }

        // load from server
        if (request.url != null) {
            String path = MblApi.getCacheFilePath(request.url, null);
            if (path != null) {
                cb.onSuccess(new File(path), false);
            } else {
                MblApi.run(new MblRequest()
                        .setMethod(MblApi.Method.GET)
                        .setUrl(request.url)
                        .setCacheDuration(Long.MAX_VALUE)
                        .setRedirectEnabled(true)
                        .setNotReturnByteArrayData(true)
                        .setCallback(new MblApi.MblApiCallback() {
                            @Override
                            public void onSuccess(MblResponse response) {
                                String path = MblApi.getCacheFilePath(request.url, null);
                                cb.onSuccess(new File(path), false);
                            }

                            @Override
                            public void onFailure(MblResponse response) {
                                cb.onError(new RuntimeException("Failed to download image from url: " + request.url));
                                if (response.getStatusCode() > 0 && response.getStatusCode() != 200) {
                                    invalidUrls.add(request.url);
                                }
                            }
                        }));
            }
        }
        // ... or load from local file
        else if (request.file != null){
            cb.onSuccess(request.file, false);
        }
        // ... or load from byte array
        else if (request.bytes != null) {
            cb.onSuccess(request.bytes);
        }
        // ... ???
        else {
            cb.onError(new IllegalStateException("Url, file or byte array is required"));
        }
    }

    private View getProgressView(LoadRequest item) {
        ProgressBar progress = new ProgressBar(item.context);
        progress.setIndeterminate(true);
        return progress;
    }

    @SuppressWarnings("ResourceType")
    private void showProgressBar(ImageView imageView, LoadRequest request, int w, int h) {

        if (!request.enableProgressView) {
            return;
        }

        // get parent view of ImageView
        ViewGroup parent = (ViewGroup) imageView.getParent();

        // check if we need to show progress bar
        if (parent == null || parent.getId() == FRAME_ID) {
            return;
        }

        // create Frame
        final FrameLayout frame = new FrameLayout(imageView.getContext());
        frame.setId(FRAME_ID);
        frame.setLayoutParams(imageView.getLayoutParams());

        // change ImageView
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setVisibility(View.INVISIBLE);

        // add/remove views
        int index = parent.indexOfChild(imageView);
        parent.removeView(imageView);
        frame.addView(imageView);
        parent.addView(frame, index);

        // create ProgressBar and add it to frame
        View progressView = getProgressView(request);
        final int MIN_SIZE = MblUtils.pxFromDp(30);
        int lpW = w > 0 ? Math.min(w / 3, MIN_SIZE) : MIN_SIZE;
        int lpH = h > 0 ? Math.min(h / 3, MIN_SIZE) : MIN_SIZE;
        int lpWH = Math.min(lpW, lpH);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(lpWH, lpWH);
        lp.gravity = Gravity.CENTER;
        progressView.setLayoutParams(lp);
        frame.addView(progressView);
    }

    @SuppressWarnings("ResourceType")
    private void hideProgressBar(ImageView imageView, LoadRequest request) {

        if (!request.enableProgressView) {
            return;
        }

        // get parent view of ImageView
        ViewGroup temp = (ViewGroup) imageView.getParent();

        // check if we need to hide progress bar
        if (temp == null || temp.getId() != FRAME_ID) {
            return;
        }

        // frame & parent
        FrameLayout frame = (FrameLayout) temp;
        if (frame.getTag() != null) {
            MblUtils.removeOnGlobalLayoutListener(frame, (OnGlobalLayoutListener)frame.getTag());
            frame.setTag(null);
        }
        ViewGroup parent = (ViewGroup) frame.getParent();

        // change ImageView
        imageView.setVisibility(View.VISIBLE);
        imageView.setLayoutParams(frame.getLayoutParams());

        // add/remove views
        int index = parent.indexOfChild(frame);
        frame.removeView(imageView);
        parent.removeView(frame);
        parent.addView(imageView, index);
    }

    private void animateImageView(ImageView imageView, LoadRequest request) {
        if (!request.enableFadingAnimation) {
            return;
        }
        ObjectAnimator.ofFloat(imageView, "alpha", 0, 1)
                .setDuration(250)
                .start();
    }

    private int getImageViewWidth(ImageView imageView) {
        return imageView.getWidth();
    }

    private int getImageViewHeight(ImageView imageView) {
        return imageView.getHeight();
    }

    private boolean isValidBitmap(Bitmap bm) {
        return bm != null && !bm.isRecycled() && bm.getWidth() != 0 && bm.getHeight() != 0;
    }

    private boolean isValidSizes(int w, int h) {
        return w != 0 && h != 0;
    }

    private boolean isStillBound(ImageView imageView, LoadRequest request) {
        return request == imageView.getTag();
    }

    private Bitmap cropBitmapToImageViewSizes(Bitmap bm, int w, int h) {
        if (bm.getWidth() <= w && bm.getHeight() <= h) {
            return bm;
        }
        Bitmap result;
        if (bm.getWidth() >= bm.getHeight()){
            result = Bitmap.createBitmap(
                    bm,
                    bm.getWidth()/2 - bm.getHeight()/2,
                    0,
                    bm.getHeight(),
                    bm.getHeight()
            );
        } else {
            result = Bitmap.createBitmap(
                    bm,
                    0,
                    bm.getHeight()/2 - bm.getWidth()/2,
                    bm.getWidth(),
                    bm.getWidth()
            );
        }
        if (result != bm) {
            bm.recycle();
        }
        return result;
    }

    private File getDiskCachedFile(String key) {
        return new File(MblUtils.getCacheAsbPath(
                ImageLoader.class.getSimpleName() + "_" + MblUtils.md5(key) + ".jpg"));
    }
}
