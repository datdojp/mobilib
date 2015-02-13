package com.datdo.mobilib.util;

import java.util.Vector;

import junit.framework.Assert;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * <pre>
 * Smart loader to display images for child views in a {@link ViewGroup}.
 * Features of this loader:
 *   1. Load images sequentially.
 *   2. Automatically scale images to match sizes of {@link ImageView}.
 *   3. Cache images using {@link LruCache}.
 *   4. Prioritize loading by last-recently-displayed, which means {@link ImageView} being displayed has higher priority than {@link ImageView} which is no longer displayed.
 *      This feature is very useful when user scrolls a {@link ListView}.
 * Override abstract methods use customize this loader.
 * 
 * Here is sample usage of this loader:
 * 
 * <code>
 * public class MyAdapter extends BaseAdapter {
 * 
 *     private MblImageLoader{@literal <}Item> mItemImageLoader = new MblImageLoader{@literal <}Item>() {
 *         // override all abstract methods
 *         // ...
 *     };
 * 
 *     {@literal @}Override
 *     public View getView(int pos, View convertView, ViewGroup parent) {
 *         // create or update view
 *         // ...
 * 
 *         mItemImageLoader.loadImage(view);
 * 
 *         return view;
 *     }
 * }
 * </code>
 * </pre>
 * @param <T> class of object bound with an child views of {@link ViewGroup}
 */
public abstract class MblImageLoader<T> {

    /**
     * <pre>
     * Check condition to load image for an item.
     * </pre>
     * @return true if should load image for item
     */
    protected abstract boolean shouldLoadImageForItem(T item);

    /**
     * <pre>
     * Get resource id of default image for items those are not necessary to load image
     * </pre>
     * @see #shouldLoadImageForItem(Object)
     */
    protected abstract int getDefaultImageResource(T item);

    /**
     * <pre>
     * Get resource id of default image for items those fails to load image
     * </pre>
     */
    protected abstract int getErrorImageResource(T item);

    /**
     * <pre>
     * Get resource id of default image for items those are being loaded
     * </pre>
     */
    protected abstract int getLoadingIndicatorImageResource(T item);

    /**
     * <pre>
     * Get data object bound with each child view.
     * </pre>
     */
    protected abstract T getItemBoundWithView(View view);

    /**
     * <pre>
     * Extract {@link ImageView} used to display image from each child view
     * </pre>
     */
    protected abstract ImageView getImageViewFromView(View view);

    /**
     * <pre>
     * Specify an ID for each data object. The ID is used for caching so please make it unique throughout the app.
     * </pre>
     */
    protected abstract String getItemId(T item);

    /**
     * <pre>
     * Do your own image loading here (from HTTP/HTTPS, from file, etc...).
     * This method is always invoked in main thread. Therefore, it is strongly recommended to do the loading asynchronously.
     * </pre>
     * @param item
     * @param cb call method of this callback when you finished the loading
     */
    protected abstract void retrieveImage(T item, MblRetrieveImageCallback cb);

    /**
     * <pre>
     * Callback class for {@link MblImageLoader#retrieveImage(Object, MblRetrieveImageCallback)}
     * When loading image finished, call 1 of 2 methods depending on returned data.
     * If loading image failed, call any method with NULL argument.
     * </pre>
     */
    public static interface MblRetrieveImageCallback {
        public void onRetrievedByteArray(byte[] bmData);
        public void onRetrievedBitmap(Bitmap bm);
        public void onRetrievedFile(String path);
    }



    private static final String TAG                 = MblUtils.getTag(MblImageLoader.class);
    private static final int    DEFAULT_CACHE_SIZE  = 2 * 1024 * 1024; // 2MB
    private static final String CACHE_KEY_SEPARATOR = "#";

    private static final class MblCachedImageData {

        public int resId = 0;
        public Bitmap bitmap;

        protected MblCachedImageData(int resId, Bitmap bitmap) {
            Assert.assertTrue(resId > 0 || bitmap != null);
            Assert.assertFalse(resId > 0 && bitmap != null);
            this.resId = resId;
            this.bitmap = bitmap;
        }
    }

