package com.datdo.mobilib.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblEventListener;
import com.datdo.mobilib.event.MblStrongEventListener;
import com.datdo.mobilib.util.MblUtils;
import com.datdo.mobilib.util.MblViewUtil;

/**
 * <pre>
 * Plug an object of this class into an activity to make this library works.
 * </pre>
 */
public class MblActivityPlugin {

    // current status
    private int mOrientation;

    // wrapper views
    private MblDecorView mDecorView;

    // for background/foreground detecting
    private static long sLastOnPause = 0;
    private static Runnable sBackgroundStatusCheckTask = new Runnable() {
        @Override
        public void run() {
            MblEventCenter.postEvent(this, MblCommonEvents.GO_TO_BACKGROUND);
        }
    };
    private static final long DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY = 2000;
    protected long mMaxAllowedTrasitionBetweenActivity = DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY;

    /**
     * Extends Activity#onCreate(Bundle)
     * @param activity targeted activity
     * @param savedInstanceState
     */
    public void onCreate(Activity activity, Bundle savedInstanceState) {
        Context context = MblUtils.getCurrentContext();
        if (context == null || !(context instanceof Activity)) {
            MblUtils.setCurrentContext(activity);
        }

        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mOrientation = activity.getResources().getConfiguration().orientation;

        MblEventCenter.postEvent(this, MblCommonEvents.ACTIVITY_CREATED, activity, savedInstanceState);
    }

    /**
     * Extends Activity#onStart()
     * @param activity targeted activity
     */
    public void onStart(Activity activity) {
        MblEventCenter.postEvent(this, MblCommonEvents.ACTIVITY_STARTED, activity);
    }

    /**
     * Extends Activity#onResume()
     * @param activity targeted activity
     */
    public void onResume(Activity activity) {
        MblUtils.setCurrentContext(activity);

        MblUtils.getMainThreadHandler().removeCallbacks(sBackgroundStatusCheckTask);
        long now = getNow();
        if (now - sLastOnPause > mMaxAllowedTrasitionBetweenActivity) {
            MblEventCenter.postEvent(this, MblCommonEvents.GO_TO_FOREGROUND);
        }

        MblEventCenter.postEvent(this, MblCommonEvents.ACTIVITY_RESUMED, activity);
    }

    /**
     * Extends Activity#onPause()
     */
    public void onPause(Activity activity) {
        MblUtils.hideKeyboard();

        sLastOnPause = getNow();
        MblUtils.getMainThreadHandler().postDelayed(sBackgroundStatusCheckTask, mMaxAllowedTrasitionBetweenActivity);

        MblEventCenter.postEvent(this, MblCommonEvents.ACTIVITY_PAUSED, activity);
    }

    /**
     * Extends Activity#onStop()
     * @param activity targeted activity
     */
    public void onStop(Activity activity) {
        MblEventCenter.postEvent(this, MblCommonEvents.ACTIVITY_STOPPED, activity);
    }

    /**
     * Extends Activity#onConfigurationChanged(Configuration)
     * @param activity targeted activity
     * @param newConfig
     */
    public void onConfigurationChanged(Activity activity, Configuration newConfig) {
        if (newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            waitForWindowOrientationReallyChanged(activity, new Runnable() {
                @Override
                public void run() {
                    MblEventCenter.postEvent(this, MblCommonEvents.ORIENTATION_CHANGED);
                }
            });
        }
    }

