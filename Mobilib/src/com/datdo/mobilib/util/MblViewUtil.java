package com.datdo.mobilib.util;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ScrollView;

import com.datdo.mobilib.base.MblDecorView;
import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblEventListener;

/**
 * <pre>
 * Utility class containing methods designed to deal with common problems/features when processing views
 * </pre>
 */
public class MblViewUtil {

    /**
     * <pre>
     * Traverse view and all its sub-view.
     * </pre>
     * @param view view at top level
     * @param delegate {@link MblInterateViewDelegate}
     */
    public static void iterateView(View view, MblInterateViewDelegate delegate) {
        if (view == null) {
            return;
        }
        if (delegate != null) {
            delegate.process(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int size = vg.getChildCount();
            for (int i = 0; i < size; i++) {
                iterateView(vg.getChildAt(i), delegate);
            }
        }
    }

    /**
     * <pre>
     * Delegate interface to customize the processing for view and sub-view traversed in {@link #iterateView(View, MblInterateViewDelegate)}
     * </pre>
     */
    public static interface MblInterateViewDelegate {
        public void process(View view);
    }

    /**
     * <pre>
     * We sometimes encounter a problem like this: ScrollView contains its content View, and we want the content View always keep its height equal to original ScrollView 's height even when ScrollView 's height is changed (for example: keyboard ON/OFF)
     * Use this method to fix content View 's height always equal to original ScrollView 's height.
     * A popular case where this method should be used is when you need to set background image for activity, but you don't want background image to shrink when keyboard is ON.
     * In that case, just put an ImageView inside a ScrollView and call <code>fixScrollViewContentHeight(scrollView)</code>
     * </pre>
     * @param sv
     */
    public static void fixScrollViewContentHeight(final ScrollView sv) {

        if (sv == null || sv.getChildCount() == 0) {
            return;
        }

        final int WRAPPING_VIEW_ID = 1427684961;

        if (sv.getHeight() == 0) {
            sv.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    MblUtils.removeOnGlobalLayoutListener(sv, this);
                    fixScrollViewContentHeight(sv);
                }
            });
            return;
        }

        if (sv.getChildAt(0).getId() != WRAPPING_VIEW_ID) {

            // get content view, then remove it from ScrollView
            View contentView = sv.getChildAt(0);
            if (contentView == null) {
                return;
            }
            sv.removeAllViews();

            // create a wrapping view
            FrameLayout wrappingView = new FrameLayout(sv.getContext());
            wrappingView.setLayoutParams(new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT));
            wrappingView.setId(WRAPPING_VIEW_ID);
            sv.addView(wrappingView);

            // set content view 's height and attach it to wrapping view
            contentView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    sv.getHeight()));
            wrappingView.addView(contentView);
        } else{
            FrameLayout wrappingView = (FrameLayout) sv.getChildAt(0);
            View contentView = wrappingView.getChildAt(0);
            contentView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    sv.getHeight()));
        }
    }

    /**
     * <pre>
     * Set background image for a screen, and ensure that it won't shrink even when decor view size is changed (keyboard ON/OFF, orientation change, etc)
     * </pre>
     * @param decorView top-level view of screen
     * @param bm bitmap data
     * @return ImageView object used to display background
     */
    public static ImageView setBackgroundNoShrinking(ViewGroup decorView, Bitmap bm) {

        Assert.assertNotNull(decorView);

        final ScrollView sv = new ScrollView(decorView.getContext()) {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                return false;
            }
            
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return false;
            }
        };
        sv.setLayoutParams(new MblDecorView.LayoutParams(
                MblDecorView.LayoutParams.MATCH_PARENT,
                MblDecorView.LayoutParams.MATCH_PARENT));

        EventListeningImageView iv = new EventListeningImageView(decorView.getContext());
        MblEventCenter.addListener((iv.mEventListener = new MblEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                fixScrollViewContentHeight(sv);
            }
        }), MblCommonEvents.ORIENTATION_CHANGED);
        iv.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        iv.setScaleType(ScaleType.CENTER_CROP);
        iv.setImageBitmap(bm);

        sv.addView(iv);
        decorView.addView(sv, 0);
        fixScrollViewContentHeight(sv);

        return iv;
    }

    private static class EventListeningImageView extends ImageView {

        @SuppressWarnings("unused")
        MblEventListener mEventListener;

        public EventListeningImageView(Context context) {
            super(context);
        }
    }

    /**
     * <pre>Same as {@link #setBackgroundNoShrinking(ViewGroup, Bitmap)}</pre>
     */
    public static ImageView setBackgroundNoShrinking(ViewGroup decorView, int bgResId) {
        Drawable drawable = MblUtils.getCurrentContext().getResources().getDrawable(bgResId);
        if (drawable instanceof BitmapDrawable) {
            return setBackgroundNoShrinking(decorView, ((BitmapDrawable) drawable).getBitmap());
        } else {
            return setBackgroundNoShrinking(decorView, null);
        }
    }

    /**
     * <pre>
     * Android keyboard sometimes overlaps the {@link EditText}, especially when it supports text prediction feature.
     * Also, {@link EditText} sometimes doesn't scroll long enough to be displayed when keyboard is ON, which makes it partially hidden.
     * This method makes EditText always scroll to best position so that user can see it clearly and fully.
     * </pre>
     * @param decoreView top-level view of screen
     * @param editTexts array of {@link EditText} to enable auto-scroll
     * @param parentScrollView parent {@link ScrollView} that contains {@link EditText} in <code>editTexts</code>
     */
    public static void makeEditTextAutoScrollOnFocused(
            final View          decoreView,
            final EditText[]    editTexts,
            final ScrollView    parentScrollView) {

        Assert.assertNotNull(decoreView);
        Assert.assertNotNull(editTexts);
        Assert.assertNotNull(parentScrollView);

        final Runnable action = new Runnable() {
            @Override
            public void run() {

                // if keyboard is not ON, we have nothing to do 
                if (!MblUtils.isKeyboardOn()) {
                    return;
                }

                // find the EditText being focused
                View focusedView = ((Activity)decoreView.getContext()).getCurrentFocus();
                EditText focusedEditText = null;
                for (EditText et : editTexts) {
                    if (et == focusedView) {
                        focusedEditText = et;
                    }
                }

                // force ScrollView scroll so that focused EditText is centered
                if (focusedEditText != null) {
                    int[] editTextLocation = new int[2];
                    focusedEditText.getLocationOnScreen(editTextLocation);
                    int[] decorViewLocation = new int[2];
                    decoreView.getLocationOnScreen(decorViewLocation);
                    int editTextYPos = editTextLocation[1] - decorViewLocation[1];

                    final int dy = (editTextYPos + focusedEditText.getHeight() / 2) - decoreView.getHeight() / 2;

                    parentScrollView.smoothScrollBy(0, dy);
                }
            }
        };

        parentScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                action.run();
            }
        });

        for (EditText et : editTexts) {
            et.setOnFocusChangeListener(new OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {

                    if (!hasFocus) {
                        return;
                    }

                    action.run();
                }
            });
        };
    }

    /**
     * <pre>
     * Same as {@link #makeEditTextAutoScrollOnFocused(MblDecorView, EditText[], ScrollView)}.
     * <code>editTexts</code> is omitted. {@link EditText} objects are collected automatically by traversing view.
     * </pre>
     */
    public static void makeEditTextAutoScrollOnFocused(
            final View          decoreView,
            final ScrollView    parentScrollView) {

        final List<EditText> editTexts = new ArrayList<EditText>();
        iterateView(parentScrollView, new MblInterateViewDelegate() {
            @Override
            public void process(View view) {
                if (view instanceof EditText) {
                    editTexts.add((EditText) view);
                }
            }
        });

        makeEditTextAutoScrollOnFocused(
                decoreView,
                editTexts.toArray(new EditText[editTexts.size()]),
                parentScrollView);
    }

    /**
     * <pre>Extract {@link String} instace from {@link EditText}</pre>
     * @param editText
     * @return
     */
    public static String extractText(EditText editText) {
        return MblUtils.trim(editText.getText().toString());
    }
}
