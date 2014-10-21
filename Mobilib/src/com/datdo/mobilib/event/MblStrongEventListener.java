package com.datdo.mobilib.event;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * <pre>
 * A special abstract class of {@link MblEventListener}. This class is frequently used to create an anonymous listener.
 * {@link MblEventCenter} only keeps a {@link WeakReference} to listeners to prevent memory leak.
 * Therefore, if anonymous listener does not extend this class, it will be destroyed by GC.
 * Remember to unregister when listener is no longer needed by calling {@link #terminate()}
 * 
 * Here is an sample usage of this class:
 * <code>
 * MblEventCenter.addListener(new MblStrongEventListener() {
 *     {@literal @}Override
 *     public void onEvent(Object sender, String name, Object... args) {
 *         // handle the event
 *         // ...
 *         
 *         if (listenerIsNoLongerNeeded) {
 *             terminate();
 *         }
 *     }
 * }, "event_name");
 * </code>
 * </pre> 
 */
public abstract class MblStrongEventListener implements MblEventListener {

    private static final Set<MblStrongEventListener> sAnonymousObservers = new HashSet<MblStrongEventListener>();

    public MblStrongEventListener() {
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.add(this);
        }
    }

    /**
     * <pre>
     * Remove this listener from {@link MblEventCenter} and make it ready to be destroyed by GC.
     * </pre>
     */
    public void terminate() {
        MblEventCenter.removeListenerFromAllEvents(this);
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.remove(this);
        }
    }
}
