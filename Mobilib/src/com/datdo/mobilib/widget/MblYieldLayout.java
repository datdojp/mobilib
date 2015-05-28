package com.datdo.mobilib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.datdo.mobilib.R;
import com.datdo.mobilib.util.MblUtils;

/**
 * Created by datdvt on 2015/05/28.
 */
public class MblYieldLayout extends FrameLayout {

    public MblYieldLayout(Context context) {
        super(context);
    }

    public MblYieldLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MblYieldLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MblYieldLayout, 0, 0);
        final int yieldId = ta.getResourceId(R.styleable.MblYieldLayout_yieldId, -1);
        final int layoutId = ta.getResourceId(R.styleable.MblYieldLayout_layoutId, -1);

        if (yieldId < 0 || layoutId < 0) {
            return;
        }

        MblUtils.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {

                if (getChildCount() == 0) {
                    return;
                }

                View child = getChildAt(0);
                View layout = MblUtils.getLayoutInflater().inflate(layoutId, null);
                ViewGroup yieldLayout = (ViewGroup) layout.findViewById(yieldId);

                removeView(child);
                yieldLayout.addView(child);
                layout.setLayoutParams(getLayoutParams());
                addView(layout);
            }
        });
    }
}
