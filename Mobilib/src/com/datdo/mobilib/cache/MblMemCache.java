package com.datdo.mobilib.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import com.datdo.mobilib.util.MblUtils;

/**
 * Cache objects in memory until they are expired. Obviously all accesses to cached object must be thread-safe.
 * @param <T> class of object being cached
 */
public class MblMemCache<T> {

    private static class CacheItem<T> {

        T       mObject;
        long    mPutAt;

        CacheItem(T object, long putAt) {
            mObject = object;
            mPutAt = putAt;
        }
    }

    /**
     * Interface to iterate through all object of this cache.
     * @see #iterateWithCallback(com.datdo.mobilib.cache.MblMemCache.IterateCallback)
     */
    public static interface IterateCallback<T> {
        public void onInterate(T object);
    }

    private final Map<String, CacheItem<T>> mMap = new HashMap<String, CacheItem<T>>();
    private long mDuration;

    /**
     * Constructor.
     * @param duration time in milliseconds before an object become expired
     */
    public MblMemCache(long duration) {
        mDuration = duration;
    }

    /**
     * Put an object to cache.
     * @param id key
     * @param object value
     */
    public void put(String id, T object) {
        put(id, object, System.currentTimeMillis());
    }

    /**
     * Put an object to cache and specify when the object was retrieved
     * @param id key
     * @param object value
     * @param putAt when the object was retrieved (in milliseconds)
     */
    public void put(String id, T object, long putAt) {
        synchronized (this) {
            CacheItem<T> cacheItem = mMap.get(id);
            if (cacheItem == null) {
                cacheItem = new CacheItem<T>(object, putAt);
            } else {
                cacheItem.mObject   = object;
                cacheItem.mPutAt    = putAt;
            }
            mMap.put(id, cacheItem);
        }
    }

    /**
     * Get an object from cache by its id.
     * @param id key
     * @return object if it exists in cache and not expired, otherwise return nul
     */
    public T get(String id) {
        synchronized (this) {
            CacheItem<T> cacheItem = mMap.get(id);
            if (cacheItem == null) {
                return null;
            } else {
                if (mDuration <= 0 || System.currentTimeMillis() - cacheItem.mPutAt < mDuration) {
                    return cacheItem.mObject;
                } else {
                    mMap.remove(id);
                    return null;
                }
            }
        }
    }

    /**
     * Like {@link #get(String)}, but for multiple ids.
     */
    public List<T> get(List<String> ids) {

        if (MblUtils.isEmpty(ids)) {
            return new ArrayList<T>();
        }

        synchronized (this) {
            List<T> ret = new ArrayList<T>();
            for (String id : ids) {
                T object = get(id);
                if (object != null) {
                    ret.add(object);
                }
            }
            return ret;
        }
    }

    /**
     * Remove an object from cache by its id.
     * @param id key
     * @return removed object if it exists in cache, otherwise null
     */
    public T remove(String id) {
        synchronized (this) {
            CacheItem<T> cacheItem = mMap.get(id);
            if (cacheItem == null) {
                return null;
            } else {
                mMap.remove(id);
                return cacheItem.mObject;
            }
        }
    }

    /**
     * Like {@link #remove(String)}, but for multiple ids.
     */
    public List<T> remove(List<String> ids) {
        synchronized (this) {
            List<T> ret = new ArrayList<T>();
            for (String id : ids) {
                T object = remove(id);
                if (object != null) {
                    ret.add(object);
                }
            }
            return ret;
        }
    }

    /**
     * iterate through all object of this cache.
     * @see com.datdo.mobilib.cache.MblMemCache.IterateCallback
     */
    public void iterateWithCallback(IterateCallback<T> callback) {

        Assert.assertNotNull(callback);

        synchronized (this) {
            Set<String> ids = new HashSet<String>(mMap.keySet());
            for (String id : ids) {
                T o = get(id);
                if (o != null) {
                    callback.onInterate(o);
                }
            }
        }
    }

    /**
     * Check if object exists in cache by its id.
     * @param id key
     * @return true if object exists in cache
     */
    public boolean containsKey(String id) {
        synchronized (this) {
            return get(id) != null;
        }
    }

    /**
     * Remove all objects.
     */
    public void clear() {
        synchronized (this) {
            mMap.clear();
        }
    }

    /**
     * Get time in milliseconds before an object become expired.
     */
    public long getDuration() {
        return mDuration;
    }

    /**
     * Set time in milliseconds before an object become expired
     */
    public void setDuration(long duration) {
        mDuration = duration;
    }
}
