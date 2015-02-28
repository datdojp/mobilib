package com.datdo.mobilib.test.utils;

import java.util.Date;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.test.MainActivity;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblUtils;

public class UtilsTestActivity extends MblBaseActivity {

    private Runnable mStopRepeatAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utils_test);

        findViewById(R.id.bt_show_toast).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MblUtils.showToast("This toast is shown from Main Thread", Toast.LENGTH_SHORT);
                MblUtils.executeOnAsyncThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        MblUtils.showToast("This toast is shown from Async Thread", Toast.LENGTH_SHORT);
                    }
                });
            }
        });

        findViewById(R.id.bt_show_progress_dialog).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MblUtils.showProgressDialog("This progress dialog will close after 3 seconds", false);
                MblUtils.getMainThreadHandler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        MblUtils.hideProgressDialog();
                    }
                }, 3000);
            }
        });

        findViewById(R.id.bt_close_app).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MblUtils.closeApp(MainActivity.class);
            }
        });

        findViewById(R.id.bt_hash_key).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String hashKey = MblUtils.getKeyHash();
                MblUtils.showToast(hashKey, Toast.LENGTH_SHORT);
            }
        });

        findViewById(R.id.bt_start_repeat).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mStopRepeatAction = MblUtils.repeatDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MblUtils.showToast("Current time: " + new Date(), Toast.LENGTH_SHORT);
                    }
                }, 3000l);
            }
        });

        findViewById(R.id.bt_stop_repeat).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mStopRepeatAction != null) {
                    mStopRepeatAction.run();
                    mStopRepeatAction = null;
                }
            }
        });
    }
}