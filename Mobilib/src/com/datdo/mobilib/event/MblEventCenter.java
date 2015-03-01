package com.datdo.mobilib.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.datdo.mobilib.event.MblWeakArrayList.MblWeakArrayListCallback;
import com.datdo.mobilib.util.MblUtils;


/**
 * <pre>
 * Class to post in-app events between objects.
 * This class keeps a {@link WeakReference} to listeners to prevent memory leak.
 * If you want to create an anonymous listener, please use {@link MblStrongEventListener}, otherwise the listener will be destroyed by GC.
 * </pre>
 */
public class MblEventCenter {
    private static final Map<String, MblWeakArrayList<MblEventListener>> sEventListenerMap = new ConcurrentHashMap<String, MblWeakArrayList<MblEventListener>>();

    /**
     * <pre>
     * Register a listener for multiple events.
     * </pre>
     * @param names event names
     */
    public static void addListener(MblEventListener listener, String[] names) {
        if (!MblUtils.isEmpty(names)) {
            for (String n : names) {
                addListener(listener, n);
            }
        }
    }

    /**
     * <pre>
     * Register a listener for one event.
     * </pre>
     * @param name
     */
    public static void addListener(MblEventListener listener, String name) {
        MblWeakArrayList<MblEventListener> listeners = null;

        if(sEventListenerMap.containsKey(name)) {
            listeners = sEventListenerMap.get(name);
        } else {
            listeners = new MblWeakArrayList<MblEventListener>();
            sEventListenerMap.put(name, listeners);
        }
        if(listeners.contains(listener)) return;

        listeners.add(listener);
    }

    /**
     * <pre>
     * Remove listener from one event
     * </pre>
     * @param name event name
     */
    public static void removeListenerFromEvent(MblEventListener listener, String name) {
        if(!sEventListenerMap.containsKey(name)) return;

        MblWeakArrayList<MblEventListener> listeners = null;

        listeners = sEventListenerMap.get(name);
        if(!listeners.contains(listener)) return;

        listeners.remove(listener);

        if(listeners.isEmpty()) {
            sEventListenerMap.remove(name);
        }
    }

    /**
     * <pre>
     * Remove listener from all events.
     * </pre>
     */
    public static void removeListenerFromAllEvents(MblEventListener listener) {
        Set<String> keys = sEventListenerMap.keySet();
        for (String aKey : keys) {
            removeListenerFromEvent(listener, aKey);
        }
    }

    /**
     * <pre>
     * Post event to all listeners.
     * The callback method {@link MblEventListener#onEvent(Object, String, Object...)} is invoked in main thread.
     * </pre>
     * @param sender object that posts the event
     * @param name event name
     * @param args data attached to event
     */
    public static void postEvent(final Object sender, final String name, final Object... args) {
        if(!sEventListenerMap.containsKey(name)) return;

        MblWeakArrayList<MblEventListener> listeners;
        listeners = sEventListenerMap.get(name);
        final List<Runnable> actions = new ArrayList<Runnable>();
        listeners.iterateWithCallback(new MblWeakArrayListCallback<MblEventListener>() {
            @Override
            public void onInterate(final MblEventListener listener) {
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        listener.onEvent(sender, name, args);
                    }
                });
            }
        });


        // post to main thread to prevent StackOverFlow
        MblUtils.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                for (Runnable r : actions) {
                    r.run();
                }
            }
        });
    }

    /**
     * <pre>
     * Get argument object from argument array safely (no {@link NullPointerException} or {@link IndexOutOfBoundsException})
     * This method is designated for {@link MblEventListener#onEvent(Object, String, Object...)}
     * </pre>
     * @param index specific index of argument
     * @param args argument array
     * @return argument object if index is valid, otherwise return null
     */
    public static Object getArgAt(int index, Object...args) {
        return args != null && args.length > index ? args[index] : null;
    }
}
