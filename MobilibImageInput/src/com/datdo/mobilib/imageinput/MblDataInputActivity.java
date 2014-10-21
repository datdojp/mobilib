package com.datdo.mobilib.imageinput;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.datdo.mobilib.base.MblBaseFragmentActivity;
import com.datdo.mobilib.util.MblUtils;

class MblDataInputActivity extends MblBaseFragmentActivity {

    private static final String EXTRA_CALLBACK = "callback";
    private static final String EXTRA_DELEGATE = "delegate";

    private CmDataInputActivityCallback mCallback;
    private CmDataInputActivityDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String callbackKey = getIntent().getExtras().getString(EXTRA_CALLBACK);
        mCallback = (CmDataInputActivityCallback) MblUtils.removeFromCommonBundle(callbackKey);

        String delegateKey = getIntent().getExtras().getString(EXTRA_DELEGATE);
        mDelegate = (CmDataInputActivityDelegate) MblUtils.removeFromCommonBundle(delegateKey);
    }

    protected void cancelInput() {
        if (mCallback != null) {
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCancel();
                }
            });
        }
        finish();
    }

    protected void finishInput(final Object... outputData) {
        if (mDelegate != null && !mDelegate.checkBeforeFinish(this, outputData)) {
            return;
        }
        if (mCallback != null) {
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.onFinish(outputData);
                }
            });
        }
        finish();
    }

    public static interface CmDataInputActivityCallback {
        public void onFinish(Object... outputData);
        public void onCancel();
    }

    public static interface CmDataInputActivityDelegate {
        public boolean checkBeforeFinish(MblDataInputActivity me, Object...outputData);
    }

    @SuppressWarnings("rawtypes")
    protected static Intent createIntent(Class clazz, CmDataInputActivityCallback callback, CmDataInputActivityDelegate delegate) {
        Context context = MblUtils.getCurrentContext();
        Intent intent = new Intent(context, clazz);

        if (callback != null) {
            intent.putExtra(EXTRA_CALLBACK, MblUtils.putToCommonBundle(callback));
        }

        if (delegate != null) {
            intent.putExtra(EXTRA_DELEGATE, MblUtils.putToCommonBundle(delegate));
        }

        return intent;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelInput();
    }
}