    private void waitForWindowOrientationReallyChanged(final Activity activity, final Runnable callback) {
        if (MblUtils.isPortraitDisplay() != isPortraitWindow(activity)) {
            MblUtils.getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    waitForWindowOrientationReallyChanged(activity, callback);
                }
            }, 10);
        } else {
            callback.run();
        }
    }

    public boolean isPortraitWindow(Activity activity) {
        View root = activity.getWindow().getDecorView();
        return root.getWidth() <= root.getHeight();
    }

    private long getNow() {
        return System.currentTimeMillis();
    }

    /**
     * Determine whether targeted activity is top activity in current task
     * @param activity
     * @return
     */
    public boolean isTopActivity(Activity activity) {
        return MblUtils.getCurrentContext() == activity;
    }

    private View createDecorViewAndAddContent(Activity activity, int layoutResId, LayoutParams params) {
        View content = MblUtils.getLayoutInflater().inflate(layoutResId, null);
        return createDecorViewAndAddContent(activity, content, params);
    }

    private View createDecorViewAndAddContent(Activity activity, View layout, LayoutParams params) {
        if (params == null) {
            params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }
        layout.setLayoutParams(params);
        MblDecorView decorView = new MblDecorView(activity);
        decorView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        decorView.setBackgroundColor(0x0);
        decorView.addView(layout);

        mDecorView = decorView;

        setViewProcessor(activity);

        return decorView;
    }

    private void setViewProcessor(final Activity activity) {
        if (MblViewUtil.getGlobalViewProcessor() == null) {
            return;
        }

        final boolean[] scrollDirty = new boolean[] { false };

        final Runnable stopper = MblUtils.repeatDelayed(new Runnable() {
            @Override
            public void run() {
                if (scrollDirty[0]) {
                    MblViewUtil.iterateView(mDecorView, MblViewUtil.getGlobalViewProcessor());
                    scrollDirty[0] = false;
                }
            }
        }, 100);

        MblEventCenter.addListener(new MblStrongEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                if (args[0] == activity) {
                    stopper.run();
                    terminate();
                }
            }
        }, MblCommonEvents.ACTIVITY_DESTROYED);

        mDecorView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                scrollDirty[0] = true;
            }
        });
        mDecorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MblViewUtil.iterateView(mDecorView, MblViewUtil.getGlobalViewProcessor());
            }
        });
    }

    /**
     * Create wrapping content view for {@link Activity#setContentView(int)}
     * @param activity targeted activity
     * @param layoutResID
     * @return wrapping content view
     */
    public View getContentView(Activity activity, int layoutResID) {
        return createDecorViewAndAddContent(activity, layoutResID, null);
    }

    /**
     * Create wrapping content view for {@link Activity#setContentView(View)}
     * @param activity targeted activity
     * @param view
     * @return wrapping content view
     */
    public View getContentView(Activity activity, View view) {
        return createDecorViewAndAddContent(activity, view, null);
    }

    /**
     * Create wrapping content view for {@link Activity#setContentView(View, LayoutParams)}
     * @param activity targeted activity
     * @param view
     * @param params
     * @return wrapping content view
     */
    public View getContentView(Activity activity, View view, LayoutParams params) {
        return createDecorViewAndAddContent(activity, view, params);
    }

    /**
     * Extends Activity#onDestroy()
     * @param activity targeted activity
     */
    public void onDestroy(Activity activity) {
        if (activity instanceof MblEventListener) {
            MblEventCenter.removeListenerFromAllEvents((MblEventListener) activity);
        }
        MblUtils.cleanupView(mDecorView);

        MblEventCenter.postEvent(this, MblCommonEvents.ACTIVITY_DESTROYED, activity);
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        MblEventCenter.postEvent(this, MblCommonEvents.ACTIVITY_RESULT, activity, requestCode, resultCode, data);
    }

    /**
     * Get root view of activity
     * @param activity targeted activity
     * @return root view
     */
    public MblDecorView getDecorView(Activity activity) {
        return mDecorView;
    }

    /**
     * <pre>
     * Change max-allowed-transition-between-activities (abbreviation MATBA). Default value is 2 seconds.
     * This will effect {@link MblCommonEvents#GO_TO_BACKGROUND} and {@link MblCommonEvents#GO_TO_FOREGROUND}
     * 
     * The following is how this library detect changes between background (BG) and foreground (FG):
     * 1) Transition between activity A and activity B:
     *        A#onPaused() at t1 ------> B#onResumed() at t2
     *    In this case, t2-t1 < MTBA. FG status is not changed
     * 2) Go to BG
     *        A#onPaused() at t1 ------> Home screen ------> t2
     *    At t2 where t2-t1 > MTBA, FG status is changed to BG. {@link MblCommonEvents#GO_TO_BACKGROUND} event is fired
     * 3) Go back to FG
     *        A#onPaused() at t1 ------> Home screen ------> t2 (BG) ------> A#onResumed() at t3
     *    At t3 where t3-t1 > MTBA, BG status is changed to FG {@link MblCommonEvents#GO_TO_FOREGROUND} event is fired
     * </pre>
     * @param activity targeted activity
     * @param duration
     */
    public void setMaxAllowedTrasitionBetweenActivity(Activity activity, long duration) {
        mMaxAllowedTrasitionBetweenActivity = duration;
    }

    /**
     * @see #setMaxAllowedTrasitionBetweenActivity(Activity, long)
     */
    public void resetDefaultMaxAllowedTrasitionBetweenActivity(Activity activity) {
        mMaxAllowedTrasitionBetweenActivity = DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY;
    }
}
