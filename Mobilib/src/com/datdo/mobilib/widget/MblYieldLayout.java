package com.datdo.mobilib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.datdo.mobilib.R;
import com.datdo.mobilib.util.MblUtils;

/**
 * <pre>
 * If you are familiar with Web development, you must know concept of "layout".
 * To use layout, you create an HTML file and one "yield" tag where your page 's content will be inserted in.
 * This widget brings Web 's "layout" concept to Android.
 *
 * Firstly, you create a common layout named "common_layout.xml":
 * {@code
 * <LinearLayout
 *      xmlns:android="http://schemas.android.com/apk/res/android"
 *      android:layout_width="match_parent"
 *      android:layout_height="match_parent"
 *      android:orientation="vertical">
 *
 *      <include layout="@layout/header"/>
 *
 *      <LinearLayout
 *          android:id="@+id/yield"
 *          android:layout_width="match_parent"
 *          android:layout_height="match_parent"
 *          android:orientation="vertical"
 *          android:gravity="center_horizontal">
 *
 *      </LinearLayout>
 *
 *      <include layout="@layout/footer"/>
 * </LinearLayout>
 * }
 *
 * Secondly, use common layout in your layout for each screen. Pay attention to "layoutId" and "yieldId":
 * {@code
 * <com.datdo.mobilib.widget.MblYieldLayout
 *      xmlns:android="http://schemas.android.com/apk/res/android"
 *      xmlns:app="http://schemas.android.com/apk/res-auto"
 *      android:layout_width="match_parent"
 *      android:layout_height="match_parent"
 *      app:layoutId="@layout/common_layout"
 *      app:yieldId="@+id/yield">
 *
 *      <!-- Content of this screen-->
 *
 * </com.datdo.mobilib.widget.MblYieldLayout>
 * }
 * </pre>
 */
public class MblYieldLayout extends LinearLayout {

    private int mYieldId;
    private int mLayoutId;

    public MblYieldLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        setOrientation(VERTICAL);

        if (isInEditMode()) {
            return;
        }

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

        if (isInEditMode()) {
            return;
        }

        View[] children = new View[getChildCount()];
        for (int i = 0; i < getChildCount(); i++) {
            children[i] = getChildAt(i);
        }
        View layout = LayoutInflater.from(getContext()).inflate(mLayoutId, null);
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
        addView(layout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }
}
