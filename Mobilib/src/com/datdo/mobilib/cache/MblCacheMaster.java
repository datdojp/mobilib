package com.datdo.mobilib.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import android.text.TextUtils;
import android.util.Log;

import com.datdo.mobilib.util.MblSerializer;
import com.datdo.mobilib.util.MblUtils;

/**
 * <pre>
 * Cache Master (CM) is a full solution for retrieving and caching objects in Android app.
 * Objects can be retrieved from 3 data-sources:
 *  1. Memory                           -> high speed, can get object instantly without blocking main thread, lost all objects when app is killed
 *  2. Database/file                    -> medium speed, should be executed on asynchronous thread, objects is still intact even when app is killed
 *  3. Server (via RESTful API, etc)    -> slow speed, use this way to retrieve new objects or expired objects, must be executed on asynchronous thread
 *
 * The mechanism of CM is that objects are stored in Memory for instant retrieval, also stored in Database to keep them intact even when app is killed,
 *  and fetched from server when they are expired or not existing in both Memory and Database.
 *
 * The following example steps depicts how CM works to retrieve list of objects by their ids:
 *  1. Given that we need to retrieve objects whose ids is [1,2,3,4]
 *  2. Firstly, CM searches in Memory for [1,2,3,4]. It finds object1 and object2 but object2 is too old (expired). 3 & 4 is not found. Result = [object1]
 *  3. Next, CM searches in Mobilib 's database for [2,3,4]. It finds out that 2 is existing but expired, 3 is existing and still fresh, 4 is not found.
 *      Then it searches in App 's database for 3 and retrieves object3. Result = [object1, object3]
 *  4. Finally, CM calls RESTful API to fetch object2 and object4 from server. Result = [object1, object2, object3, object4]
 * Of course this is just a happy case. At step 4, we may not be able to fetch objects from server, and have to fallback to fetch them from App 's database even though they are expired.
 *
 * Note that Mobilib 's database and App 's database is different. Mobilib 's database is to determine whether object of certain id is existing or expired. App 's database is where App stores objects.
 *
 * For Memory Cache, CM utilizes {@link com.datdo.mobilib.cache.MblMemCache} which is just a simple id:object mapping with expiration.
 * For Database Cache, CM utilizes {@link com.datdo.mobilib.cache.MblDatabaseCache} which is just a simple id:timeInMs mapping to determine whether object of id is expired.
 * Fetching objects from App 's database and server is done by App by overriding 2 method {@link #fetchFromDatabase(java.util.List)} and {@link #fetchFromServer(java.util.List, com.datdo.mobilib.cache.MblCacheMaster.MblGetManyCallback)}
 * get/put/delete/clear methods are executed serially by {@link com.datdo.mobilib.util.MblSerializer} to make CM thread-safe, and also to ensure that we don't send 2 server requests for the same object.
 * All accesses to Databases are executed on asynchronous thread so that it doesn't burden main thread.
 *
 * Sample code:
 * {@code
 *      MblCacheMaster cm = new MblCacheMaster<User>(User.class, 60 * 1000) {
 *
 *          @Override
 *          protected String getObjectId(User user) {
 *              return user.getId();
 *          }
 *
 *          @Override
 *          protected List<T> fetchFromDatabase(List<String> ids) {
 *              return UserDatabase.fetchUsers(ids);
 *          }
 *
 *          @Override
 *          protected void storeToDatabase(List<User> users) {
 *              UserDatabase.saveUsers(users);
 *          }
 *
 *          @Override
 *          protected void fetchFromServer(List<String> ids, final MblGetManyCallback<T> callback) {
 *              UserRestApi.fetchUsers(ids, new FetchUsersCallback() {
 *                  @Override
 *                  public void onSuccess(List<User> users) {
 *                      callback.onSuccess(users);
 *                  }
 *
 *                  @Override
 *                  public void onError() {
 *                      callback.onError();
 *                  }
 *              });
 *          }
 *
 *          @Override
 *          protected boolean  fallbackToDatabaseWhenServerFail() {
 *              return true;
 *          }
 *      }
 *
 *      cm.get(new String[] { 1, 2, 3, 4 }, new MblGetManyCallback<User>() {
 *          @Override
 *          public void onSuccess(List<User> users) {
 *              // ... display users
 *          }
 *
 *          @Override
 *          public void onError() {
 *              // ... show error message
 *          }
 *      });
 * }
 *
 * </pre>
 * @param <T> class of object being cached
 * @see com.datdo.mobilib.cache.MblMemCache
 * @see com.datdo.mobilib.cache.MblDatabaseCache
 * @see com.datdo.mobilib.util.MblSerializer
 */
public abstract class MblCacheMaster<T> {

    private static final String TAG = MblUtils.getTag(MblCacheMaster.class);

