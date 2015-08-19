package com.datdo.mobilib.util;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * <pre>
 * A movement method that detects links in {@link TextView}.
 * Also support callback methods when link is clicked.
 * This class is used together with {@link MblLinkRecognizer}
 * </pre>
 */
public class MblLinkMovementMethod extends LinkMovementMethod {
    private static final long LONG_CLICK_DELAY = 500;
    private boolean mLongClickDetected = false;
    private MblLinkMovementMethodCallback mCallback;

    public static interface MblLinkMovementMethodCallback {
        /**
         * <pre>
         * Invoked when link is clicked.
         * </pre>
         */
        public void onLinkClicked(String link);
        /**
         * <pre>
         * Invoked when {@link TextView} is long-clicked.
         * </pre>
         */
        public void onLongClicked();
    }

    public MblLinkMovementMethod(MblLinkMovementMethodCallback callback) {
        super();
        mCallback = callback;
    }

    private Runnable mOnLongClickAction = new Runnable() {
        @Override
        public void run() {
            mLongClickDetected = true;
        }
    };

    // return true --> should handle event as long click
    private boolean listenLongClick(MotionEvent event) {
        if (mLongClickDetected) {
            if ( event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP ) {
                mLongClickDetected = false; // this is the end of long click
            }
            if (mCallback != null) {
                mCallback.onLongClicked();
            }
            return true;
        } else {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                MblUtils.getMainThreadHandler().postDelayed(mOnLongClickAction, LONG_CLICK_DELAY);
            } else if ( event.getAction() == MotionEvent.ACTION_CANCEL ||
                    event.getAction() == MotionEvent.ACTION_UP ) {
                MblUtils.getMainThreadHandler().removeCallbacks(mOnLongClickAction);
            }
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
        if (!listenLongClick(event)) {
            if (action == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                if (link.length != 0) {
                    String linkString = buffer.toString().substring(buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
                    if (mCallback != null) {
                        mCallback.onLinkClicked(linkString);
                    }
                    Selection.removeSelection(buffer);
                    return false;
                }
            }
        }
        return false;
    }
}
