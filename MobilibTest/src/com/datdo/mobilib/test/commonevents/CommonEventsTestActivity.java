package com.datdo.mobilib.test.commonevents;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblEventListener;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblUtils;

public class CommonEventsTestActivity extends MblBaseActivity implements MblEventListener {

    private static final String TAG = MblUtils.getTag(CommonEventsTestActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_events_test);

        MblEventCenter.addListener(this, new String[] {
                MblCommonEvents.ORIENTATION_CHANGED,
                MblCommonEvents.NETWORK_OFF,
                MblCommonEvents.NETWORK_ON,
                MblCommonEvents.BLUETOOTH_OFF,
                MblCommonEvents.BLUETOOTH_ON,
                MblCommonEvents.GO_TO_BACKGROUND,
                MblCommonEvents.GO_TO_FOREGROUND,
                MblCommonEvents.KEYBOARD_HIDDEN,
                MblCommonEvents.KEYBOARD_SHOWN
        });

        MblUtils.focusNothing(this);
    }

    @Override
    protected void onDestroy() {
        MblEventCenter.removeListenerFromAllEvents(this);
        super.onDestroy();
    }

    @Override
    public void onEvent(Object sender, String name, Object... args) {
        Log.d(TAG, "onEvent: " + name);
        if (MblCommonEvents.ORIENTATION_CHANGED == name) {
            String orientation;
            if (MblUtils.isPortraitDisplay()) {
                orientation = "portrait";
            } else {
                orientation = "landscape";
            }
            MblUtils.showToast("ORIENTATION CHANGED: " + orientation, Toast.LENGTH_SHORT);
        } else if (MblCommonEvents.NETWORK_OFF.equals(name)) {
            MblUtils.showToast("NETWORK OFF", Toast.LENGTH_SHORT);
        } else if (MblCommonEvents.NETWORK_ON.equals(name)) {
            MblUtils.showToast("NETWORK ON", Toast.LENGTH_SHORT);
        } else if (MblCommonEvents.GO_TO_BACKGROUND.equals(name)) {
            MblUtils.showToast("GO TO BACKGROUND", Toast.LENGTH_SHORT);
            Log.d(TAG, "MblUtils.isAppInForeGround()=" + MblUtils.isAppInForeGround());
        } else if (MblCommonEvents.GO_TO_FOREGROUND.equals(name)) {
            MblUtils.showToast("GO TO FOREGROUND", Toast.LENGTH_SHORT);
            Log.d(TAG, "MblUtils.isAppInForeGround()=" + MblUtils.isAppInForeGround());
        } else if (MblCommonEvents.KEYBOARD_HIDDEN.equals(name)) {
            MblUtils.showToast("KEYBOARD HIDDEN", Toast.LENGTH_SHORT);
        } else if (MblCommonEvents.KEYBOARD_SHOWN.equals(name)) {
            MblUtils.showToast("KEYBOARD SHOWN", Toast.LENGTH_SHORT);
        } else if (MblCommonEvents.BLUETOOTH_OFF.equals(name)) {
            MblUtils.showToast("BLUETOOTH OFF", Toast.LENGTH_SHORT);
        } else if (MblCommonEvents.BLUETOOTH_ON.equals(name)) {
            MblUtils.showToast("BLUETOOTH ON", Toast.LENGTH_SHORT);
        }
    }
}
