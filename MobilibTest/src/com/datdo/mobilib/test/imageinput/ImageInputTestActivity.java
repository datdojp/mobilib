package com.datdo.mobilib.test.imageinput;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.imageinput.MblImageInput;
import com.datdo.mobilib.imageinput.MblPickImageActivity;
import com.datdo.mobilib.imageinput.MblPickImageActivity.MblPickImageCallback;
import com.datdo.mobilib.imageinput.MblTakeImageActivity;
import com.datdo.mobilib.imageinput.MblTakeImageActivity.MblTakeImageCallback;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblUtils;
import com.datdo.mobilib.v2.image.ImageLoader;

public class ImageInputTestActivity extends MblBaseActivity {

    private ThumbnailAdapter mAdapter;
    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_input_test);

        MblImageInput.configure(null, null, null, 0.1f, 3f);

        findViewById(R.id.take_image_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                takeImage();
            }
        });

        findViewById(R.id.pick_image_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        findViewById(R.id.take_crop_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                takeAndCropImage();
            }
        });

        findViewById(R.id.pick_crop_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pickAndCropImage();
            }
        });
        
        ListView listview = (ListView) findViewById(R.id.thumbnail_list);
        imageLoader = new ImageLoader(this);
        mAdapter = new ThumbnailAdapter(this, imageLoader);
        listview.setAdapter(mAdapter);
    }

    private void takeImage() {
        MblTakeImageActivity.start(this, null, -1, -1, new MblTakeImageCallback() {

            @Override
            public void onFinish(String path) {
                mAdapter.changeData(new String[] { path });
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }

    private void pickImage() {
        MblPickImageActivity.start(this, 5, -1, -1, new MblPickImageCallback() {

            @Override
            public void onFinish(String[] paths) {
                mAdapter.changeData(paths);
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }

    private void takeAndCropImage() {
        MblTakeImageActivity.start(this, null, 200, 200, new MblTakeImageCallback() {

            @Override
            public void onFinish(String path) {
                mAdapter.changeData(new String[] { path });
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }

    private void pickAndCropImage() {
        MblPickImageActivity.start(this, 1, 200, 200, new MblPickImageCallback() {

            @Override
            public void onFinish(String[] paths) {
                mAdapter.changeData(paths);
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }
}
