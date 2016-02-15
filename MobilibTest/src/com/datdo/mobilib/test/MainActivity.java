package com.datdo.mobilib.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.datdo.mobilib.api.MblApi;
import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.test.asynctask.AsyncTaskTestActivity;
import com.datdo.mobilib.test.carrier.CarrierTestActivity;
import com.datdo.mobilib.test.commonevents.CommonEventsTestActivity;
import com.datdo.mobilib.test.imageinput.ImageInputTestActivity;
import com.datdo.mobilib.test.imageloader.ImageLoaderTestActivity;
import com.datdo.mobilib.test.urlrecognizer.UrlRecognizerTestActivity;
import com.datdo.mobilib.test.utils.UtilsTestActivity;
import com.datdo.mobilib.util.MblUtils;


public class MainActivity extends MblBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.bt_image_loader).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openTestActivity(ImageLoaderTestActivity.class);
            }
        });

        findViewById(R.id.bt_comment_events).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openTestActivity(CommonEventsTestActivity.class);
            }
        });

        findViewById(R.id.bt_url_recognizer).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openTestActivity(UrlRecognizerTestActivity.class);
            }
        });

        findViewById(R.id.bt_async_tasl).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openTestActivity(AsyncTaskTestActivity.class);
            }
        });

        findViewById(R.id.bt_utils).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openTestActivity(UtilsTestActivity.class);
            }
        });

        findViewById(R.id.bt_close_app).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MblUtils.closeApp(MainActivity.class);
            }
        });

        findViewById(R.id.bt_image_input).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                openTestActivity(ImageInputTestActivity.class);
            }
        });

        findViewById(R.id.bt_api_clear_cache).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MblApi.clearCache();
                MblUtils.showToast("Done", Toast.LENGTH_SHORT);
            }
        });

        findViewById(R.id.bt_carrier).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                openTestActivity(CarrierTestActivity.class);
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void openTestActivity(Class clazz) {
        startActivity(new Intent(this, clazz));
    }

    @Override
    protected void onResume() {
        super.onResume();
        UnitTest.run();
    }
}
