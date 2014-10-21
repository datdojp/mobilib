package com.datdo.mobilib.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

class MblWeakArrayList<T> {
    private List<WeakReference<T>> mData = new Vector<WeakReference<T>>();

    public MblWeakArrayList() {}

    public MblWeakArrayList(MblWeakArrayList<T> other) {
        mData.clear();
        mData.addAll(other.mData);
    }

    public void add(T item) {
        if (item == null) return;

        synchronized (mData) {
            if (!__contains(item)) {
                mData.add(new WeakReference<T>(item));
            }
        }
    }

    public void remove(T item) {
        if (item == null) return;

        synchronized (mData) {
            int index = __indexOf(item);
            if (index >= 0) {
                mData.remove(index);
            }
        }
    }

    public boolean contains(T item) {
        synchronized (mData) {
            return __contains(item);
        }
    }

    public boolean isEmpty() {
        synchronized (mData) {
            __flush();
            return mData.isEmpty();
        }
    }

    public void iterateWithCallback(MblWeakArrayListCallback<T> cb) {
        if (cb == null) return;

        synchronized (mData) {
            __flush();
            for (WeakReference<T> ref : mData) {
                T item = ref.get();
                if (item != null) cb.onInterate(item);
            }
        }
    }

    @Deprecated
    public Iterator<T> iterate() {
        return null; // not yet used
    }

    public static interface MblWeakArrayListCallback<T> {
        public void onInterate(T item);
    }

    private boolean __contains(T item) {
        return __indexOf(item) >= 0;
    }

    private int __indexOf(T item) {
        if (item == null) return -1;

        int i = 0;
        for (WeakReference<T> anItem : mData) {
            if (anItem.get() == item) {
                return i;
            }
            i++;
        }

        return -1;
    }

    private void __flush() {
        List<WeakReference<T>> needToRemove = new ArrayList<WeakReference<T>>();
        for (WeakReference<T> anItem : mData) {
            if (anItem.get() == null) {
                needToRemove.add(anItem);
            }
        }
        mData.removeAll(needToRemove);
    }
}