    private final Vector<Pair<T, View>> mQueue          = new Vector<Pair<T,View>>();
    private boolean                     mLoadingImage   = false;

    private static LruCache<String, MblCachedImageData> sStringPictureLruCache;
    private static boolean sDoubleCacheSize = false;

    private static void initCacheIfNeeded() {
        if (sStringPictureLruCache == null) {
            Context context = MblUtils.getCurrentContext();
            int cacheSize = DEFAULT_CACHE_SIZE;
            if (context != null) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
                cacheSize = memoryClassBytes / 8;
            }
            if (sDoubleCacheSize) {
                cacheSize = cacheSize * 2;
            }
            sStringPictureLruCache = new LruCache<String, MblCachedImageData>(cacheSize) {
                @Override
                protected void entryRemoved(boolean evicted, String key, MblCachedImageData oldValue, MblCachedImageData newValue) {
                    Log.v(TAG, "Image cache size: " + size());
                }
                @Override
                protected int sizeOf(String key, MblCachedImageData value) {
                    if (value.bitmap != null) {
                        Bitmap bm = value.bitmap;
                        return bm.getRowBytes() * bm.getHeight();
                    } else if (value.resId > 0) {
                        return 4;
                    }
                    return 0;
                }
            };
        }
    }
    private static MblCachedImageData remove(String key) {
        synchronized (sStringPictureLruCache) {
            return sStringPictureLruCache.remove(key);
        }
    }
    private static void put(String key, MblCachedImageData val) {
        synchronized (sStringPictureLruCache) {
            sStringPictureLruCache.put(key, val);
            Log.v(TAG, "Image cache size: " + sStringPictureLruCache.size());
        }
    }
    private static MblCachedImageData get(String key) {
        return sStringPictureLruCache.get(key);
    }

    public MblImageLoader() {
        initCacheIfNeeded();
    }

    /**
     * <pre>
     * Double memory-cache 's size to increase number of bitmap being kept in memory.
     * Call this method before creating any instance.
     * </pre>
     */
    public static void doubleCacheSize() {
        if (sStringPictureLruCache != null) {
            throw new RuntimeException("doubleCacheSize() must be called before first instance of this class being created");
        }
        sDoubleCacheSize = true;
    }

    /**
     * <pre>
     * Stop loading. This methods should be called when the view did disappear.
     * </pre>
     */
    public void stop() {
        synchronized (mQueue) {
            mQueue.clear();
        }
    }

    /**
     * <pre>
     * Request loading for a child view.
     * The loading request is put into a queue and executed sequentially.
     * </pre>
     * @param view the child view for which you want to load image
     */
    public void loadImage(final View view) {
        MblUtils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                T item = getItemBoundWithView(view);
                final ImageView imageView = getImageViewFromView(view);
                if (item == null || imageView == null) return;
                if (!shouldLoadImageForItem(item)) {
                    setImageViewResource(imageView, getDefaultImageResource(item));
                    return;
                }
                int w = getImageViewWidth(imageView);
                int h = getImageViewHeight(imageView);
                if (w == 0 && h == 0) {
                    final Runnable[] timeoutAction = new Runnable[] { null };
                    final OnGlobalLayoutListener globalLayoutListener = new OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            MblUtils.removeOnGlobalLayoutListener(imageView, this);
                            MblUtils.getMainThreadHandler().removeCallbacks(timeoutAction[0]);
                            loadImage(view);
                        }
                    };
                    timeoutAction[0] = new Runnable() {
                        @Override
                        public void run() {
                            MblUtils.removeOnGlobalLayoutListener(imageView, globalLayoutListener);
                            loadImage(view);
                        }
                    };
                    imageView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
                    MblUtils.getMainThreadHandler().postDelayed(timeoutAction[0], 500l);
                    return;
                }

                String fullCacheKey = getFullCacheKey(item, w, h);
                MblCachedImageData pic = get(fullCacheKey);
                if(pic != null) {
                    if (pic.bitmap != null) {
                        Bitmap bm = pic.bitmap;
                        if (!bm.isRecycled()) {
                            imageView.setImageBitmap(bm);
                        } else {
                            remove(fullCacheKey);
                            handleBitmapUnavailable(view, imageView, item);
                        }
                    } else if (pic.resId > 0) {
                        setImageViewResource(imageView, pic.resId);
                    }
                } else {
                    handleBitmapUnavailable(view, imageView, item);
                }
            }
        });
    }

    private void handleBitmapUnavailable(View view, ImageView imageView, T item) {
        setImageViewResource(imageView, getLoadingIndicatorImageResource(item));
        synchronized (mQueue) {
            mQueue.add(new Pair<T, View>(item, view));
        }
        loadNextImage();
    }

    private Pair<T, View> getNextPair() {
        synchronized (mQueue) {
            if (mQueue.isEmpty()) {
                return null;
            } else {
                return mQueue.remove(0);
            }
        }
    }

    private boolean isItemBoundWithView(T item, View view) {

        // if item and view 's item are same object, just return TRUE
        T viewItem = getItemBoundWithView(view);
        if (item != null && item == viewItem) {
            return true;
        }

        // otherwise, compare id
        String id1 = item != null ? getItemId(item) : null;
        String id2 = viewItem != null ? getItemId(viewItem) : null;
        return id1 != null && id2 != null && TextUtils.equals(id1, id2);
    }

    private void loadNextImage() {

        if (mLoadingImage) return;

        Pair<T, View> pair = getNextPair();
        if (pair == null) return;
        final T item =  pair.first;
        final View view = pair.second;
        final ImageView imageView = getImageViewFromView(view);

        if (!isItemBoundWithView(item, view)) {
            MblUtils.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    loadNextImage();
                }
            });
            return;
        }

        if (!shouldLoadImageForItem(item)) {
            setImageViewResource(imageView, getDefaultImageResource(item));
            MblUtils.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    loadNextImage();
                }
            });
            return;
        }

        final String fullCacheKey = getFullCacheKey(
                item,
                getImageViewWidth(imageView),
                getImageViewHeight(imageView));
        MblCachedImageData pic = get(fullCacheKey);
        if(pic != null) {
            boolean isSet = false;

            if (pic.bitmap != null) {
                Bitmap bm = pic.bitmap;
                if (!bm.isRecycled()) {
                    imageView.setImageBitmap(bm);
                    isSet = true;
                } else {
                    remove(fullCacheKey);
                }
            } else if (pic.resId > 0) {
                setImageViewResource(imageView, pic.resId);
                isSet = true;
            }

            if (isSet) {
                MblUtils.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        loadNextImage();
                    }
                });
                return;
            }
        }

        mLoadingImage = true;
        final boolean isNetworkConnected = MblUtils.isNetworkConnected();
        retrieveImage(item, new MblRetrieveImageCallback() {
            @Override
            public void onRetrievedByteArray(final byte[] bmData) {
                if (MblUtils.isEmpty(bmData)) {
                    handleBadReturnedBitmap(item, view, fullCacheKey, !isNetworkConnected);
                } else {
                    MblUtils.executeOnAsyncThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int w = getImageViewWidth(imageView);
                                int h = getImageViewHeight(imageView);
                                Bitmap bm = MblUtils.loadBitmapMatchSpecifiedSize(w, h, bmData);
                                if (bm == null) {
                                    handleBadReturnedBitmap(item, view, fullCacheKey, !isNetworkConnected);
                                } else {
                                    Log.d(TAG, "Scale bitmap: w=" + w + ", h=" + h +
                                            ", bm.w=" + bm.getWidth() + ", bm.h=" + bm.getHeight());
                                    handleGoodReturnedBitmap(item, view, fullCacheKey, bm);
                                }
                            } catch (OutOfMemoryError e) {
                                Log.e(TAG, "OutOfMemoryError", e);
                                handleOutOfMemory(item, view, fullCacheKey);
                            }
                        }
                    });
                }
            }

            @Override
            public void onRetrievedFile(final String path) {
                if (MblUtils.isEmpty(path)) {
                    handleBadReturnedBitmap(item, view, fullCacheKey, !isNetworkConnected);
                } else {
                    MblUtils.executeOnAsyncThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int w = getImageViewWidth(imageView);
                                int h = getImageViewHeight(imageView);
                                Bitmap bm = MblUtils.loadBitmapMatchSpecifiedSize(w, h, path);
                                if (bm == null) {
                                    handleBadReturnedBitmap(item, view, fullCacheKey, !isNetworkConnected);
                                } else {
                                    Log.d(TAG, "Scale bitmap: w=" + w + ", h=" + h +
                                            ", bm.w=" + bm.getWidth() + ", bm.h=" + bm.getHeight());
                                    handleGoodReturnedBitmap(item, view, fullCacheKey, bm);
                                }
                            } catch (OutOfMemoryError e) {
                                Log.e(TAG, "OutOfMemoryError", e);
                                handleOutOfMemory(item, view, fullCacheKey);
                            }
                        }
                    });
                }
            }

            @Override
            public void onRetrievedBitmap(Bitmap bm) {
                if (bm == null) {
                    handleBadReturnedBitmap(item, view, fullCacheKey, !isNetworkConnected);
                } else {
                    handleGoodReturnedBitmap(item, view, fullCacheKey, bm);
                }
            }
        });
    }

    private void handleGoodReturnedBitmap(final T item, final View view, final String fullCacheKey, final Bitmap bm) {
        MblUtils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                put(fullCacheKey, new MblCachedImageData(0, bm));
                postLoadImageForItem(item, view);
            }
        });

    }

    private void handleBadReturnedBitmap(final T item, final View view, final String fullCacheKey, final boolean shouldRetry) {
        MblUtils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                int errorImageRes = getErrorImageResource(item);
                if (errorImageRes > 0) {
                    put(fullCacheKey, new MblCachedImageData(errorImageRes, null));
                }
                postLoadImageForItem(item, view);

                // failed due to network disconnect -> should try to load later
                if (shouldRetry) {
                    MblUtils.getMainThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            remove(fullCacheKey);
                        }
                    });
                }
            }
        });
    }

    private void handleOutOfMemory(final T item, final View view, final String fullCacheKey) {
        MblUtils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                // release 1/2 of cache size for memory
                synchronized (sStringPictureLruCache) {
                    sStringPictureLruCache.trimToSize(sStringPictureLruCache.size()/2);
                }
                System.gc();

                handleBadReturnedBitmap(item, view, fullCacheKey, true);
            }
        });
    }

    private void postLoadImageForItem(final T item, final View view) {
        if (isItemBoundWithView(item, view)) {
            loadImage(view);
        }

        // run loadNextImage() using "post" to prevent deep recursion
        MblUtils.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                mLoadingImage = false;
                loadNextImage();
            }
        });
    }

    private void setImageViewResource(ImageView imageView, int resId) {
        if (resId <= 0) {
            imageView.setImageBitmap(null);
        } else {
            imageView.setImageResource(resId);
        }
    }

    private String getFullCacheKey(T item, int w, int h) {
        String key = TextUtils.join(CACHE_KEY_SEPARATOR, new Object[] {
                item.getClass().getSimpleName(),
                MblUtils.md5(getItemId(item)),
                w,
                h
        });
        return key;
    }

    private int getImageViewWidth(ImageView imageView) {
        LayoutParams lp = imageView.getLayoutParams();
        if (lp.width == LayoutParams.WRAP_CONTENT) {
            return -1; // do not care
        } else if (lp.width == LayoutParams.MATCH_PARENT){
            return imageView.getWidth(); // 0 or parent 's width
        } else {
            return lp.width; // specified width
        }
    }

    private int getImageViewHeight(ImageView imageView) {
        LayoutParams lp = imageView.getLayoutParams();
        if (lp.height == LayoutParams.WRAP_CONTENT) {
            return -1; // do not care
        } else if (lp.height == LayoutParams.MATCH_PARENT){
            return imageView.getHeight(); // 0 or parent 's height
        } else {
            return lp.height; // specified height
        }
    }
}
