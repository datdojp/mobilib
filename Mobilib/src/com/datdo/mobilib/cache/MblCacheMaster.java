package com.datdo.mobilib.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datdo.mobilib.api.MblCache;
import com.datdo.mobilib.util.MblMemCache;
import com.datdo.mobilib.util.MblSerializer;
import com.datdo.mobilib.util.MblUtils;

public abstract class MblCacheMaster<T> {

    protected abstract String   getObjectId(T object);
    protected abstract List<T>  fetchFromDatabase(List<String> ids);
    protected abstract void     fetchFromServer(List<String> ids, MblGetManyCallback<T> callback);
    protected abstract boolean  fallbackToDatabaseWhenServerFail();

    private long            mDuration;
    private MblIDConverter  mIdConverter;
    private MemCache        mMemCache;
    private MblSerializer   mSerializer;

    private class MemCache extends MblMemCache<T> {
        public MemCache(Class<T> type, long duration) {
            super(type, duration);
        }
    }

    public MblCacheMaster(Class<T> type, long duration) {
        mDuration       = duration;
        mIdConverter    = new MblIDConverter(type);
        mMemCache       = new MemCache(type, duration);
        mSerializer     = new MblSerializer();
    }

    public void put(List<T> objects) {

        if (MblUtils.isEmpty(objects)) {
            return;
        }

        mSerializer.run(new MblSerializer.Task() {
            @Override
            public void run(Runnable finishCallback) {

            }
        });
    }

    public void get(String id, final MblGetOneCallback<T> callback) {
        List<String> ids = new ArrayList<String>();
        ids.add(id);
        get(ids, new MblGetManyCallback<T>() {

            @Override
            public void onSuccess(List<T> objects) {
                if (callback != null) {
                    if (!MblUtils.isEmpty(objects)) {
                        callback.onSuccess(objects.get(0));
                    } else {
                        callback.onError();
                    }
                }
            }

            @Override
            public void onError() {
                if (callback != null) {
                    callback.onError();
                }
            }
        });
    }

    public void get(String[] ids, final MblGetManyCallback<T> callback) {
        if (!MblUtils.isEmpty(ids)) {
            get(Arrays.asList(ids), callback);
        } else {
            get(new ArrayList<String>(), callback);
        }
    }

    public void get(final List<String> ids, final MblGetManyCallback<T> callback) {

        if (MblUtils.isEmpty(ids)) {
            if (callback != null) {
                MblUtils.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(new ArrayList<T>());
                    }
                });
            }
            return;
        }

        mSerializer.run(new MblSerializer.Task() {
            @Override
            public void run(final Runnable finishCallback) {

                // firstly, load from memory
                final List<T> results = mMemCache.get(ids);

                if (results.size() == ids.size()) {
                    done(results, finishCallback);
                } else {

                    // secondly, load from database
                    List<String> idsNotInMemCache = new ArrayList<String>(ids);
                    for (T o : results) {
                        idsNotInMemCache.remove(getObjectId(o));
                    }

                    List<MblCache> dbCaches = MblCache.get(
                            mIdConverter.toComboIds(idsNotInMemCache),
                            mDuration);
                    Map<String, MblCache> mapIdAndDbCache = new HashMap<String, MblCache>();
                    for (MblCache c : dbCaches) {
                        mapIdAndDbCache.put(
                                mIdConverter.toOriginId(c.getKey()),
                                c);
                    }
                    List<T> objectsInDatabase = fetchFromDatabase(new ArrayList<String>(mapIdAndDbCache.keySet()));
                    for (T o : objectsInDatabase) {
                        results.add(o);
                        mMemCache.put(
                                getObjectId(o),
                                o,
                                mapIdAndDbCache.get(getObjectId(o)).getDate());
                    }

                    if (results.size() == ids.size()) {
                        done(results, finishCallback);
                    } else {

                        // thirdly, load from server
                        final List<String> idsNotInMemCacheAndDbCache = new ArrayList<String>(ids);
                        for (T o : results) {
                            idsNotInMemCacheAndDbCache.remove(getObjectId(o));
                        }
                        fetchFromServer(idsNotInMemCacheAndDbCache, new MblGetManyCallback<T>() {

                            @Override
                            public void onSuccess(List<T> objects) {
                                long now = System.currentTimeMillis();
                                List<MblCache> dbCaches = new ArrayList<MblCache>();
                                for (T o : objects) {
                                    mMemCache.put(getObjectId(o), o, now);
                                    dbCaches.add(new MblCache(
                                            mIdConverter.toComboId(getObjectId(o)),
                                            now));
                                }
                                results.addAll(objects);
                                MblCache.upsert(dbCaches);
                                if (results.size() == ids.size()) {
                                    done(results, finishCallback);
                                } else {
                                    done(null, finishCallback);
                                }
                            }

                            @Override
                            public void onError() {

                                // failed to load from server -> fallback -> load from database
                                if (fallbackToDatabaseWhenServerFail()) {
                                    List<T> objectsInDatabase = fetchFromDatabase(idsNotInMemCacheAndDbCache);
                                    results.addAll(objectsInDatabase);
                                    if (results.size() == ids.size()) {
                                        done(results, finishCallback);
                                    } else {
                                        done(null, finishCallback);
                                    }
                                } else {
                                    done(null, finishCallback);
                                }
                            }
                        });
                    }
                }
            }

            void done(final List<T> objects, final Runnable finishCallback) {
                MblUtils.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            if (!MblUtils.isEmpty(objects)) {
                                callback.onSuccess(objects);
                            } else {
                                callback.onError();
                            }
                        }
                        finishCallback.run();
                    }
                });
            }
        });
    }

    public static interface MblGetOneCallback<T> {
        public void onSuccess(T object);
        public void onError();
    }

    public static interface MblGetManyCallback<T> {
        public void onSuccess(List<T> objects);
        public void onError();
    }
}
