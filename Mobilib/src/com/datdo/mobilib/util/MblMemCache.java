package com.datdo.mobilib.util;

import java.util.ArrayList;
import java.util.HashMap;
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
        return mTypeName + "#" + id;
    }

    private String mTypeName;
    private long mDuration;

    public MblMemCache(Class<T> type, long duration) {
        Assert.assertNotNull(type);
        mTypeName = type.getName();
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
                if (mDuration <= 0 || cacheItem.mPutAt + mDuration > System.currentTimeMillis()) {
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
            Set<String> cacheIds = sMap.keySet();
            String prefix = mTypeName + "#";
            for (String cid : cacheIds) {
                if (cid.startsWith(prefix)) {
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
            for (String mapId : sMap.keySet()) {
                if (mapId.startsWith(mTypeName)) {
                    removedIds.add(mapId);
                }
            }
            for (String id : removedIds) {
                sMap.remove(id);
            }
        }
    }
}
