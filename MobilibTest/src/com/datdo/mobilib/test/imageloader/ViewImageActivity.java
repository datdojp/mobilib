package com.datdo.mobilib.test.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ViewGroup;

import com.datdo.mobilib.api.MblApi;
import com.datdo.mobilib.api.MblApi.MblApiCallback;
import com.datdo.mobilib.api.MblRequest;
import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblUtils;
import com.datdo.mobilib.widget.MblTouchImageView;

public class ViewImageActivity extends MblBaseActivity {

    private MblTouchImageView mImageView;
    private boolean mLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageView = new MblTouchImageView(this);
        mImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mImageView.setOptions(0.5f, 3f, 1f, 0, 0, 0, 0);

        setContentView(mImageView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String link = getIntent().getStringExtra("link");
        if (link != null) {
            MblApi.run(new MblRequest()
                    .setMethod(MblApi.Method.GET)
                    .setUrl(link)
                    .setCacheDuration(Long.MAX_VALUE)
                    .setCallback(new MblApiCallback() {

                        @Override
                        public void onSuccess(int statusCode, byte[] data) {
                            final Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                            if (bm != null) {
                                mImageView.setImageBitmap(bm);
                                mLoaded = true;
                            } else {
                                onFailure(0, null);
                            }
                        }

                        @Override
                        public void onFailure(int error, String errorMessage) {
                            mImageView.setImageResource(R.drawable.error);
                        }
                    }));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLoaded) {
            MblUtils.recycleImageView(mImageView);
        }
    }
}
