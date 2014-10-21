package com.datdo.mobilib.widget;

import com.datdo.mobilib.util.MblUtils;

import junit.framework.Assert;
import android.content.Context;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class MblSideMenuEnabledLayout extends FrameLayout {
    private static final int DEFAULT_HIDDEN_VIEW_MARGIN_IN_DP = 50;
    private static final int SCROLL_X_PER_Y_RATE_THRESHHOLD = 4;

    private static final int LEFT_VIEW_ID = 1;
    private static final int MID_VIEW_ID = 2;
    private static final int RIGHT_VIEW_ID = 3;

    private static final int DIRECTION_LEFT_TO_RIGHT = -1;
    private static final int DIRECTION_RIGHT_TO_LEFT = 1;

    public static enum SidePosition {
        LEFT, MID, RIGHT;
    }

    // views
    private ViewGroup mLeftView;
    private ViewGroup mMidView;
    private ViewGroup mRightView;
    private boolean mHasLeftContent;
    private boolean mHasRightContent;
    private int mHiddenViewMargin;
    private int mShadowPadding;

    // gestures
    private SidePosition mSidePos = SidePosition.MID;
    private Scroller mScroller;
    private GestureDetector mGestureDetector;
    private float mCurrentX;
    private boolean mDraggingHorizontally;
    private boolean mFlingDetected;
    private int mFlingDirection;
    private boolean mTapMidViewToCloseSideViewDetected;
    private Handler mMainThread = new Handler();
    private MblSideMenuEnabledLayoutDelegate mDelegate;

    public MblSideMenuEnabledLayout(Context context) {
        super(context);
    }

    public MblSideMenuEnabledLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MblSideMenuEnabledLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(
            final Context context,
            final Object leftContent,
            final Object midContent,
            final Object rightContent,
            final int hiddenViewMargin,
            final int shadowPadding,
            final int shadowDrawableResId,
            final MblSideMenuEnabledLayoutDelegate delegate) {

        // assertions
        Assert.assertTrue(midContent != null);

        // options
        mHiddenViewMargin = hiddenViewMargin < 0 ? MblUtils.pxFromDp(DEFAULT_HIDDEN_VIEW_MARGIN_IN_DP) : hiddenViewMargin;
        mShadowPadding = shadowPadding > 0 && shadowDrawableResId > 0 ? shadowPadding : 0;
        mHasLeftContent = leftContent != null;
        mHasRightContent = rightContent != null;
        mDelegate = delegate;

        // scroller && gesture detector
        mScroller = new Scroller(context);
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private boolean mShouldHandle;
            @Override
            public boolean onDown(MotionEvent e) {
                mFlingDetected = false;
                mTapMidViewToCloseSideViewDetected = false;
                mShouldHandle = MblUtils.motionEventOnView(e, mMidView);
                return mShouldHandle;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!mShouldHandle) return false;

                if (mSidePos != SidePosition.MID && MblUtils.motionEventOnView(e, mMidView)) {
                    mTapMidViewToCloseSideViewDetected = true;
                    return true;
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!mShouldHandle) return false;

                // check if this dragging should be considered as horizontal dragging
                float rate = distanceY != 0 ? distanceX/distanceY : SCROLL_X_PER_Y_RATE_THRESHHOLD;
                if (Math.abs(rate) >= SCROLL_X_PER_Y_RATE_THRESHHOLD) {
                    mDraggingHorizontally = true;
                }
                if (mDraggingHorizontally) {
                    float toX = mCurrentX - distanceX;
                    toX = Math.max(toX, getMostLeftX());
                    toX = Math.min(toX, getMostRightX());
                    scrollTo(toX);
                }

                // return true if a horizontal dragging is detected
                return mDraggingHorizontally;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (!mShouldHandle) return false;

                // if left content or right content is empty, forbid flinging to that direction
                int flingDirection = velocityX > 0 ? DIRECTION_LEFT_TO_RIGHT : DIRECTION_RIGHT_TO_LEFT;
                boolean isWrongDirection = 
                        (!mHasLeftContent && flingDirection == DIRECTION_LEFT_TO_RIGHT) ||
                        (!mHasRightContent && flingDirection == DIRECTION_RIGHT_TO_LEFT);
                if (mSidePos == SidePosition.MID && isWrongDirection) return false;

                // check if velocity is big enough to be considered a fling
                if (Math.abs(velocityX) >= getFlingThreshHold()) {
                    mFlingDetected = true;
                    mFlingDirection = flingDirection;
                }

                // fling the scroll so that mid view will fly together with the finger
                mScroller.fling(
                        (int) mCurrentX, 0,
                        (int) -velocityX, 0,
                        getMostLeftX(), getMostRightX(), // TODO: should not use getMostLeftX(), getMostRightX() because left view or right view may be empty
                        0, 0);
                updateTranslationX();

                // return true if a fling is detected
                return mFlingDetected;
            }
        });

        // left view
        mLeftView = generateSubView(context, LEFT_VIEW_ID, 0, mHiddenViewMargin);
        addView(mLeftView);

        // right view
        mRightView = generateSubView(context, RIGHT_VIEW_ID, mHiddenViewMargin, 0);
        addView(mRightView);

        // mid view
        mMidView = generateSubView(context, MID_VIEW_ID, 0, 0);
        mMidView.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        if (shadowDrawableResId > 0) mMidView.setBackgroundResource(shadowDrawableResId);
        addView(mMidView);
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MblUtils.removeOnGlobalLayoutListener(MblSideMenuEnabledLayout.this, this);
                mMidView.getLayoutParams().width = getWidth() + 2 * mShadowPadding;
                mMidView.setPadding(mShadowPadding, 0, mShadowPadding, 0);
                setMidViewMargin(getOriginX());
                mCurrentX = getOriginX();
            }
        });

        // add content to views
        addContent(context, leftContent, mLeftView, LEFT_VIEW_ID);
        addContent(context, midContent, mMidView, MID_VIEW_ID);
        addContent(context, rightContent, mRightView, RIGHT_VIEW_ID);
    }

    private ViewGroup generateSubView(Context context, int id, int leftMargin, int rightMargin) {
        FrameLayout ret = new FrameLayout(context);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.leftMargin = leftMargin;
        lp.rightMargin = rightMargin;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        ret.setLayoutParams(lp);
        ret.setId(id);
        return ret;
    }

    private void addContent(Context context, Object content, ViewGroup view, int viewId) {
        if (content == null) return;
        if (content instanceof View) {
            view.addView((View) content);
        } else if (content instanceof Fragment) {
            FragmentActivity activity = (FragmentActivity) context;
            activity.getSupportFragmentManager()
            .beginTransaction()
            .replace(viewId, (Fragment) content)
            .commit();
        }
    }

    public SidePosition getSidePosition() {
        return mSidePos;
    }

    private void setMidViewMargin(int margin) {
        MarginLayoutParams lp = (MarginLayoutParams) mMidView.getLayoutParams();
        lp.leftMargin = margin;
        mMidView.setLayoutParams(lp);
    }

    private void updateTranslationX() {
        if (mScroller.computeScrollOffset()) {
            setMidViewMargin(mScroller.getCurrX());
        }
        if (!mScroller.isFinished()) {
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    updateTranslationX();
                }
            });
        } else {
            mCurrentX = mScroller.getCurrX();
        }
    }

    private void scrollTo(float toX) {
        mScroller.startScroll((int) mCurrentX, 0, (int)(toX - mCurrentX), 0);
        mCurrentX = toX;
        updateTranslationX();
    }

    public void scrollTo(final SidePosition newPos) {
        if (newPos == SidePosition.LEFT) {
            scrollTo(getMostRightX());
        } else if (newPos == SidePosition.MID) {
            scrollTo(getOriginX());
        } else if (newPos == SidePosition.RIGHT) {
            scrollTo(getMostLeftX());
        }

        boolean isChanged = mSidePos != newPos;
        mSidePos = newPos;
        if (mDelegate != null && isChanged) {
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    mDelegate.handleCurrentSideChange(newPos);
                }
            });
        }
    }

    private void handleFling() {
        if (mSidePos == SidePosition.LEFT) {
            if (mFlingDirection == DIRECTION_RIGHT_TO_LEFT) {
                scrollTo(SidePosition.MID);
                return;
            }
        } else if (mSidePos == SidePosition.MID) {
            if (mFlingDirection == DIRECTION_LEFT_TO_RIGHT) {
                scrollTo(SidePosition.LEFT);
                return;
            } else if (mFlingDirection == DIRECTION_RIGHT_TO_LEFT) {
                scrollTo(SidePosition.RIGHT);
                return;
            }
        } else if (mSidePos == SidePosition.RIGHT) {
            if (mFlingDirection == DIRECTION_LEFT_TO_RIGHT) {
                scrollTo(SidePosition.MID);
                return;
            }
        }
        scrollTo(mSidePos); // scroll back to original position 
    }

    private void handleFinishDragging() {
        float relativeX = mCurrentX - getOriginX();
        if (mSidePos == SidePosition.LEFT) {
            if (relativeX >= 0 && relativeX <= getWidth() / 2) {
                scrollTo(SidePosition.MID);
                return;
            }
        } else if (mSidePos == SidePosition.MID) {
            if (relativeX > getWidth()/2) {
                scrollTo(SidePosition.LEFT);
                return;
            } else if (relativeX < -getWidth()/2) {
                scrollTo(SidePosition.RIGHT);
                return;
            }
        } else if (mSidePos == SidePosition.RIGHT) {
            if (relativeX >= -getWidth()/2 && relativeX <= 0) {
                scrollTo(SidePosition.MID);
                return;
            }
        }
        scrollTo(mSidePos); // scroll back to original position
    }

    private int getFlingThreshHold() {
        return getWidth();
    }

    private boolean shouldHandleTouchEvent(MotionEvent event) {
        if (mDelegate != null && !mDelegate.shouldAllowSwipeToOpenSideView(event)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!shouldHandleTouchEvent(event)) return false;

        boolean handled = mGestureDetector.onTouchEvent(event);
        boolean isDown = event.getAction() == MotionEvent.ACTION_DOWN;
        if (isUpOrCancel(event) && mTapMidViewToCloseSideViewDetected) {
            scrollTo(SidePosition.MID);
        }
        return handled && !isDown;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!shouldHandleTouchEvent(event)) return false;

        mGestureDetector.onTouchEvent(event);
        if (isUpOrCancel(event)) {
            if (mFlingDetected) {
                handleFling();
            } else {
                handleFinishDragging();
            }
            mDraggingHorizontally = false;
        }
        return true;
    }

    private boolean isUpOrCancel(MotionEvent event) {
        return  event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL;
    }

    private int getMidViewWidth() {
        return mMidView.getWidth();
    }

    private int getOriginX() {
        return -mShadowPadding;
    }

    private int getMostLeftX() {
        if (!mHasRightContent) {
            return getOriginX();
        }
        return (mHiddenViewMargin + mShadowPadding) - getMidViewWidth();
    }

    private int getMostRightX() {
        if (!mHasLeftContent) {
            return getOriginX();
        }
        return getWidth() - (mHiddenViewMargin + mShadowPadding);
    }

    public static interface MblSideMenuEnabledLayoutDelegate {
        public boolean shouldAllowSwipeToOpenSideView(MotionEvent event);
        public void handleCurrentSideChange(SidePosition pos);
    }
}