    /**
     * Determine how to get id of an object.
     */
    protected abstract String   getObjectId(T object);

    /**
     * <pre>
     * Determine how to fetch objects from App 's database.
     * Note that objects must not be stored in database, they can be stored in files. Of course database is recommended to store structured objects.
     * </pre>
     * @param ids ids of objects
     * @return list of objects (size may be less than ids 's size)
     */
    protected abstract List<T>  fetchFromDatabase(List<String> ids);

    /**
     * <pre>
     * Determine how to store objects to App 's database. Normally, this method is where you store objects fetched from server via {@link #fetchFromServer(java.util.List, com.datdo.mobilib.cache.MblCacheMaster.MblGetManyCallback)}
     * Note that objects must not be stored in database, they can be stored in files. Of course database is recommended to store structured objects.
     * </pre>
     * @param objects objects fetched from server
     */
    protected abstract void     storeToDatabase(List<T> objects);

    /**
     * Determine how to fetch objects from server (via RESTful API, etc)
     * @param ids ids of objects
     * @param callback callback to received result objects or error
     */
    protected abstract void     fetchFromServer(List<String> ids, MblGetManyCallback<T> callback);

    /**
     * Determine if Cache Master should fallback to fetch expired objects from App 's database when we lost connection to server.
     * @return true if should fallback
     */
    protected abstract boolean  fallbackToDatabaseWhenServerFail();

    private long            mDuration;
    private MblIDConverter  mIdConverter;
    private MblMemCache<T>  mMemCache;
    private MblSerializer   mSerializer;

    /**
     * Contructor.
     * @param type class of objects
     * @param duration time in milliseconds before an object become expired
     */
    public MblCacheMaster(Class<T> type, long duration) {
        mDuration       = duration;
        mIdConverter    = new MblIDConverter(type);
        mMemCache       = new MblMemCache<T>(duration);
        mSerializer     = new MblSerializer();
    }

    /**
     * Put an object to cache (for example, when we receive object from server)
     */
    public void put(T object) {
        List<T> objects = new ArrayList<T>();
        objects.add(object);
        put(objects);
    }

