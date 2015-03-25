package com.datdo.mobilib.test.carrier;

import java.util.Map;

import android.content.Context;
import android.widget.TextView;

import com.datdo.mobilib.carrier.MblInterceptor;
import com.datdo.mobilib.test.R;

public class Interceptor4 extends MblInterceptor {

    public Interceptor4(Context context, Map<String, Object> extras) {
        super(context, extras);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.interceptor_4);

        ((TextView)findViewById(R.id.text)).setText((String) getExtra("text", null));
    }
}
