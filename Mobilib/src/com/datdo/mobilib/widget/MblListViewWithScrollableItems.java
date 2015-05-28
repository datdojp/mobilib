package com.datdo.mobilib.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

@Deprecated
public class MblListViewWithScrollableItems extends ListView {
    private Delegate mDelegate;

    public MblListViewWithScrollableItems(Context context) {
        super(context);
    }

    public MblListViewWithScrollableItems(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MblListViewWithScrollableItems(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mDelegate != null && mDelegate.shouldNOTInterceptTouchEvent(event)) {
            return false;
        }
        return super.onInterceptTouchEvent(event);
    };
    
    
    
    public static interface Delegate {
        public boolean shouldNOTInterceptTouchEvent(MotionEvent event);
    }

    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }
}
