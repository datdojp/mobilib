package com.datdo.mobilib.event;

import com.datdo.mobilib.util.MblUtils;

/**
 * <pre>
 * Listener of {@link MblEventCenter}
 * </pre>
 */
public interface MblEventListener {
    /**
     * <pre>
     * Invoked when a registered event is posted.
     * This method is always executed on main thread by {@link MblEventCenter}.
     * Use {@link MblUtils#getArgAt(int, Object...)} to get argument object from "args" safely.
     * </pre>
     * @param sender object that posts the event
     * @param name event name
     * @param args data attached to event
     */
    public void onEvent(Object sender, String name, Object... args);
}
