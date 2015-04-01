package com.datdo.mobilib.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import com.datdo.mobilib.util.MblUtils;

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

    private final Map<String, CacheItem<T>> mMap = new HashMap<String, CacheItem<T>>();
    private long mDuration;

    public MblMemCache(long duration) {
        mDuration = duration;
    }

    public void put(String id, T object) {
        put(id, object, System.currentTimeMillis());
    }

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

    public boolean containsKey(String id) {
        synchronized (this) {
            return get(id) != null;
        }
    }

    public void clear() {
        synchronized (this) {
            mMap.clear();
        }
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }
}
