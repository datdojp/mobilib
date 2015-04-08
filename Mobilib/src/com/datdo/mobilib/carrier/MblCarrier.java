package com.datdo.mobilib.carrier;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.FrameLayout;

import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblEventListener;
import com.datdo.mobilib.util.MblUtils;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

@SuppressLint("InflateParams")
public abstract class MblCarrier implements MblEventListener {

    private static final String TAG = MblUtils.getTag(MblCarrier.class);

    static final class Events {
        static final String FINISH_INTERCEPTOR      = Events.class + "#finish_interceptor";
        static final String START_INTERCEPTOR       = Events.class + "#start_interceptor";
    }

    public static interface MblCarrierCallback {
        public void onNoInterceptor();
    }

    protected Context                   mContext;
    protected FrameLayout               mInterceptorContainerView;
    protected MblCarrierCallback        mCallback;
    private boolean                     mInterceptorBeingStarted;
    private final Stack<MblInterceptor> mInterceptorStack = new Stack<MblInterceptor>();


    public MblCarrier(Context context, FrameLayout interceptorContainerView, MblCarrierCallback callback) {

        mContext                    = context;
        mInterceptorContainerView   = interceptorContainerView;
        mCallback                   = callback;

        MblEventCenter.addListener(this, new String[] {
                Events.START_INTERCEPTOR,
                Events.FINISH_INTERCEPTOR,
                MblCommonEvents.ACTIVITY_RESUMED,
                MblCommonEvents.ACTIVITY_PAUSED,
                MblCommonEvents.ACTIVITY_DESTROYED
        });
    }

    protected abstract void animateForStarting(
            final MblInterceptor    currentInterceptor,
            final MblInterceptor    nextInterceptor,
            final Runnable          onAnimationEnd);

    protected abstract void animateForFinishing(
            final MblInterceptor currentInterceptor,
            final MblInterceptor previousInterceptor,
            final Runnable onAnimationEnd);

    public void finishAllInterceptors() {
        try {
            mInterceptorContainerView.removeAllViews();
            while(!mInterceptorStack.isEmpty()) {
                MblInterceptor interceptor = mInterceptorStack.pop();
                interceptor.onPause();
                interceptor.onDestroy();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Unable to finish all interceptors", e);
        }

        if (mInterceptorStack.isEmpty()) {
            if (mCallback != null) {
                mCallback.onNoInterceptor();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEvent(Object sender, String name, Object... args) {

        if (sender instanceof MblInterceptor) {
            MblInterceptor interceptor = (MblInterceptor) sender;
            if (!mInterceptorStack.contains(interceptor)) {
                return;
            }

            if (Events.START_INTERCEPTOR == name) {
                startInterceptor((Class<? extends MblInterceptor>) args[0], (Map<String, Object>) args[1]);
            }

            if (Events.FINISH_INTERCEPTOR == name) {
                try {
                    finishInterceptor((MblInterceptor) sender);
                } catch (MblInterceptorNotBelongToCarrierException e) {
                    Log.e(TAG, "", e);
                }
            }
        }

        if (sender instanceof Activity) {
            if (sender != mContext) {
                return;
            }

            if (MblCommonEvents.ACTIVITY_RESUMED == name) {
                onResume();
            }

            if (MblCommonEvents.ACTIVITY_PAUSED == name) {
                onPause();
            }

            if (MblCommonEvents.ACTIVITY_DESTROYED == name) {
                onDestroy();
            }
        }
    }

    public MblInterceptor startInterceptor(Class<? extends MblInterceptor> clazz, Object... extras) {
        return startInterceptor(clazz, convertExtraArrayToMap(extras));
    }

    public MblInterceptor startInterceptor(final Class<? extends MblInterceptor> clazz, final Map<String, Object> extras) {

        if (mInterceptorBeingStarted) {
            return null;
        }

        mInterceptorBeingStarted = true;

        try {
            final MblInterceptor nextInterceptor = clazz.getConstructor(Context.class, Map.class).newInstance(mContext, extras);

            if (mInterceptorStack.isEmpty()) {
                mInterceptorContainerView.addView(nextInterceptor);
                nextInterceptor.onResume();
                mInterceptorBeingStarted = false;
            } else {
                final MblInterceptor currentInterceptor = mInterceptorStack.peek();
                mInterceptorContainerView.addView(nextInterceptor);
                animateForStarting(
                        currentInterceptor,
                        nextInterceptor,
                        new Runnable() {
                            @Override
                            public void run() {
                                mInterceptorContainerView.removeView(currentInterceptor);
                                currentInterceptor.onPause();
                                nextInterceptor.onResume();
                                mInterceptorBeingStarted = false;
                            }
                        });
            }

            mInterceptorStack.push(nextInterceptor);
            return nextInterceptor;
        } catch (Throwable e) {
            Log.e(TAG, "Unable to start interceptor: " + clazz, e);
            return null;
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
                    mInterceptorContainerView.removeView(currentInterceptor);
                    currentInterceptor.onPause();
                    currentInterceptor.onDestroy();
                } else {
                    final MblInterceptor previousInterceptor = mInterceptorStack.peek();
                    mInterceptorContainerView.addView(previousInterceptor);
                    animateForFinishing(
                            currentInterceptor,
                            previousInterceptor,
                            new Runnable() {
                                @Override
                                public void run() {
                                    mInterceptorContainerView.removeView(currentInterceptor);
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
            if (mCallback != null) {
                mCallback.onNoInterceptor();
            }
        }
    }

    private void onResume() {
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            currentInterceptor.onResume();
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onResume()", e);
        }
    }

    private void onPause() {
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            currentInterceptor.onPause();
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onPause()", e);
        }
    }

    private void onDestroy() {
        finishAllInterceptors();
    }

    public boolean onBackPressed() {
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            return currentInterceptor.onBackPressed();
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onBackPressed()", e);
            return false;
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            return currentInterceptor.onActivityResult(requestCode, resultCode, data);
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onBackPressed()", e);
            return false;
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
