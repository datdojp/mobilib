package com.datdo.mobilib.carrier;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.datdo.mobilib.util.MblUtils;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

/**
 * <pre>
 * An implementation of {@link com.datdo.mobilib.carrier.MblCarrier} with sliding animation when navigatings betweens interceptor.
 * 4 direction: LeftRight, RightLeft, TopBottom, BottomTop.
 * Default direction is LeftRight.
 * The animation works on all Android API >= 10
 * </pre>
 */
@SuppressLint("RtlHardcoded")
public class MblSlidingCarrier extends MblCarrier {

    /**
     * Contructor
     * @see com.datdo.mobilib.carrier.MblCarrier#MblCarrier(android.content.Context, android.widget.FrameLayout, com.datdo.mobilib.carrier.MblCarrier.MblCarrierCallback)
     */
    public MblSlidingCarrier(Context context, FrameLayout interceptorContainerView, MblCarrierCallback callback) {
        super(context, interceptorContainerView, callback);
    }

    /**
     * Sliding direction
     */
    public static enum SlidingDirection {
        LEFT_RIGHT, RIGHT_LEFT,
        TOP_BOTTOM, BOTTOM_TOP;
    }

    private SlidingDirection mSlidingDirection = SlidingDirection.LEFT_RIGHT;

    /**
     * Get direction to animate.
     */
    public SlidingDirection getSlidingDirection() {
        return mSlidingDirection;
    }

    /**
     * Set direction to animate.
     */
    public void setSlidingDirection(SlidingDirection slidingOrientation) {
        Assert.assertNotNull(slidingOrientation);
        mSlidingDirection = slidingOrientation;
    }

    private void animate(
            final MblInterceptor    currentInterceptor,
            final MblInterceptor    nextInterceptor,
            final int               from,
            final int               to,
            final Runnable          onAnimationEnd) {
        
        ValueAnimator anim = ValueAnimator.ofInt(from, to);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator anim) {

                int val = (Integer) anim.getAnimatedValue();

                if (mSlidingDirection == SlidingDirection.LEFT_RIGHT || mSlidingDirection == SlidingDirection.RIGHT_LEFT) {
                    setX(currentInterceptor, val);
                    setX(nextInterceptor, val - to);
                } else if (mSlidingDirection == SlidingDirection.TOP_BOTTOM || mSlidingDirection == SlidingDirection.BOTTOM_TOP) {
                    setY(currentInterceptor, val);
                    setY(nextInterceptor, val - to);
                }

                // prevent blinking
                if (Math.abs(val - from) >= MblUtils.pxFromDp(10)) {
                    nextInterceptor.setVisibility(View.VISIBLE);
                } else {
                    nextInterceptor.setVisibility(View.INVISIBLE);
                }
            }
        });
        anim.addListener(new AnimatorListener() {

            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd.run();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {}

            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimationEnd.run();
            }
        });
        anim.setDuration(300);
        anim.start();
    }
    
    @Override
    protected void animateForStarting(
            final MblInterceptor    currentInterceptor,
            final MblInterceptor    nextInterceptor,
            final Runnable          onAnimationEnd) {

        final int from = 0;
        final int to;
        switch (mSlidingDirection) {
        case LEFT_RIGHT:
            to = mInterceptorContainerView.getWidth();
            break;
        case RIGHT_LEFT:
            to = -mInterceptorContainerView.getWidth();
            break;
        case TOP_BOTTOM:
            to = mInterceptorContainerView.getHeight();
            break;
        case BOTTOM_TOP:
            to = -mInterceptorContainerView.getHeight();
            break;
        default:
            return; // never happen
        }

        animate(currentInterceptor, nextInterceptor, from, to, onAnimationEnd);
    }

    @Override
    protected void animateForFinishing(
            final MblInterceptor currentInterceptor,
            final MblInterceptor previousInterceptor,
            final Runnable onAnimationEnd) {

        final int from = 0;
        final int to;
        switch (mSlidingDirection) {
        case LEFT_RIGHT:
            to = -mInterceptorContainerView.getWidth();
            break;
        case RIGHT_LEFT:
            to = mInterceptorContainerView.getWidth();
            break;
        case TOP_BOTTOM:
            to = -mInterceptorContainerView.getHeight();
            break;
        case BOTTOM_TOP:
            to = mInterceptorContainerView.getHeight();
            break;
        default:
            return; // never happen
        }

        animate(currentInterceptor, previousInterceptor, from, to, onAnimationEnd);
    }

    @SuppressLint("NewApi")
    private void setX(View view, int x) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.setLeft(x);
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
            lp.leftMargin = x;
            lp.rightMargin = -x;
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            view.setLayoutParams(lp);
        }
    }

    @SuppressLint("NewApi")
    private void setY(View view, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.setTop(y);
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
            lp.topMargin = y;
            lp.bottomMargin = -y;
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            view.setLayoutParams(lp);
        }
    }
}
