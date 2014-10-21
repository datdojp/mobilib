package com.datdo.mobilib.event;

import com.datdo.mobilib.util.MblUtils;

/**
 * Events frequently used in Android apps.
 */
public final class MblCommonEvents {
    /**
     * <pre>
     * Screen orientation is changed between Portrait <--> Landscape.
     * Call {@link MblUtils#isPortraitDisplay()} to get current orientation
     * </pre>
     */
    public static final String ORIENTATION_CHANGED              = MblCommonEvents.class + "#orientation_changed";

    /**
     * <pre>
     * Network is connected.
     * Call {@link MblUtils#isNetworkConnected()} to get current network status.
     * </pre>
     */
    public static final String NETWORK_ON                       = MblCommonEvents.class + "#network_on";

    /**
     * <pre>
     * Network is disconnected.
     * Call {@link MblUtils#isNetworkConnected()} to get current network status.
     * </pre>
     */
    public static final String NETWORK_OFF                      = MblCommonEvents.class + "#network_off";

    /**
     * <pre>
     * Keyboard is shown.
     * </pre>
     * Call {@link MblUtils#isKeyboardOn()} to get current keyboard status.
     */
    public static final String KEYBOARD_SHOWN                   = MblCommonEvents.class + "#keyboard_shown";

    /**
     * <pre>
     * Keyboard is hidden.
     * </pre>
     * Call {@link MblUtils#isKeyboardOn()} to get current keyboard status.
     */
    public static final String KEYBOARD_HIDDEN                  = MblCommonEvents.class + "#keyboard_hidden";

    /**
     * <pre>
     * App goes to background (to Home screen, to other app, or screen is locked).
     * Call {@link MblUtils#isAppInForeGround()} to get background/foreground status.
     * </pre>
     */
    public static final String GO_TO_BACKGROUND                 = MblCommonEvents.class + "#go_to_background";

    /**
     * <pre>
     * App goes to foreground.
     * Call {@link MblUtils#isAppInForeGround()} to get background/foreground status.
     * </pre>
     */
    public static final String GO_TO_FOREGROUND                 = MblCommonEvents.class + "#go_to_foreground";

    /**
     * <pre>
     * An activity is resumed.
     * The activity is attached in args of {@link MblEventListener#onEvent(Object, String, Object...)}.
     * </pre> 
     */
    public static final String ACTIVITY_RESUMED                 = MblCommonEvents.class + "#activity_resumed";
}
