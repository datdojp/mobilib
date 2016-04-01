package com.datdo.mobilib.base;

import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblStrongEventListener;
import com.datdo.mobilib.util.MblUtils;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * <pre>
 * Root view for activity. This view is mainly for detecting keyboard ON/OFF.
 * At {@link #onSizeChanged(int, int, int, int)}, if screen_size - view_size > 200dp, it is considered "Keyboard ON", otherwise it is "Keyboard OFF".
 * </pre>
 */
public class MblDecorView extends FrameLayout {
    private static final int KB_SHOWN = 1;
    private static final int KB_HIDDEN = 2;
    private static final int MIN_KEYBOARD_HEIGHT = MblUtils.pxFromDp(200);

    private static int sKeyboardStatus = 0;

    private int mMaxDisplaySize;
    private int mMinDisplaySize;
    private MblOnSizeChangedListener mOnSizeChangedListener;


    public MblDecorView(Context context) {
        super(context);
        init();
    }

    public MblDecorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MblDecorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        int[] displaySizes = MblUtils.getDisplaySizes();
        mMaxDisplaySize = Math.max(displaySizes[0], displaySizes[1]);
        mMinDisplaySize = Math.min(displaySizes[0], displaySizes[1]);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }

        if (getContext() != MblUtils.getCurrentContext()) {
            return;
        }

        int maxVisibleSize = Math.max(w, h);
        int minVisibleSize = Math.min(w, h);

        int maxDiff = Math.max(Math.abs(mMaxDisplaySize - maxVisibleSize), Math.abs(mMinDisplaySize - minVisibleSize));
        int kbStt = (maxDiff >= MIN_KEYBOARD_HEIGHT) ? KB_SHOWN : KB_HIDDEN;
        if (sKeyboardStatus != kbStt) {
            boolean isShown = kbStt == KB_SHOWN;
            sKeyboardStatus = kbStt;
            MblEventCenter.postEvent(this,
                    isShown ? MblCommonEvents.KEYBOARD_SHOWN : MblCommonEvents.KEYBOARD_HIDDEN);
        }
    }

    public static interface MblOnSizeChangedListener {
        public void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    public void setOnSizeChangedListener(MblOnSizeChangedListener listener) {
        mOnSizeChangedListener = listener;
    }

    /**
     * @return whether keyboard is ON or OFF
     */
    public static boolean isKeyboardOn() {
        return sKeyboardStatus == KB_SHOWN;
    }

    static {
        MblEventCenter.addListener(new MblStrongEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                Activity activity = (Activity) args[0];
                boolean isTop = activity == MblUtils.getCurrentContext();
                if (isTop && activity.isFinishing() && isKeyboardOn()) {
                    sKeyboardStatus = KB_HIDDEN;
                    MblEventCenter.postEvent(null, MblCommonEvents.KEYBOARD_HIDDEN);
                }
            }
        }, MblCommonEvents.ACTIVITY_PAUSED);
    }
}
