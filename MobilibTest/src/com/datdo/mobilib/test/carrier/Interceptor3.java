package com.datdo.mobilib.test.carrier;

import java.util.Map;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.datdo.mobilib.carrier.MblInterceptor;
import com.datdo.mobilib.test.R;

public class Interceptor3 extends MblInterceptor {

    public Interceptor3(Context context, Map<String, Object> extras) {
        super(context, extras);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.interceptor_3);

        ((TextView)findViewById(R.id.text)).setText((String) getExtra("text", null));

        findViewById(R.id.button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startInterceptor(Interceptor4.class, "text", "From Interceptor 3");
            }
        });
    }
}
