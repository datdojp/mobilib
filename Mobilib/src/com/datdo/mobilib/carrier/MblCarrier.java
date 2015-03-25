package com.datdo.mobilib.carrier;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.datdo.mobilib.base.MblBaseFragmentActivity;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblEventListener;
import com.datdo.mobilib.util.MblUtils;

@SuppressLint("InflateParams")
public abstract class MblCarrier extends MblBaseFragmentActivity implements MblEventListener {

    private static final String TAG = MblUtils.getTag(MblCarrier.class);

    static final class Events {
        static final String FINISH_INTERCEPTOR      = Events.class + "#finish_interceptor";
        static final String START_INTERCEPTOR       = Events.class + "#start_interceptor";
    }

    private boolean                     mInterceptorBeingStarted;
    private final Stack<MblInterceptor> mInterceptorStack = new Stack<MblInterceptor>();

    protected abstract void animateForStarting(
            final MblInterceptor    currentInterceptor,
            final MblInterceptor    nextInterceptor,
            final Runnable          onAnimationEnd);

    protected abstract void animateForFinishing(
            final MblInterceptor currentInterceptor,
            final MblInterceptor previousInterceptor,
            final Runnable onAnimationEnd);

    protected abstract FrameLayout getInterceptorContainerView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MblEventCenter.addListener(this, new String[] {
                Events.START_INTERCEPTOR,
                Events.FINISH_INTERCEPTOR
        });
    }

    public void finishAllInterceptors() {
        try {
            getInterceptorContainerView().removeAllViews();
            while(!mInterceptorStack.isEmpty()) {
                MblInterceptor interceptor = mInterceptorStack.pop();
                interceptor.onPause();
                interceptor.onDestroy();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Unable to finish all interceptors", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEvent(Object sender, String name, Object... args) {

        MblInterceptor interceptor = (MblInterceptor) sender;
        if (!mInterceptorStack.contains(interceptor)) {
            return;
        }

        if (Events.START_INTERCEPTOR == name) {
            startInterceptor((Class<? extends MblInterceptor>)args[0], (Map<String, Object>)args[1]);
        }

        if (Events.FINISH_INTERCEPTOR == name) {
            try {
                finishInterceptor((MblInterceptor)sender);
            } catch (MblInterceptorNotBelongToCarrierException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    public void startInterceptor(Class<? extends MblInterceptor> clazz, Object... extras) {
        startInterceptor(clazz, convertExtraArrayToMap(extras));
    }
    
    public void startInterceptor(final Class<? extends MblInterceptor> clazz, final Map<String, Object> extras) {

        if (mInterceptorBeingStarted) {
            return;
        }

        mInterceptorBeingStarted = true;

        try {
            final MblInterceptor nextInterceptor = clazz.getConstructor(Context.class, Map.class).newInstance(this, extras);

            if (mInterceptorStack.isEmpty()) {
                getInterceptorContainerView().addView(nextInterceptor);
                nextInterceptor.onResume();
                mInterceptorBeingStarted = false;
            } else {
                final MblInterceptor currentInterceptor = mInterceptorStack.peek();
                getInterceptorContainerView().addView(nextInterceptor);
                animateForStarting(
                        currentInterceptor,
                        nextInterceptor,
                        new Runnable() {
                            @Override
                            public void run() {
                                getInterceptorContainerView().removeView(currentInterceptor);
                                currentInterceptor.onPause();
                                nextInterceptor.onResume();
                                mInterceptorBeingStarted = false;
                            }
                        });
            }

            mInterceptorStack.push(nextInterceptor);

        } catch (Throwable e) {
            Log.e(TAG, "Unable to start interceptor: " + clazz, e);
        }
    }

    public void finishInterceptor(final MblInterceptor currentInterceptor) throws MblInterceptorNotBelongToCarrierException {

        if (!mInterceptorStack.contains(currentInterceptor)) {
            throw new MblInterceptorNotBelongToCarrierException("Unable to finish interceptor: interceptor does not belong to carrier.");
        }

        try {
            boolean isTop = currentInterceptor == mInterceptorStack.peek();
            if (isTop) {
                mInterceptorStack.pop();
                if (mInterceptorStack.isEmpty()) {
                    // just remove top interceptor
                    getInterceptorContainerView().removeView(currentInterceptor);
                    currentInterceptor.onPause();
                    currentInterceptor.onDestroy();
                } else {
                    final MblInterceptor previousInterceptor = mInterceptorStack.peek();
                    getInterceptorContainerView().addView(previousInterceptor);
                    animateForFinishing(
                            currentInterceptor,
                            previousInterceptor,
                            new Runnable() {
                                @Override
                                public void run() {
                                    getInterceptorContainerView().removeView(currentInterceptor);
                                    currentInterceptor.onPause();
                                    currentInterceptor.onDestroy();
                                    previousInterceptor.onResume();
                                }
                            });
                }
            } else {
                // just remove interceptor from stack silently
                mInterceptorStack.remove(currentInterceptor);
                currentInterceptor.onPause();
                currentInterceptor.onDestroy();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Unable to finish interceptor: " + currentInterceptor, e);
        }

        if (mInterceptorStack.isEmpty()) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            currentInterceptor.onResume();
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onResume()", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            currentInterceptor.onPause();
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onPause()", e);
        }
    }

    @Override
    protected void onDestroy() {
        finishAllInterceptors();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            currentInterceptor.onBackPressed();
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onBackPressed()", e);
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            currentInterceptor.onActivityResult(requestCode, resultCode, data);
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onBackPressed()", e);
            super.onBackPressed();
        }
    }

    static Map<String, Object> convertExtraArrayToMap(Object... extras) {

        Assert.assertTrue(extras == null || extras.length % 2 == 0);

        Map<String, Object> mapExtras = new HashMap<String, Object>();
        if (!MblUtils.isEmpty(extras)) {
            int i = 0;
            while (i < extras.length) {
                Object key = extras[i];
                Object value = extras[i+1];
                Assert.assertTrue(key != null && key instanceof String);
                if (value != null) {
                    mapExtras.put((String) key, value);
                }
                i += 2;
            }
        }

        return mapExtras;
    }
}
