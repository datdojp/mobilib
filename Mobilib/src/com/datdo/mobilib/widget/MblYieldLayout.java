package com.datdo.mobilib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.datdo.mobilib.R;
import com.datdo.mobilib.util.MblUtils;

/**
 * Created by datdvt on 2015/05/28.
 */
public class MblYieldLayout extends LinearLayout {

    private int mYieldId;
    private int mLayoutId;

    public MblYieldLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MblYieldLayout, 0, 0);
        mYieldId = ta.getResourceId(R.styleable.MblYieldLayout_yieldId, -1);
        mLayoutId = ta.getResourceId(R.styleable.MblYieldLayout_layoutId, -1);
        if (mYieldId < 0 || mLayoutId < 0) {
            throw new RuntimeException(MblYieldLayout.class + ": layoutId or yieldId is missing");
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View[] children = new View[getChildCount()];
        for (int i = 0; i < getChildCount(); i++) {
            children[i] = getChildAt(i);
        }
        View layout = MblUtils.getLayoutInflater().inflate(mLayoutId, null);
        if (layout == null) {
            throw new RuntimeException(MblYieldLayout.class + ": layoutId not found");
        }
        ViewGroup yieldLayout = (ViewGroup) layout.findViewById(mYieldId);
        if (yieldLayout == null) {
            throw new RuntimeException(MblYieldLayout.class + ": yieldId not found");
        }

        for (View child : children) {
            removeView(child);
            yieldLayout.addView(child);
        }
        addView(layout);
    }
}
