package com.datdo.mobilib.carrier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.datdo.mobilib.carrier.MblCarrier.Events;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.util.MblUtils;

@SuppressLint("InflateParams")
public abstract class MblInterceptor extends FrameLayout {

    private boolean                     mIsTop;
    private final Map<String, Object>   mExtras = new ConcurrentHashMap<String, Object>();

    protected MblInterceptor(Context context, Map<String, Object> extras) {
        super(context);
        mExtras.clear();
        if (!MblUtils.isEmpty(extras)) {
            mExtras.putAll(extras);
        }
        onCreate();
    }

    public Object getExtra(String key, Object defaultVal) {
        Object ret = mExtras.get(key);
        if (ret != null) {
            return ret;
        } else {
            return defaultVal;
        }
    }

    public void setContentView(int layoutResId) {
        View contentView = (ViewGroup) MblUtils.getLayoutInflater().inflate(layoutResId, null);
        setContentView(contentView);
    }

    public void setContentView(View contentView) {
        removeAllViews();
        addView(contentView);
    }

    public View inflate(int layoutResId) {
        return MblUtils.getLayoutInflater().inflate(layoutResId, null);
    }

    public void finish() {
        MblEventCenter.postEvent(this, Events.FINISH_INTERCEPTOR);
    }

    public void startInterceptor(Class<? extends MblInterceptor> clazz, Object... extras) {
        startInterceptor(clazz, MblCarrier.convertExtraArrayToMap(extras));
    }

    public void startInterceptor(Class<? extends MblInterceptor> clazz, Map<String, Object> extras) {
        MblEventCenter.postEvent(this, Events.START_INTERCEPTOR, clazz, extras);
    }

    public void onCreate() {}

    public void onResume() {
        mIsTop = true;
    }

    public void onPause() {
        mIsTop = false;
    }

    public void onDestroy() {
        MblUtils.cleanupView(this);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    public boolean onBackPressed() {
        finish();
        return true;
    }

    public boolean isTop() {
        return mIsTop;
    }
}
