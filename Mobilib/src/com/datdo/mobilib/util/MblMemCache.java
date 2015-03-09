package com.datdo.mobilib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

/**
 * <pre>
 * TBD
 * </pre>
 *
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public class MblMemCache<T> {

    private static class CacheItem<V> {

        V mObject;
        long mPutAt;

        CacheItem(V object, long putAt) {
            mObject = object;
            mPutAt = putAt;
        }
    }

    public static interface IterateCallback<T> {
        public void onInterate(T object);
    }

    private static final Map<String, CacheItem> sMap = new HashMap<String, CacheItem>();

    private String getCacheId(String id) {
        if (isCacheIdOfThisClass(id)) {
            return id;
        } else {
            return mCacheIdPrefix + id;
        }
    }

    private boolean isCacheIdOfThisClass(String id) {
        return id.startsWith(mCacheIdPrefix);
    }

    private String mTypeName;
    private String mCacheIdPrefix;
    private long mDuration;

    public MblMemCache(Class<T> type, long duration) {
        Assert.assertNotNull(type);
        mTypeName = type.getName();
        mCacheIdPrefix = mTypeName + "#";
        mDuration = duration;
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param id
     * @param object
     */
    @SuppressWarnings("unchecked")
    public void put(String id, T object) {
        synchronized (MblMemCache.class) {
            String cachId = getCacheId(id);
            CacheItem<T> cacheItem = sMap.get(cachId);
            if (cacheItem == null) {
                cacheItem = new CacheItem<T>(object, System.currentTimeMillis());
            } else {
                cacheItem.mObject = object;
                cacheItem.mPutAt = System.currentTimeMillis();
            }
            sMap.put(cachId, cacheItem);
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
        synchronized (MblMemCache.class) {
            String cachId = getCacheId(id);
            CacheItem<T> cacheItem = sMap.get(cachId);
            if (cacheItem == null) {
                return null;
            } else {
                if (mDuration <= 0 || System.currentTimeMillis() - cacheItem.mPutAt < mDuration) {
                    return cacheItem.mObject;
                } else {
                    sMap.remove(cachId);
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

        synchronized (MblMemCache.class) {
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
        synchronized (MblMemCache.class) {
            String cachId = getCacheId(id);
            CacheItem<T> cacheItem = sMap.get(cachId);
            if (cacheItem == null) {
                return null;
            } else {
                sMap.remove(cachId);
                return cacheItem.mObject;
            }
        }
    }

    /**
     * <pre>TBD</pre>
     * @param cb
     */
    public void iterateWithCallback(IterateCallback<T> cb) {

        if (cb == null) {
            return;
        }

        synchronized (MblMemCache.class) {
            Set<String> cacheIds = new HashSet<String>(sMap.keySet());
            for (String cid : cacheIds) {
                if (isCacheIdOfThisClass(cid)) {
                    T o = get(cid);
                    if (o != null) {
                        cb.onInterate(o);
                    }
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
        synchronized (MblMemCache.class) {
            return get(id) != null;
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     */
    public void clear() {
        synchronized (MblMemCache.class) {
            List<String> removedIds = new ArrayList<String>();
            Set<String> cacheIds = new HashSet<String>(sMap.keySet());
            for (String cid : cacheIds) {
                if (isCacheIdOfThisClass(cid)) {
                    removedIds.add(cid);
                }
            }
            for (String id : removedIds) {
                sMap.remove(id);
            }
        }
    }
}
