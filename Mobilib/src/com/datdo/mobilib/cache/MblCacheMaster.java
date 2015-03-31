package com.datdo.mobilib.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;

import com.datdo.mobilib.util.MblSerializer;
import com.datdo.mobilib.util.MblUtils;

public abstract class MblCacheMaster<T> {

    private static final String TAG = MblUtils.getTag(MblCacheMaster.class);

    protected abstract String   getObjectId(T object);
    protected abstract List<T>  fetchFromDatabase(List<String> ids);
    protected abstract void     fetchFromServer(List<String> ids, MblGetManyCallback<T> callback);
    protected abstract boolean  fallbackToDatabaseWhenServerFail();

    private long            mDuration;
    private MblIDConverter  mIdConverter;
    private MblMemCache<T>  mMemCache;
    private MblSerializer   mSerializer;

    public MblCacheMaster(Class<T> type, long duration) {
        mDuration       = duration;
        mIdConverter    = new MblIDConverter(type);
        mMemCache       = new MblMemCache<T>(duration);
        mSerializer     = new MblSerializer();
    }

    public void put(T object) {
        List<T> objects = new ArrayList<T>();
        objects.add(object);
        put(objects);
    }

    public void put(final List<T> objects) {

        if (MblUtils.isEmpty(objects)) {
            return;
        }

        mSerializer.run(new MblSerializer.Task() {
            @Override
            public void run(Runnable finishCallback) {

                long now = System.currentTimeMillis();
                List<MblDatabaseCache> dbCaches = new ArrayList<MblDatabaseCache>();
                for (T object : objects) {
                    String id = getObjectId(object);
                    mMemCache.put(id, object);
                    dbCaches.add(new MblDatabaseCache(mIdConverter.toComboId(id), now));
                }
                MblDatabaseCache.upsert(dbCaches);
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

                Log.d(TAG, "get: ids=" + TextUtils.join(",", ids));

                // firstly, load from memory
                final List<T> results = mMemCache.get(ids);

                if (results.size() == ids.size()) {
                    Log.d(TAG, "get: all ids are in memory-cache -> OK");
                    done(results, finishCallback);
                } else {

                    // secondly, load from database
                    List<String> idsNotInMemCache = new ArrayList<String>(ids);
                    for (T o : results) {
                        idsNotInMemCache.remove(getObjectId(o));
                    }

                    Log.d(TAG, "get: load from DB cache: ids=" + TextUtils.join(",", idsNotInMemCache));
                    List<MblDatabaseCache> dbCaches = MblDatabaseCache.get(
                            mIdConverter.toComboIds(idsNotInMemCache),
                            mDuration);
                    Map<String, MblDatabaseCache> mapIdAndDbCache = new HashMap<String, MblDatabaseCache>();
                    for (MblDatabaseCache c : dbCaches) {
                        mapIdAndDbCache.put(
                                mIdConverter.toOriginId(c.getKey()),
                                c);
                    }
                    Log.d(TAG, "get: fetch from DB: ids=" + TextUtils.join(",", mapIdAndDbCache.keySet()));
                    List<T> objectsInDatabase = fetchFromDatabase(new ArrayList<String>(mapIdAndDbCache.keySet()));
                    for (T o : objectsInDatabase) {
                        results.add(o);
                        mMemCache.put(
                                getObjectId(o),
                                o,
                                mapIdAndDbCache.get(getObjectId(o)).getDate());
                    }

                    if (results.size() == ids.size()) {
                        Log.d(TAG, "get: remaining ids are fetched from DB -> OK");
                        done(results, finishCallback);
                    } else {

                        // thirdly, load from server
                        final List<String> idsNotInMemCacheAndDbCache = new ArrayList<String>(ids);
                        for (T o : results) {
                            idsNotInMemCacheAndDbCache.remove(getObjectId(o));
                        }
                        Log.d(TAG, "get: fetch from server: ids=" + TextUtils.join(",", idsNotInMemCacheAndDbCache));
                        fetchFromServer(idsNotInMemCacheAndDbCache, new MblGetManyCallback<T>() {

                            @Override
                            public void onSuccess(List<T> objects) {
                                Log.d(TAG, "get: fetch from server: SUCCESS");
                                long now = System.currentTimeMillis();
                                List<MblDatabaseCache> dbCaches = new ArrayList<MblDatabaseCache>();
                                for (T o : objects) {
                                    mMemCache.put(getObjectId(o), o, now);
                                    dbCaches.add(new MblDatabaseCache(
                                            mIdConverter.toComboId(getObjectId(o)),
                                            now));
                                }
                                results.addAll(objects);
                                MblDatabaseCache.upsert(dbCaches);
                                if (results.size() == ids.size()) {
                                    Log.d(TAG, "get: remaining ids are fetched from server -> OK");
                                    done(results, finishCallback);
                                } else {
                                    Log.d(TAG, "get: some id is not fetched from server -> NG");
                                    done(null, finishCallback);
                                }
                            }

                            @Override
                            public void onError() {

                                Log.d(TAG, "get: fetch from server: ERROR");
                                
                                // failed to load from server -> fallback -> load from database
                                if (fallbackToDatabaseWhenServerFail()) {
                                    Log.d(TAG, "get: fallback to DB");
                                    List<T> objectsInDatabase = fetchFromDatabase(idsNotInMemCacheAndDbCache);
                                    results.addAll(objectsInDatabase);
                                    if (results.size() == ids.size()) {
                                        Log.d(TAG, "get: remaining ids are fetched from DB -> OK");
                                        done(results, finishCallback);
                                    } else {
                                        Log.d(TAG, "get: some id is not fetched from DB -> NG");
                                        done(null, finishCallback);
                                    }
                                } else {
                                    Log.d(TAG, "get: NG");
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
