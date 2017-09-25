package com.datdo.mobilib.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.datdo.mobilib.R;
import com.datdo.mobilib.util.MblUtils;

/**
 * Created by dat on 2016/02/18.
 */
public class MblPagerIndicator extends LinearLayout {

    private int numberOfDots;
    private int dotSize;
    private int dotSpacing;
    private int defaultIndex;
    private int dotIconResourceID;

    private int currentIndex;

    public MblPagerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MblPagerIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public MblPagerIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        // ignore edit mode
        if (isInEditMode()) {
            return;
        }

        // extract attribute
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MblPagerIndicator, 0, 0);
        numberOfDots        = ta.getInt(R.styleable.MblPagerIndicator_numberOfDots,     0);
        dotSize             = ta.getInt(R.styleable.MblPagerIndicator_dotSize,          MblUtils.pxFromDp(7));
        dotSpacing          = ta.getInt(R.styleable.MblPagerIndicator_dotSpacing,       MblUtils.pxFromDp(10));
        defaultIndex        = ta.getInt(R.styleable.MblPagerIndicator_defaultIndex,     0);
        dotIconResourceID   = ta.getInt(R.styleable.MblPagerIndicator_dotIcon,          R.drawable.states_pager_indicator_dot);

        // add dots to layout
        super.setOrientation(HORIZONTAL);
        super.setGravity(Gravity.CENTER);
        createDots();

        // default index
        setCurrentIndex(defaultIndex);
    }

    private void createDots() {
        removeAllViews();
        for (int i = 0; i < numberOfDots; i++) {
            View dot = new View(getContext());
            LayoutParams lp = new LayoutParams(dotSize, dotSize);
            if (i > 0) {
                lp.leftMargin = dotSpacing;
            }
            dot.setLayoutParams(lp);
            MblUtils.setBackgroundDrawable(dot, ContextCompat.getDrawable(getContext(), dotIconResourceID));
            addView(dot);
        }
    }

    private void updateDotColor() {
        for (int i = 0; i < getChildCount(); i++) {
            View dot = getChildAt(i);
            dot.setEnabled(i == currentIndex);
        }
    }

    public void setCurrentIndex(int index) {
        if (index < 0 || index > getChildCount()) {
            return;
        }
        currentIndex = index;
        updateDotColor();
    }

    public void setNumberOfDots(int numberOfDots) {
        this.numberOfDots = numberOfDots;
        createDots();
        updateDotColor();
    }

    @Override
    public void setOrientation(int orientation) {
        throw new RuntimeException(MblPagerIndicator.class + ": `setOrientation` is not allowed for this custom view");
    }
}
