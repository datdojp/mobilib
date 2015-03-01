package com.datdo.mobilib.util;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * TBD
 * </pre>
 */
public class MblSerializer {

    /**
     * <pre>
     * TBD
     * </pre>
     */
    public static interface Task {
        public void run(Runnable finishCallback);
    }

    private final List<Task>    mTasks              = new ArrayList<Task>();
    private boolean             mIsRunning          = false;
    private final Runnable      mFinishCallback     = new Runnable() {
        @Override
        public void run() {
            mIsRunning = false;
            runNextTask();
        }
    };

    private void runNextTask() {
        synchronized (this) {
            if (!mIsRunning && !mTasks.isEmpty()) {
                mIsRunning = true;
                // post to main thread to prevent StackOverFlow
                MblUtils.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mTasks.remove(0).run(mFinishCallback);
                    }
                });
            }
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param task
     */
    public void run(Task task) {
        synchronized (this) {
            mTasks.add(task);
            runNextTask();
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param task
     * @return
     */
    public boolean cancel(Task task) {
        synchronized (this) {
            return mTasks.remove(task);
        }
    }

    /**
     * <pre>TBD</pre>
     */
    public void cancelAll() {
        synchronized (this) {
            mTasks.clear();
        }
    }
}
