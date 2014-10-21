package com.datdo.mobilib.imageinput;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

class MblAutoResizeSquareImageView extends ImageView {
    public MblAutoResizeSquareImageView(Context context) {
        super(context);
    }

    public MblAutoResizeSquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MblAutoResizeSquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}
