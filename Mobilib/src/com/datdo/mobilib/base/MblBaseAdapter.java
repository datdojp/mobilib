package com.datdo.mobilib.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.datdo.mobilib.util.MblUtils;

import junit.framework.Assert;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ListView;

/**
 * <pre>
 * An extension of {@link BaseAdapter}.
 * Make all data changes and calls of {@link #notifyDataSetChanged()} synchronized on main thread to prevent concurrency problems.
 * </pre>
 * @param <T> class of object bound with an item of {@link ListView}
 */
public abstract class MblBaseAdapter<T> extends BaseAdapter {

    @SuppressWarnings("serial")
    private final List<T> mData = new ArrayList<T>() {
        @Override
        public void clear() {
            Assert.assertTrue(MblUtils.isMainThread());
            super.clear();
        }

        @Override
        public void add(int index, T object) {
            Assert.assertTrue(MblUtils.isMainThread());
            super.add(index, object);
        }

        @Override
        public boolean add(T object) {
            Assert.assertTrue(MblUtils.isMainThread());
            return super.add(object);
        }

        @Override
        public boolean addAll(Collection<? extends T> collection) {
            Assert.assertTrue(MblUtils.isMainThread());
            return super.addAll(collection);
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> collection) {
            Assert.assertTrue(MblUtils.isMainThread());
            return super.addAll(index, collection);
        }

        @Override
        public T remove(int index) {
            Assert.assertTrue(MblUtils.isMainThread());
            return super.remove(index);
        }

        @Override
        public boolean remove(Object object) {
            Assert.assertTrue(MblUtils.isMainThread());
            return super.remove(object);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            Assert.assertTrue(MblUtils.isMainThread());
            return super.removeAll(collection);
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            Assert.assertTrue(MblUtils.isMainThread());
            super.removeRange(fromIndex, toIndex);
        }
    };

    private LayoutInflater mLayoutInflater;

    /**
     * <pre>
     * Get LayoutInflater instance which is mandatory for most Adapter.
     * </pre>
     */
    protected LayoutInflater getLayoutInflater() {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(MblUtils.getCurrentContext());
        }
        return mLayoutInflater;
    }

    /**
     * <pre>
     * Subclasses use this method to get internal data objects.
     * </pre>
     * @return list of internal data objects.
     */
    protected List<T> getData() {
        return mData;
    }

    @Override
    public void notifyDataSetChanged() {
        MblUtils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                MblBaseAdapter.super.notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int pos) {
        if (pos < mData.size()) {
            return mData.get(pos);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    /**
     * <pre>
     * Subclasses is strongly recommended to use this method when it wants to change internal data.
     * 
     * Here is an example:
     * <code>
     * public void appendData(Object newItem) {
     *     changeDataSafely(new Runnable() {
     *         {@literal @}Override
     *         public void run() {
     *            getData().add(newItem);
     *            notifyDataSetChanged();
     *         }
     *     });
     * }
     * </code>
     * </pre> 
     * @param action wraps all changes on internal data
     */
    protected void changeDataSafely(final Runnable action) {
        MblUtils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                synchronized (mData) {
                    action.run();
                }
            }
        });
    }

    /**
     * <pre>
     * Discard all current data and replace them by new ones.
     * Also refresh UI automatically.
     * </pre>
     * @param data new data
     */
    public void changeData(final List<T> data) {
        changeDataSafely(new Runnable() {
            @Override
            public void run() {
                getData().clear();
                if (data != null) {
                    getData().addAll(data);
                }
                notifyDataSetChanged();
            }
        });
    }
    
    /**
     * <pre>
     * Discard all current data and replace them by new ones.
     * Also refresh UI automatically.
     * </pre>
     * @param data new data
     */
    public void changeData(final T[] data) {
        changeDataSafely(new Runnable() {
            @Override
            public void run() {
                getData().clear();
                if (data != null) {
                    for (T d : data) {
                        getData().add(d);
                    }
                }
                notifyDataSetChanged();
            }
        });
    }
}
