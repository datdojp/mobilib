package com.datdo.mobilib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.datdo.mobilib.R;
import com.datdo.mobilib.util.MblUtils;

/**
 * Created by datdvt on 2015/05/26.
 */
public class MblFlexibleSizedImageView extends ImageView {

    public static enum FlexibleSize {
        WIDTH, HEIGHT;
    }

    private FlexibleSize    mFlexibleSize = FlexibleSize.HEIGHT;
    private Bitmap          mBitmap;
    private int minWidth;
    private int maxWidth;
    private int minHeight;
    private int maxHeight;

    public MblFlexibleSizedImageView(Context context) {
        super(context);
    }

    public MblFlexibleSizedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
    }

    public MblFlexibleSizedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
    }

    private void initAttr(Context context, AttributeSet attrs) {

        // get custom attributes
        TypedArray customAttrs = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MblFlexibleSizedImageView, 0, 0);
        int flexibleIndex = customAttrs.getInt(R.styleable.MblFlexibleSizedImageView_flexibleSize, -1);
        if (flexibleIndex >= 0) {
            mFlexibleSize = FlexibleSize.values()[flexibleIndex];
        }

        // get android attributes
        TypedArray androidAttrs = context.obtainStyledAttributes(attrs, new int[]{
                android.R.attr.minWidth,
                android.R.attr.maxWidth,
                android.R.attr.minHeight,
                android.R.attr.maxHeight
        });
        minWidth    = androidAttrs.getInt(0, -1);
        maxWidth    = androidAttrs.getInt(1, -1);
        minHeight   = androidAttrs.getInt(2, -1);
        maxHeight   = androidAttrs.getInt(3, -1);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable != null && drawable instanceof BitmapDrawable) {
            expandSize(((BitmapDrawable) drawable).getBitmap());
        }
        super.setImageDrawable(drawable);
    }

    private void expandSize(final Bitmap bm) {
        if (getWidth() == 0 || getHeight() == 0) {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    MblUtils.removeOnGlobalLayoutListener(MblFlexibleSizedImageView.this, this);
                    setImageBitmap(bm);
                }
            });
            return;
        }
        if (bm == null || bm.isRecycled() || bm.getWidth() == 0 || bm.getHeight() == 0) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (mFlexibleSize == FlexibleSize.WIDTH) {
            w = Math.round(h * bm.getWidth() / bm.getHeight());
            if (minWidth >= 0) {
                w = Math.max(minWidth, w);
            }
            if (maxWidth >= 0) {
                w = Math.min(w, maxWidth);
            }
        } else if (mFlexibleSize == FlexibleSize.HEIGHT) {
            h = Math.round(w * bm.getHeight() / bm.getWidth());
            if (minHeight >= 0) {
                h = Math.max(minHeight, h);
            }
            if (maxHeight >= 0) {
                h = Math.min(h, maxHeight);
            }
        }
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) {
            if (mFlexibleSize == FlexibleSize.WIDTH && w != lp.width) {
                lp.width = w;
            } else if (mFlexibleSize == FlexibleSize.HEIGHT && h != lp.height) {
                lp.height = h;
            }
        }
        mBitmap = bm;
    }

    public FlexibleSize getFlexibleSize() {
        return mFlexibleSize;
    }

    public void setFlexibleSize(FlexibleSize flexibleSize) {
        if (mFlexibleSize != flexibleSize) {
            mFlexibleSize = flexibleSize;
            if (mBitmap != null) {
                setImageBitmap(mBitmap);
            }
        }
    }
}
