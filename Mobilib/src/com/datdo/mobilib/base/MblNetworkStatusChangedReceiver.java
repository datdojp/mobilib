package com.datdo.mobilib.base;

import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.util.MblUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

class MblNetworkStatusChangedReceiver extends BroadcastReceiver {

    private static enum MblNetworkStatus {
        ON, OFF
    }

    private MblNetworkStatus mLastStatus;

    public MblNetworkStatusChangedReceiver() {
        mLastStatus = getStatus();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MblNetworkStatus status = getStatus();
        if (mLastStatus != status) {
            boolean isOn = status == MblNetworkStatus.ON;
            MblEventCenter.postEvent(this,
                    isOn ? MblCommonEvents.NETWORK_ON : MblCommonEvents.NETWORK_OFF);
            mLastStatus = status;
        }
    }

    private MblNetworkStatus getStatus() {
        return MblUtils.isNetworkConnected() ? MblNetworkStatus.ON : MblNetworkStatus.OFF;
    }
}
