package com.datdo.mobilib.widget;

import com.datdo.mobilib.util.MblUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class MblHorizontalViewPager extends HorizontalScrollView {
    private static final float SCROLL_X_PER_Y_RATE_THRESHHOLD = 1.5f;

    private static final int DIRECTION_LEFT_TO_RIGHT = -1;
    private static final int DIRECTION_RIGHT_TO_LEFT = 1;

    private ViewGroup mContainerLayout;
    private int mNumberOfPages;
    private GestureDetector mGestureDetector;
    private boolean mFlingDetected;
    private int mFlingDirection;
    private int mCurrentScrollX;
    private int mCurrentIndex;
    private boolean mDraggingHorizontally;
    private MblHorizontalViewPagerCallback mCallback;
    private int mDividerWidth;

    public MblHorizontalViewPager(Context context) {
        super(context);
    }

    public MblHorizontalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MblHorizontalViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(
            final Context context,
            final View[] pages,
            final int dividerWidth,
            final int dividerColor,
            MblHorizontalViewPagerCallback callback) {
        mCallback = callback;
        
        // setup
        setHorizontalScrollBarEnabled(false);
        setSmoothScrollingEnabled(true);
        
        // create container layout (horizontal linearlayout)
        if (mContainerLayout == null) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            addView(linearLayout);
            mContainerLayout = linearLayout;
        } else {
            mContainerLayout.removeAllViews();
        }

        // add views
        mDividerWidth = dividerWidth;
        mContainerLayout.removeAllViews();
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MblUtils.removeOnGlobalLayoutListener(MblHorizontalViewPager.this, this);
                for (int i = 0; i < pages.length; i++) {
                    // add separator
                    if (i > 0 && dividerWidth > 0) {
                        View divider = new View(context);
                        divider.setLayoutParams(new ViewGroup.LayoutParams(dividerWidth, ViewGroup.LayoutParams.MATCH_PARENT));
                        divider.setBackgroundColor(dividerColor);
                        mContainerLayout.addView(divider);
                    }
                    View p = pages[i];
                    ViewGroup containerView = generateContainerView(context);
                    containerView.addView(p);
                    mContainerLayout.addView(containerView);
                }
                mNumberOfPages = pages.length;
            }
        });

        // gesture detector
        mGestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                mFlingDetected = false;
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float rate = distanceY != 0 ? distanceX/distanceY : SCROLL_X_PER_Y_RATE_THRESHHOLD;
                if (Math.abs(rate) >= SCROLL_X_PER_Y_RATE_THRESHHOLD) {
                    mDraggingHorizontally = true;
                }
                if (mDraggingHorizontally) {
                    float toX = mCurrentScrollX + distanceX;
                    toX = Math.max(getMostLeftX(), toX);
                    toX = Math.min(toX, getMostRightX());
                    scrollTo((int) toX);
                }

                // return true if a horizontal dragging is detected
                return mDraggingHorizontally;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // if left content or right content is empty, forbid flinging to that direction
                int flingDirection = velocityX > 0 ? DIRECTION_LEFT_TO_RIGHT : DIRECTION_RIGHT_TO_LEFT;

                // check if velocity is big enough to be considered a fling
                if (Math.abs(velocityX) >= getFlingThreshHold()) {
                    mFlingDetected = true;
                    mFlingDirection = flingDirection;
                }

                // return true if a fling is detected
                return mFlingDetected;
            }
        });

        // scroll to origin
        mCurrentIndex = -1;
        scrollToIndex(0);
    }

    public void scrollTo(int x) {
        mCurrentScrollX = x;
        smoothScrollTo(x, 0); // TODO: not smooth enough. use Scroller to make it smoother
    }

    public void scrollToIndex(final int index) {
        scrollTo(getXForIndex(index));
        if (index != mCurrentIndex) {
            if (mCallback != null) {
                MblUtils.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onIndexChanged(mCurrentIndex, index);
                    }
                });
            }
            mCurrentIndex = index;
        }
    }

    // generate view that contains one page
    private ViewGroup generateContainerView(Context context) {
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(
                new ViewGroup.LayoutParams(
                        getPageWidth(),
                        ViewGroup.LayoutParams.MATCH_PARENT));
        return container;
    }

    private int getFlingThreshHold() {
        return getPageWidth()/2;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean handled = mGestureDetector.onTouchEvent(event);
        boolean isDown = event.getAction() == MotionEvent.ACTION_DOWN;
        return handled && !isDown;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        boolean isUp = event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL;
        if (isUp) {
            if (mFlingDetected) {
                handleFling();
            } else {
                handleFinishDragging();
            }
            mDraggingHorizontally = false;
        }
        return true;
    }

    private void handleFling() {
        // get new index
        int newIndex = mCurrentIndex;
        if (mFlingDirection == DIRECTION_LEFT_TO_RIGHT) {
            newIndex--;
        } else if (mFlingDirection == DIRECTION_RIGHT_TO_LEFT) {
            newIndex++;
        }
        newIndex = Math.max(0, newIndex);
        newIndex = Math.min(newIndex, mNumberOfPages-1);

        // scroll to new index
        if (newIndex != mCurrentIndex) {
            scrollToIndex(newIndex);
        }
    }

    private void handleFinishDragging() {
        // get deltaX and direction
        int dx = mCurrentScrollX - getXForIndex(mCurrentIndex);
        int direction = dx > 0 ? DIRECTION_RIGHT_TO_LEFT : DIRECTION_LEFT_TO_RIGHT;

        // get new index
        int newIndex = mCurrentIndex;
        if (Math.abs(dx) > getPageWidth()/2) {
            if (direction == DIRECTION_LEFT_TO_RIGHT) {
                newIndex--;
            } else if (direction == DIRECTION_RIGHT_TO_LEFT) {
                newIndex++;
            }
        }
        newIndex = Math.max(0, newIndex);
        newIndex = Math.min(newIndex, mNumberOfPages-1);

        if (newIndex != mCurrentIndex) { // scroll to new index
            scrollToIndex(newIndex);
        } else {
            scrollToIndex(mCurrentIndex); // or roll back
        }
    }

    public static interface MblHorizontalViewPagerCallback {
        public void onIndexChanged(int oldIndex, int newIndex);
    }
    
    private int getMostLeftX() {
        return 0;
    }
    
    private int getMostRightX() {
        return mContainerLayout.getWidth() - getPageWidth();
    }
    
    private int getPageWidth() {
        return getWidth();
    }
    
    private int getXForIndex(int index) {
        return index * (getPageWidth() + mDividerWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        MblUtils.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mContainerLayout != null) {
                    for(int i = 0; i < mContainerLayout.getChildCount(); i++) {
                        View child = mContainerLayout.getChildAt(i);
                        if (child instanceof FrameLayout) {
                            ViewGroup.LayoutParams lp = child.getLayoutParams();
                            lp.width = getPageWidth();
                            child.setLayoutParams(lp);
                        }
                    }
                }

                requestLayout();

                MblUtils.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        scrollToIndex(mCurrentIndex);
                    }
                });
            }
        });
    }

}
