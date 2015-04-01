package com.datdo.mobilib.util;

import java.util.ArrayList;
import java.util.List;

public class MblSerializer {

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
        // post to main thread to prevent StackOverFlow
        MblUtils.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                synchronized (MblSerializer.this) {
                    if (!mIsRunning && !mTasks.isEmpty()) {
                        mIsRunning = true;
                        mTasks.remove(0).run(mFinishCallback);
                    }
                }
            }
        });
    }

    public void run(Task task) {
        synchronized (this) {
            mTasks.add(task);
            runNextTask();
        }
    }

    public boolean cancel(Task task) {
        synchronized (this) {
            return mTasks.remove(task);
        }
    }

    public void cancelAll() {
        synchronized (this) {
            mTasks.clear();
        }
    }
}
