package com.datdo.mobilib.exception;

import android.os.Build;

public class CurrentSdkNotSupportedException extends Exception {
    public CurrentSdkNotSupportedException(int minSdkVersion) {
        super("Require api" + minSdkVersion + ", current api is " + Build.VERSION.SDK_INT);
    }
}