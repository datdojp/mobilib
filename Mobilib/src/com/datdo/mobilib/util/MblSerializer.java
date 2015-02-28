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

    private static final List<Task> sTasks              = new ArrayList<Task>();
    private static boolean          sIsRunning          = false;
    private static final Runnable   sFinishCallback     = new Runnable() {
        @Override
        public void run() {
            sIsRunning = false;
            runNextTask();
        }
    };

    private static void runNextTask() {
        synchronized (MblSerializer.class) {
            if (!sIsRunning && !sTasks.isEmpty()) {
                sIsRunning = true;
                sTasks.remove(0).run(sFinishCallback);
            }
        }
    }

    /**
     * <pre>
     * TBD
     * </pre>
     * @param task
     */
    public static void run(Task task) {
        synchronized (MblSerializer.class) {
            sTasks.add(task);
            runNextTask();
        }
    }
}
