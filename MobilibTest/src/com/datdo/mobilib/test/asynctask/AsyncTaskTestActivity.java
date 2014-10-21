package com.datdo.mobilib.test.asynctask;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.util.MblUtils;

public class AsyncTaskTestActivity extends MblBaseActivity {

    private static final String TAG = MblUtils.getTag(AsyncTaskTestActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new FrameLayout(this));

        for (int i = 0; i < 200; i++) {
            final String n = MblUtils.fillZero("" + (i+1), 3);
            MblUtils.executeOnAsyncThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Running async task #" + n + (MblUtils.isMainThread() ? ". ERROR: run on main thread!!!" : ""));
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Failed to sleep", e);
                    }
                    Log.i(TAG, "Finish async task #" + n);
                }
            });
        }
    }
}
