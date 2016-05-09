package com.datdo.mobilib.test.imageloader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblUtils;
import com.datdo.mobilib.v2.image.ImageLoader;
import com.datdo.mobilib.v2.image.ImageTool;
import com.datdo.mobilib.widget.MblTouchImageView;

public class ViewImageActivity extends MblBaseActivity {

    private MblTouchImageView mImageView;
    private boolean mLoaded;
    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageView = new MblTouchImageView(this);
        mImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mImageView.setOptions(0.5f, 3f, 1f, 0, 0, 0, 0);

        setContentView(mImageView);

        imageLoader = new ImageLoader(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String link = getIntent().getStringExtra("link");
        if (link != null) {
            imageLoader.forOneImageView(this)
                    .load(link)
                    .error(R.drawable.error)
                    .enableProgressView(true)
                    .callback(new ImageLoader.Callback() {
                        @Override
                        public void onSuccess(ImageView image, Bitmap bm, ImageLoader.LoadRequest request) {
                            mLoaded = true;
                        }

                        @Override
                        public void onError(Throwable t, ImageView image, ImageLoader.LoadRequest request) {}
                    })
                    .into(mImageView);
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
