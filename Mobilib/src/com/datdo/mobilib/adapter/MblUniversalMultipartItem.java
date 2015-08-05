package com.datdo.mobilib.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by datdvt on 2015/07/07.
 */
public class MblUniversalMultipartItem implements MblUniversalItem {

    private List<MblUniversalSinglePartItem> mParts;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;
    private int mSpacing;

    public MblUniversalMultipartItem(List<? extends MblUniversalSinglePartItem> parts) {
        mParts = new ArrayList<>(parts);
    }

    @Override
    public View create(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    @Override
    public void display(View view) {
        // add part-frames if needed
        LinearLayout layout = (LinearLayout) view;
        if (layout.getChildCount() == 0) {
            layout.setWeightSum(mParts.size());
            for (int i = 0; i < mParts.size(); i++) {
                FrameLayout partFrame = new FrameLayout(layout.getContext());
                partFrame.setId(i + 1);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.weight = 1;
                partFrame.setLayoutParams(lp);
                layout.addView(partFrame);
            }
        }

        // display parts
        for (int i = 0; i < mParts.size(); i++) {
            displayPart(view, i + 1, mParts.get(i));
        }

        // set paddings
        view.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
    }

    private void displayPart(View view, int partFrameId, MblUniversalSinglePartItem part) {

        FrameLayout partFrame = (FrameLayout) view.findViewById(partFrameId);
        if (partFrameId > 1 && mSpacing > 0) {
            ((ViewGroup.MarginLayoutParams)partFrame.getLayoutParams()).leftMargin = mSpacing;
        }

        if (part != null) {
            boolean needInflate = partFrame.getChildCount() == 0
                    || partFrame.getTag() == null
                    || part.getClass() != partFrame.getTag().getClass();
            if (needInflate) {
                View partView = part.create(view.getContext());
                partFrame.removeAllViews();
                partFrame.addView(partView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
                part.display(partView);
            } else {
                part.display(partFrame.getChildAt(0));
            }
            partFrame.setTag(part);
        } else {
            partFrame.removeAllViews();
            partFrame.setTag(null);
        }
    }

    public MblUniversalSinglePartItem getPartAt(int index) {
        try {
            return mParts.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean setPartAt(MblUniversalSinglePartItem part, int index) {
        try {
            mParts.set(index, part);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setPaddings(int l, int t, int r, int b) {
        mPaddingLeft    = l;
        mPaddingTop     = t;
        mPaddingRight   = r;
        mPaddingBottom  = b;
    }

    public void setSpacing(int spacing) {
        mSpacing = spacing;
    }
}