    /**
     * Put many objects to cache (for example, when we receive objects from server)
     */
    public void put(final List<T> objects) {

        if (MblUtils.isEmpty(objects)) {
            return;
        }

        mSerializer.run(new MblSerializer.Task() {
            @Override
            public void run(final Runnable finishCallback) {

                MblUtils.executeOnAsyncThread(new Runnable() {
                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        List<MblDatabaseCache> dbCaches = new ArrayList<MblDatabaseCache>();
                        for (T object : objects) {
                            String id = getObjectId(object);
                            mMemCache.put(id, object);
                            dbCaches.add(new MblDatabaseCache(mIdConverter.toComboId(id), now));
                        }
                        MblDatabaseCache.upsert(dbCaches);
                        finishCallback.run();
                    }
                });
            }
        });
    }

    /**
     * Retrieve object by its id.
     * @param id
     * @param callback callback to received result object or error
     * @return Runnable object to cancel the request if is a pending one.
     */
    public Runnable get(String id, final MblGetOneCallback<T> callback) {
        List<String> ids = new ArrayList<String>();
        ids.add(id);
        return get(ids, new MblGetManyCallback<T>() {

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

    /**
     * Retrieve objects by their ids.
     * @param ids array of ids
     * @param callback callback to received result objects or error
     * @return Runnable object to cancel the request if is a pending one.
     */
    public Runnable get(String[] ids, final MblGetManyCallback<T> callback) {
        if (!MblUtils.isEmpty(ids)) {
            return get(Arrays.asList(ids), callback);
        } else {
            return get(new ArrayList<String>(), callback);
        }
    }

    /**
     * Retrieve objects by their ids.
     * @param ids list of ids
     * @param callback callback to received result objects or error
     * @return Runnable object to cancel the request if is a pending one.
     */
    public Runnable get(final List<String> ids, final MblGetManyCallback<T> callback) {

        if (MblUtils.isEmpty(ids)) {
            Log.d(TAG, "get: ids is empty");
            if (callback != null) {
                MblUtils.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(new ArrayList<T>());
                    }
                });
            }
            return new Runnable() {
                @Override
                public void run() {}
            };
        }

        final MblSerializer.Task task = new MblSerializer.Task() {
            @Override
            public void run(final Runnable finishCallback) {

                Log.d(TAG, "get: ids=" + TextUtils.join(",", ids));

                // firstly, load from memory
                final List<T> results = mMemCache.get(ids);

                if (results.size() == ids.size()) {
                    Log.d(TAG, "get: all ids are in memory-cache -> OK");
                    done(results, finishCallback);
                } else {
                    MblUtils.executeOnAsyncThread(new Runnable() {
                        @Override
                        public void run() {
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
                            if (!MblUtils.isEmpty(objectsInDatabase)) {
                                for (T o : objectsInDatabase) {
                                    results.add(o);
                                    mMemCache.put(
                                            getObjectId(o),
                                            o,
                                            mapIdAndDbCache.get(getObjectId(o)).getDate());
                                }
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
                                    public void onSuccess(final List<T> objects) {
                                        MblUtils.executeOnAsyncThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d(TAG, "get: fetch from server: SUCCESS");
                                                if (!MblUtils.isEmpty(objects)) {
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
                                                    storeToDatabase(objects);
                                                }
                                                if (results.size() == ids.size()) {
                                                    Log.d(TAG, "get: remaining ids are fetched from server -> OK");
                                                    done(results, finishCallback);
                                                } else {
                                                    Log.d(TAG, "get: some id is not fetched from server -> NG");
                                                    done(null, finishCallback);
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError() {

                                        Log.d(TAG, "get: fetch from server: ERROR");

                                        // failed to load from server -> fallback -> load from database
                                        if (fallbackToDatabaseWhenServerFail()) {
                                            MblUtils.executeOnAsyncThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.d(TAG, "get: fallback to DB");
                                                    List<T> objectsInDatabase = fetchFromDatabase(idsNotInMemCacheAndDbCache);
                                                    if (!MblUtils.isEmpty(objectsInDatabase)) {
                                                        results.addAll(objectsInDatabase);
                                                    }
                                                    if (results.size() == ids.size()) {
                                                        Log.d(TAG, "get: remaining ids are fetched from DB -> OK");
                                                        done(results, finishCallback);
                                                    } else {
                                                        Log.d(TAG, "get: some id is not fetched from DB -> NG");
                                                        done(null, finishCallback);
                                                    }
                                                }
                                            });
                                        } else {
                                            Log.d(TAG, "get: NG");
                                            done(null, finishCallback);
                                        }
                                    }
                                });
                            }
                        }
                    });
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
        };

        mSerializer.run(task);

        return new Runnable() {
            @Override
            public void run() {
                mSerializer.cancel(task);
            }
        };
    }

    /**
     * Interface for callback to receive one object (or error)
     */
    public static interface MblGetOneCallback<T> {
        public void onSuccess(T object);
        public void onError();
    }

    /**
     * Interface for callback to receive many objects (or error)
     */
    public static interface MblGetManyCallback<T> {
        public void onSuccess(List<T> objects);
        public void onError();
    }

    /**
     * Get {@link com.datdo.mobilib.cache.MblMemCache} instance used in this Cache Master
     */
    public MblMemCache<T> getMemCache() {
        return mMemCache;
    }

    /**
     * Set {@link com.datdo.mobilib.cache.MblMemCache} instance used in this Cache Master
     */
    public void setMemCache(MblMemCache<T> memCache) {
        Assert.assertTrue(memCache.getDuration() == mMemCache.getDuration());
        mMemCache = memCache;
    }

    /**
     * <pre>
     * Remove all objects from both Memory Cache and Database Cache.
     * Note that objects stored in App 's database are stilled intact.
     * </pre>
     */
    public void clear() {
        mSerializer.run(new MblSerializer.Task() {
            @Override
            public void run(final Runnable finishCallback) {

                MblUtils.executeOnAsyncThread(new Runnable() {
                    @Override
                    public void run() {
                        mMemCache.clear();
                        MblDatabaseCache.deleteByPrefix(mIdConverter.getPrefix());
                        finishCallback.run();
                    }
                });
            }
        });
    }

    /**
     * <pre>
     * Remove an objects from both Memory Cache and Database Cache by its id.
     * Note that object stored in App 's database is stilled intact.
     * </pre>
     */
    public void delete(final String id) {
        mSerializer.run(new MblSerializer.Task() {
            @Override
            public void run(final Runnable finishCallback) {
                MblUtils.executeOnAsyncThread(new Runnable() {
                    @Override
                    public void run() {
                        mMemCache.remove(id);
                        MblDatabaseCache.deleteByKey(mIdConverter.toComboId(id));
                        finishCallback.run();
                    }
                });
            }
        });
    }

    /**
     * <pre>
     * Remove many objects from both Memory Cache and Database Cache by their ids.
     * Note that objects stored in App 's database are stilled intact.
     * </pre>
     */
    public void delete(final List<String> ids) {
        mSerializer.run(new MblSerializer.Task() {
            @Override
            public void run(final Runnable finishCallback) {
                MblUtils.executeOnAsyncThread(new Runnable() {
                    @Override
                    public void run() {
                        mMemCache.remove(ids);
                        MblDatabaseCache.deleteByKeys(mIdConverter.toComboIds(ids));
                        finishCallback.run();
                    }
                });
            }
        });
    }
}
