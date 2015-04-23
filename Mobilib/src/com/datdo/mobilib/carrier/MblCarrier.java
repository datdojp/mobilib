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

/**
 * <pre>
 * Carrier/Interceptor is a alternative of old Activity/Fragment model.
 *
 * Due to the fact that Activity/Fragment model has too many drawbacks:
 *   1. Quite complicated to start and manage lifecycle.
 *      :::How you start a Fragment with parameters
 *      {@code
 *      Fragment newFragment = new ExampleFragment();
 *      Bundle args = new Bundle();
 *      args.putInt("param1", param1);
 *      args.putInt("param2", param2);
 *      newFragment.setArguments(args);
 *      FragmentTransaction transaction = getFragmentManager().beginTransaction(); // or getSupportFragmentManager()
 *      transaction.replace(R.id.fragment_container, newFragment);
 *      transaction.addToBackStack(null);
 *      transaction.commit();
 *      }
 *      :::Fragment 's lifecycle (quite different from Activity 's lifecycle, why Google didn't make coding simpler?)
 *      onAttach -> onCreate -> onCreateView -> onActivityCreated -> onStart -> onResume -> onPause -> onStop -> onDestroyView -> onDestroy -> onDetach

 *   2. Cause potential bugs (especially {@code Fragment#getActivity()} method which causes {@link java.lang.NullPointerException}.
 *   3. Fragment can not contain another fragment (for example: you can not add Google MapFragment into your fragment)
 *   4. Unable to start a fragment directly from another fragment while an Activity can be started directly from another Activity (you can do it by using getActivity() method, but it is still complicated, as mentioned in [1])
 *   5. Activity must be subclass of FragmentActivity.
 *
 * it is recommended to use Carrier/Interceptor alternative when you need to render multiple sub-screens in a parent screen.
 *
 * Benefits of Carrier/Interceptor:
 *   1. Easy to use
 *     :::How you start an Interceptor with parameters
 *     {@code
 *     carrier.startInterceptor(ExampleInterceptor.class, "param1", param1, "param2", param2);
 *     }
 *     :::Interceptor 's lifecycle just looks like Activity 's lifecycle, even simpler
 *     onCreate -> onResume -> onPause -> onDestroy
 *   2. Interceptor can contains another interceptor due to the fact that interceptor is just a View
 *   3. You can start an interceptor from another interceptor, just like starting Activity from another Activity, even simpler
 *     {@code
 *     public class ExampleInterceptor extends MblInterceptor {
 *         public void foo() {
 *             startInterceptor(NextInterceptor.class, "param1", param1, "param2", param2);
 *         }
 *     }
 *     }
 *   4. MblCarrier is just an object wrapping a {@link FrameLayout} view which is the container view of its Interceptors, therefore Carrier can be plugged in any Activity or View.
 *
 * Sample code:
 *
 * {@code
 *      FrameLayout interceptorContainerView = (FrameLayout) findViewById(R.id.interceptor_container);
 *      mCarrier = new MblSlidingCarrier(this, interceptorContainerView, new MblCarrier.MblCarrierCallback() {
 *          @Override
 *          public void onNoInterceptor() {
 *              // ... handle when Carrier does not contain any Interceptor
 *          }
 *      });
 *      mCarrier.startInterceptor(Interceptor1.class);
 * }
 *
 * P/S: the name "Carrier/Interceptor" is inspired by legendary game Starcraft ;)
 * </pre>
 * @see com.datdo.mobilib.carrier.MblInterceptor
 */
@SuppressLint("InflateParams")
public abstract class MblCarrier implements MblEventListener {

    private static final String TAG = MblUtils.getTag(MblCarrier.class);

    static final class Events {
        static final String FINISH_INTERCEPTOR      = Events.class + "#finish_interceptor";
        static final String START_INTERCEPTOR       = Events.class + "#start_interceptor";
    }

