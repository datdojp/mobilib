package com.datdo.mobilib.cache;

import android.util.Pair;

import com.datdo.mobilib.util.MblUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * An extension of {@link MblCacheMaster}. All requests via get() methods are accumulated in a list and are executed at once every 100 milliseconds.
 * </pre>
 */
public abstract class MblPendingCacheMaster<T> extends MblCacheMaster<T> {

    /**
     * Contructor.
     * @param type     class of objects
     * @param duration time in milliseconds before an object become expired
     */
    public MblPendingCacheMaster(Class<T> type, long duration) {
        super(type, duration);
        MblUtils.repeatDelayed(new Runnable() {
            @Override
            public void run() {
                runAll();
            }
        }, 100);
    }


    private List<Pair<List<String>, MblGetManyCallback>> mPendingRequests = Collections.synchronizedList(new ArrayList<Pair<List<String>, MblGetManyCallback>>());

    @Override
    public Runnable get(final List<String> ids, MblGetManyCallback<T> callback) {
        if (MblUtils.isEmpty(ids)) {
            return super.get(ids, callback);
        }

        mPendingRequests.add(new Pair<List<String>, MblGetManyCallback>(ids, callback));
        return new Runnable() {
            @Override
            public void run() {
                mPendingRequests.remove(ids);
            }
        };
    }

    private void runAll() {
        if (mPendingRequests.isEmpty()) {
            return;
        }
        final Set<String> allIds = new HashSet<>();
        final List<Pair<List<String>, MblGetManyCallback>> pendingRequests = new ArrayList<>(mPendingRequests);
        mPendingRequests.clear();
        for (Pair<List<String>, MblGetManyCallback> p : pendingRequests) {
            allIds.addAll(p.first);
        }
        super.get(new ArrayList<>(allIds), new MblGetManyCallback<T>() {
            @Override
            public void onSuccess(List<T> objects) {
                Map<String, T> idToObject = new HashMap<>();
                for (T o : objects) {
                    idToObject.put(getObjectId(o), o);
                }

                for (Pair<List<String>, MblGetManyCallback> p : pendingRequests) {
                    List<T> results = new ArrayList<T>();
                    MblGetManyCallback callback = p.second;
                    for (String id : p.first) {
                        T o = idToObject.get(id);
                        if (o != null) {
                            results.add(o);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(results);
                    }
                }
            }

            @Override
            public void onError() {
                for (Pair<List<String>, MblGetManyCallback> p : pendingRequests) {
                    MblGetManyCallback callback = p.second;
                    if (callback != null) {
                        callback.onError();
                    }
                }
            }
        });
    }
}
