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
 * <pre>
 * TBD
 * </pre>
 *
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public class MblMemCache<T> {

    private static class CacheItem<T> {

        T       mObject;
        long    mPutAt;

        CacheItem(T object, long putAt) {
            mObject = object;
            mPutAt = putAt;
        }
    }

    public static interface IterateCallback<T> {
        public void onInterate(T object);
    }

    private Map<String, CacheItem>  mMap;
    private long                    mDuration;
    private MblIDConverter          mIdConverter;

    public MblMemCache(Class<T> type, long duration) {
        Assert.assertNotNull(type);
        mMap            = new HashMap<String, CacheItem>();
        mDuration       = duration;
        mIdConverter    = new MblIDConverter(type);
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param id
     * @param object
     */
    public void put(String id, T object) {
        put(id, object, System.currentTimeMillis());
    }

    /**
     * TBD
     * @param id
     * @param object
     * @param putAt
     */
    @SuppressWarnings("unchecked")
    public void put(String id, T object, long putAt) {
        synchronized (this) {
            String cacheId = mIdConverter.toComboId(id);
            CacheItem<T> cacheItem = mMap.get(cacheId);
            if (cacheItem == null) {
                cacheItem = new CacheItem<T>(object, putAt);
            } else {
                cacheItem.mObject   = object;
                cacheItem.mPutAt    = putAt;
            }
            mMap.put(cacheId, cacheItem);
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public T get(String id) {
        synchronized (this) {
            String cacheId = mIdConverter.toComboId(id);
            CacheItem<T> cacheItem = mMap.get(cacheId);
            if (cacheItem == null) {
                return null;
            } else {
                if (mDuration <= 0 || System.currentTimeMillis() - cacheItem.mPutAt < mDuration) {
                    return cacheItem.mObject;
                } else {
                    mMap.remove(cacheId);
                    return null;
                }
            }
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param ids
     * @return
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
     * <pre>TBD</pre>
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public T remove(String id) {
        synchronized (this) {
            String cacheId = mIdConverter.toComboId(id);
            CacheItem<T> cacheItem = mMap.get(cacheId);
            if (cacheItem == null) {
                return null;
            } else {
                mMap.remove(cacheId);
                return cacheItem.mObject;
            }
        }
    }

    /**
     * <pre>TBD</pre>
     * @param callback
     */
    public void iterateWithCallback(IterateCallback<T> callback) {

        Assert.assertNotNull(callback);

        synchronized (this) {
            Set<String> cacheIds = new HashSet<String>(mMap.keySet());
            for (String cid : cacheIds) {
                T o = get(cid);
                if (o != null) {
                    callback.onInterate(o);
                }
            }
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param id
     * @return
     */
    public boolean containsKey(String id) {
        synchronized (this) {
            return get(id) != null;
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     */
    public void clear() {
        synchronized (this) {
            mMap.clear();
        }
    }
}
