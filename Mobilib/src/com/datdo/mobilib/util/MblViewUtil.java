package com.datdo.mobilib.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
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
import android.widget.TextView;

import com.datdo.mobilib.base.MblDecorView;
import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblEventListener;
import com.datdo.mobilib.event.MblStrongEventListener;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import com.datdo.mobilib.util.MblLinkMovementMethod.*;

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
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) contentView.getLayoutParams();
            lp.width    = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.height   = sv.getHeight();
            contentView.setLayoutParams(lp);
        }
    }

    /**
     * <pre>
     * Set background image for a screen, and ensure that it won't shrink even when decor view size is changed (keyboard ON/OFF, orientation change, etc)
     * </pre>
     * @param decorView top-level view of screen
     * @param portraitBitmap bitmap data for portrait orientation
     * @param landscapeBitmap bitmap data for landscape orientation
     */
    public static ImageView setBackgroundNoShrinking(
            ViewGroup decorView,
            final Bitmap portraitBitmap,
            final Bitmap landscapeBitmap) {

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

        final EventListeningImageView iv = new EventListeningImageView(decorView.getContext());
        iv.mEventListener = new MblEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                if (MblUtils.isKeyboardOn()) {
                    MblEventCenter.addListener(new MblStrongEventListener() {
                        @Override
                        public void onEvent(Object sender, String name, Object... args) {
                            justifyHeight();
                            terminate();
                        }
                    }, MblCommonEvents.KEYBOARD_HIDDEN);
                    MblUtils.hideKeyboard();
                } else {
                    justifyHeight();
                }
            }

            void justifyHeight() {
                MblUtils.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        fixScrollViewContentHeight(sv);
                        if (MblUtils.isPortraitDisplay()) {
                            iv.setImageBitmap(portraitBitmap);
                        } else {
                            iv.setImageBitmap(landscapeBitmap);
                        }
                    }
                });
            }
        };
        MblEventCenter.addListener(iv.mEventListener, MblCommonEvents.ORIENTATION_CHANGED);
        iv.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        iv.setScaleType(ScaleType.CENTER_CROP);

        sv.addView(iv);
        decorView.addView(sv, 0);
        iv.mEventListener.onEvent(null, null);

        return iv;
    }

    private static class EventListeningImageView extends ImageView {

        MblEventListener mEventListener;

        public EventListeningImageView(Context context) {
            super(context);
        }
    }

    /**
     * <pre>Same as {@link #setBackgroundNoShrinking(ViewGroup, Bitmap, Bitmap)}</pre>
     */
    public static ImageView setBackgroundNoShrinking(ViewGroup decorView, int portraitBgResId, int landscapeBgResId) {
        Drawable portraitDrawable   = MblUtils.getCurrentContext().getResources().getDrawable(portraitBgResId);
        Bitmap portraitBitmap = null;
        if (portraitDrawable instanceof BitmapDrawable) {
            portraitBitmap = ((BitmapDrawable) portraitDrawable).getBitmap();
        }

        Drawable landscapeDrawable  = MblUtils.getCurrentContext().getResources().getDrawable(landscapeBgResId);
        Bitmap landscapeBitmap = null;
        if (landscapeDrawable instanceof BitmapDrawable) {
            landscapeBitmap = ((BitmapDrawable) landscapeDrawable).getBitmap();
        }

        return setBackgroundNoShrinking(decorView, portraitBitmap, landscapeBitmap);
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
     * Same as {@link #makeEditTextAutoScrollOnFocused(View, EditText[], ScrollView)}.
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
     * <pre>Extract {@link String} instance from {@link TextView}</pre>
     */
    public static String extractText(TextView editText) {
        return MblUtils.trim(editText.getText().toString());
    }

    private static MblInterateViewDelegate sGlobalViewProcessor;

    /**
     * Get global view processor set by {@link #setGlobalViewProcessor(MblInterateViewDelegate)}
     */
    public static MblInterateViewDelegate getGlobalViewProcessor() {
        return sGlobalViewProcessor;
    }

    /**
     * <pre>
     * Customize the way to process each view in the activity 's layout.
     * This method is used to apply common processing to views like setting default font, default behaviours, default attributes, etc
     * </pre>
     */
    public static void setGlobalViewProcessor(MblInterateViewDelegate globalViewProcessor) {
        sGlobalViewProcessor = globalViewProcessor;
    }

    /**
     * <pre>
     * Display a string that may contains links (email, web-url, phone) using {@link TextView}. Links are clickable.
     * </pre>
     * @param callback customize how to handle link-clicked and long-clicked
     */
    public static void displayTextWithLinks(TextView textView, String content, final MblLinkMovementMethodCallback callback) {
        String html = MblLinkRecognizer.getLinkRecognizedHtmlText(content);
        textView.setText(Html.fromHtml(html));
        textView.setMovementMethod(new MblLinkMovementMethod(new MblLinkMovementMethodCallback() {
            @Override
            public void onLinkClicked(final String link) {
                if (MblUtils.isEmail(link)) {
                    MblUtils.sendEmail(null, new String[]{link}, null, null, null, null, null);
                } else if (MblUtils.isWebUrl(link)) {
                    MblUtils.openWebUrl(link);
                } else if (MblUtils.isPhone(link)) {
                    if (MblUtils.hasPhone()) {
                        MblUtils.getCurrentContext().startActivity(
                                new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + link)));
                    }
                }
                if (callback != null) {
                    callback.onLinkClicked(link);
                }
            }
            @Override
            public void onLongClicked() {
                if (callback != null) {
                    callback.onLongClicked();
                }
            }
        }));
    }
}
