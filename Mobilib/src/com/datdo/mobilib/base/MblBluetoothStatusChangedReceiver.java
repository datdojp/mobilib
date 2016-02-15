package com.datdo.mobilib.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.util.MblUtils;

class MblBluetoothStatusChangedReceiver extends BroadcastReceiver {

    private static enum MblBluetoothStatus {
        ON, OFF
    }

    private MblBluetoothStatus mLastStatus;

    public MblBluetoothStatusChangedReceiver() {
        mLastStatus = getStatus();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MblBluetoothStatus status = getStatus();
        if (mLastStatus != status) {
            boolean isOn = status == MblBluetoothStatus.ON;
            MblEventCenter.postEvent(this,
                    isOn ? MblCommonEvents.BLUETOOTH_ON : MblCommonEvents.BLUETOOTH_OFF);
            mLastStatus = status;
        }
    }

    private MblBluetoothStatus getStatus() {
        try {
            return MblUtils.isBluetoothOn() ? MblBluetoothStatus.ON : MblBluetoothStatus.OFF;
        } catch (Exception e) { // android.permission.BLUETOOTH is not allowed, etc...
            return null;
        }
    }
}