    /**
     * Callback interface for {@link com.datdo.mobilib.carrier.MblCarrier}.
     */
    public static interface MblCarrierCallback {
        /**
         * Invoked when Carrier does not contain any Interceptor.
         */
        public void onNoInterceptor();
    }

    protected Context                   mContext;
    protected FrameLayout               mInterceptorContainerView;
    protected MblCarrierCallback        mCallback;
    private boolean                     mInterceptorBeingStarted;
    private final Stack<MblInterceptor> mInterceptorStack = new Stack<MblInterceptor>();


    /**
     * Constructor
     * @param context activity in which this Carrier is plugged
     * @param interceptorContainerView view that contains all Interceptors of this Carrier
     * @param callback instance of {@link com.datdo.mobilib.carrier.MblCarrier.MblCarrierCallback}
     */
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

    /**
     * <pre>
     * Overridden by subclasses.
     * This method defines the animations when navigating from currentInterceptor to nextInterceptor.
     * </pre>
     * @param currentInterceptor interceptor which is currently displayed
     * @param nextInterceptor next interceptor to navigate
     * @param onAnimationEnd invoked when animation has ended
     */
    protected abstract void animateForStarting(
            final MblInterceptor    currentInterceptor,
            final MblInterceptor    nextInterceptor,
            final Runnable          onAnimationEnd);

    /**
     * <pre>
     * Overridden by subclasses.
     * This method defines the animations when navigating from current interceptor back to previous interceptor.
     * </pre>
     * @param currentInterceptor interceptor which is currently displayed
     * @param previousInterceptor previous interceptor to navigate back
     * @param onAnimationEnd invoked when animation has ended
     */
    protected abstract void animateForFinishing(
            final MblInterceptor currentInterceptor,
            final MblInterceptor previousInterceptor,
            final Runnable onAnimationEnd);

    /**
     * Destroy all interceptors.
     */
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

    /**
     * Start an interceptor for this Carrier.
     * @param clazz class of interceptor to start
     * @param extras parameters passed to the new interceptor, in key,value (for example: "param1", param1Value, "param1", param2Value, ...)
     * @return the new interceptor instance
     */
    public MblInterceptor startInterceptor(Class<? extends MblInterceptor> clazz, Object... extras) {
        return startInterceptor(clazz, convertExtraArrayToMap(extras));
    }

    /**
     * Start an interceptor for this Carrier.
     * @param clazz class of interceptor to start
     * @param extras parameters passed to the new interceptor, in key,value
     * @return the new interceptor instance
     */
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

    /**
     * Destroy an interceptor.
     * @param currentInterceptor interceptor to destroy
     * @throws MblInterceptorNotBelongToCarrierException thrown when the interceptor does not belongs to this carrier
     */
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

    /**
     * <pre>
     * Handle when user pressed Android Back button.
     * This method is called by parent Activity of this carrier
     * {@code
     *      public class ExampleActivity extends MblBaseActivity {
     *          @Override
     *          public void onBackPressed() {
     *              if (mCarrier.onBackPressed()) {
     *                  return;
     *              }
     *              // ...
     *              super.onBackPressed();
     *          }
     *      }
     * }
     * </pre>
     * @return true if this event is handled by current interceptor
     */
    public boolean onBackPressed() {
        try {
            MblInterceptor currentInterceptor = mInterceptorStack.peek();
            return currentInterceptor.onBackPressed();
        } catch (Throwable e) {
            Log.e(TAG, "Unable to handle onBackPressed()", e);
            return false;
        }
    }

    /**
     * <pre>
     * Handle when parent Activity of this carrier receives activity result.
     * This method is called by parent Activity of this carrier
     * {@code
     *      public class ExampleActivity extends MblBaseActivity {
     *          @Override
     *          public void onActivityResult(int requestCode, int resultCode, Intent data) {
     *              if (mCarrier.onActivityResult(requestCode, resultCode, data)) {
     *                  return;
     *              }
     *              // ...
     *              super.onActivityResult();
     *          }
     *      }
     * }
     * </pre>
     * @return true if this event is handled by current interceptor
     */
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
