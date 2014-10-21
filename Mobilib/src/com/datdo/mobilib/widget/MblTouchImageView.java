package com.datdo.mobilib.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class MblTouchImageView extends ImageView {

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final int CLICK = 3;

    private Matrix mMatrix;
    private int mMode = NONE;
    private PointF mLast = new PointF();
    private PointF mStart = new PointF();
    private float mMinScale = 1f;
    private float mMaxScale = 3f;
    private float[] mMatrixValues;
    private float mCurrentScale = 1f;
    private float mOriginWidth, mOriginHeight;
    private int mLeftDragPadding;
    private int mTopDragPadding;
    private int mRightDragPadding;
    private int mBottomDragPadding;
    private ScaleGestureDetector mScaleDetector;
    private OnTouchListener mExtraTouchListener;

    public MblTouchImageView(Context context) {
        super(context);
        init(context);
    }

    public MblTouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MblTouchImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        super.setClickable(true);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mMatrix = new Matrix();
        mMatrixValues = new float[9];
        super.setImageMatrix(mMatrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (mExtraTouchListener != null) mExtraTouchListener.onTouch(v, event);

                mScaleDetector.onTouchEvent(event);
                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLast.set(curr);
                    mStart.set(mLast);
                    mMode = DRAG;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mMode == DRAG) {
                        float deltaX = curr.x - mLast.x;
                        float deltaY = curr.y - mLast.y;
                        if (hasDragPaddings()) {
                            mMatrix.postTranslate(deltaX, deltaY);
                        } else {
                            float fixTransX = getFixDragTranslation(deltaX, getWidth(), mOriginWidth * mCurrentScale);
                            float fixTransY = getFixDragTranslation(deltaY, getHeight(), mOriginHeight * mCurrentScale);
                            mMatrix.postTranslate(fixTransX, fixTransY);
                        }
                        fixTranslations();
                        mLast.set(curr.x, curr.y);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mMode = NONE;
                    int xDiff = (int) Math.abs(curr.x - mStart.x);
                    int yDiff = (int) Math.abs(curr.y - mStart.y);
                    if (xDiff < CLICK && yDiff < CLICK)
                        performClick();
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    mMode = NONE;
                    break;
                }

                MblTouchImageView.super.setImageMatrix(mMatrix);
                invalidate();
                return true; // indicate event was handled
            }
        });

        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                applyOptionsIfReady();
            }
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mMode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = mCurrentScale;
            mCurrentScale *= mScaleFactor;
            if (mCurrentScale > mMaxScale) {
                mCurrentScale = mMaxScale;
                mScaleFactor = mMaxScale / origScale;
            } else if (mCurrentScale < mMinScale) {
                mCurrentScale = mMinScale;
                mScaleFactor = mMinScale / origScale;
            }

            if (mOriginWidth * mCurrentScale <= getWidth() || mOriginHeight * mCurrentScale <= getHeight())
                mMatrix.postScale(mScaleFactor, mScaleFactor, getWidth() / 2, getHeight() / 2);
            else
                mMatrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());

            fixTranslations();
            return true;
        }
    }

    private void fixTranslations() {
        mMatrix.getValues(mMatrixValues);
        float transX = mMatrixValues[Matrix.MTRANS_X];
        float transY = mMatrixValues[Matrix.MTRANS_Y];

        float fixTransX = getFixTranslation(transX, getWidth(), mOriginWidth * mCurrentScale, mLeftDragPadding, mRightDragPadding);
        float fixTransY = getFixTranslation(transY, getHeight(), mOriginHeight * mCurrentScale, mTopDragPadding, mBottomDragPadding);

        if (fixTransX != 0 || fixTransY != 0) {
            mMatrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getFixTranslation(float trans, float viewSize, float contentSize, int dragPaddingFrom, int dragPaddingTo) {

        float minTrans, maxTrans;

        if (hasDragPaddings()) {
            minTrans = (viewSize - contentSize) - dragPaddingFrom;
            maxTrans = dragPaddingTo;
        } else {
            if (contentSize <= viewSize) {
                minTrans = 0;
                maxTrans = viewSize - contentSize;
            } else {
                minTrans = viewSize - contentSize;
                maxTrans = 0;
            }
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }

    private float getFixDragTranslation(float delta, float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return 0;
        }
        return delta;
    }

    private boolean hasDragPaddings() {
        return
                mLeftDragPadding != 0 ||
                mTopDragPadding != 0 ||
                mRightDragPadding != 0 ||
                mBottomDragPadding != 0;
    }

    public float[] getMatrixValues() {
        float[] ret = new float[9];
        mMatrix.getValues(ret);
        return ret;
    }

    public void setOptions(
            float minScale,
            float maxScale,
            float currentScale,
            int leftDragPadding,
            int topDragPadding,
            int rightDragPadding,
            int bottomDragPadding) {
        // save min zoom, max zoom and current zoom
        mMinScale = minScale;
        mMaxScale = maxScale;
        mCurrentScale = currentScale;

        // save drag padding
        mLeftDragPadding = leftDragPadding;
        mTopDragPadding = topDragPadding;
        mRightDragPadding = rightDragPadding;
        mBottomDragPadding = bottomDragPadding;

        // config if ready
        applyOptionsIfReady();
    }

    private void applyOptionsIfReady() {
        if (isReady()) {
            applyOptions();
        }
    }

    private void applyOptions() {
        // get bitmap sizes
        Drawable drawable = getDrawable();
        int bmWidth = drawable.getIntrinsicWidth();
        int bmHeight = drawable.getIntrinsicHeight();

        // create new matrix
        mMatrix = new Matrix();
        mMatrix.postScale(mCurrentScale, mCurrentScale, 0, 0);

        // save original sizes
        mOriginWidth = bmWidth;
        mOriginHeight = bmHeight;

        // center the image
        float redundantXSpace = (getWidth() - mCurrentScale * bmWidth) / 2;
        float redundantYSpace = (getHeight() - mCurrentScale * bmHeight) / 2;
        mMatrix.postTranslate(redundantXSpace, redundantYSpace);

        // transform image
        super.setImageMatrix(mMatrix);
        fixTranslations();
    }

    private boolean isReady() {
        Drawable drawable;
        return
                getWidth() > 0 &&
                getHeight() > 0 &&
                (drawable = getDrawable()) != null &&
                drawable.getIntrinsicWidth() > 0 &&
                drawable.getIntrinsicHeight() > 0;
    }

    public void setExtraTouchListener(OnTouchListener extraTouchListener) {
        mExtraTouchListener = extraTouchListener;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        // only accept matrix scaletype
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType);
        }
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        // not allow
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        applyOptionsIfReady();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        applyOptionsIfReady();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        applyOptionsIfReady();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        applyOptionsIfReady();
    }
}