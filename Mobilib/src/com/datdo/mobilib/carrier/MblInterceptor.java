package com.datdo.mobilib.carrier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.datdo.mobilib.util.MblUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * Interceptor class of Carrier/Interceptor model.
 * Interceptor is designed to be as identical to Activity as possible, so that coding life is easier.
 * Note that subclass of this class must have a public constructor like this
 * {@code
 *      public class Interceptor1 extends MblInterceptor {
 *          public Interceptor1(Context context, Map<String, Object> extras) {
 *              super(context, extras);
 *              // ... do something here to initialize this interceptor
 *          }
 *      }
 * }
 * </pre>
 * @see com.datdo.mobilib.carrier.MblCarrier
 */
@SuppressLint("InflateParams")
public abstract class MblInterceptor extends FrameLayout {

    private static final String TAG = MblUtils.getTag(MblInterceptor.class);

    private MblCarrier                  mCarrier;
    private final Map<String, Object>   mExtras = new ConcurrentHashMap<String, Object>();

    public MblInterceptor(Context context, MblCarrier carrier, Map<String, Object> extras) {
        super(context);
        mCarrier = carrier;
        mExtras.clear();
        if (!MblUtils.isEmpty(extras)) {
            mExtras.putAll(extras);
        }
        onCreate();
    }

    /**
     * Get parameter passed to this interceptor.
     * @param key parameter name
     * @param defaultVal default value if param is not found
     * @return value of parameter
     */
    public Object getExtra(String key, Object defaultVal) {
        Object ret = mExtras.get(key);
        if (ret != null) {
            return ret;
        } else {
            return defaultVal;
        }
    }

    /**
     * Set content view for this interceptor
     * @see android.app.Activity#setContentView(int)
     */
    public void setContentView(int layoutResId) {
        View contentView = LayoutInflater.from(getContext()).inflate(layoutResId, null);
        setContentView(contentView);
    }

    /**
     * Set content view for this interceptor
     * @see android.app.Activity#setContentView(View)
     */
    public void setContentView(View contentView) {
        removeAllViews();
        addView(contentView);
    }

    /**
     * Create a View from its layout ID.
     */
    public View inflate(int layoutResId) {
        return LayoutInflater.from(getContext()).inflate(layoutResId, null);
    }

    /**
     * Finish this interceptor.
     * @see android.app.Activity#finish()
     */
    public void finish() {
        if (mCarrier != null) {
            mCarrier.finishInterceptor(this);
        }
    }

    /**
     * Start another interceptor.
     * @param clazz class of interceptor to start
     * @param options extra option when adding new interceptor to carrier
     * @param extras parameters passed to the new interceptor, in key,value (for example: "param1", param1Value, "param1", param2Value, ...)
     */
    public void startInterceptor(Class<? extends MblInterceptor> clazz, MblCarrier.Options options, Object... extras) {
        startInterceptor(clazz, options, MblCarrier.convertExtraArrayToMap(extras));
    }

    /**
     * Start another interceptor.
     * @param clazz class of interceptor to start
     * @param options extra option when adding new interceptor to carrier
     * @param extras parameters passed to the new interceptor, in key,value
     */
    public void startInterceptor(Class<? extends MblInterceptor> clazz, MblCarrier.Options options, Map<String, Object> extras) {
        if (mCarrier != null) {
            mCarrier.startInterceptor(clazz, options, extras);
        }
    }

    /**
     * Invoked when this interceptor is created.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate() {}

    /**
     * Invoked when this interceptor is displayed.
     * @see android.app.Activity#onResume()
     */
    public void onResume() {
    }

    /**
     * Invoked when this interceptor is not displayed any more (destroyed or navigate to other interceptor)
     * @see android.app.Activity#onPause()
     */
    public void onPause() {
    }

    /**
     * Invoked when this interceptor is detached from parent carrier and ready to be recycled by Garbage Collector.
     * @see android.app.Activity#onDestroy()
     */
    public void onDestroy() {
        MblUtils.cleanupView(this);
    }

    /**
     * Invoked when parent Activity received activity result.
     * @return true if this interceptor handled the activity result
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    /**
     * Invoked when user presses Android Back button
     * @return true if this interceptor handled the event
     * @see android.app.Activity#onBackPressed()
     */
    public boolean onBackPressed() {
        finish();
        return true;
    }

    /**
     * Check if this interceptor is on top of interceptor stack of its parent carrier.
     */
    public boolean isTop() {
        if (mCarrier == null) {
            return false;
        }
        List<MblInterceptor> list = mCarrier.getInterceptors();
        int index = list.indexOf(this);
        return index >= 0 && index == list.size() - 1;
    }

    /**
     * Get {@link com.datdo.mobilib.carrier.MblCarrier} instance bound with this interceptor
     * @return
     */
    public MblCarrier getCarrier() {
        return mCarrier;
    }
}
