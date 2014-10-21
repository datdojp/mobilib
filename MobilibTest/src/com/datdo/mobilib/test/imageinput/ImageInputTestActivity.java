package com.datdo.mobilib.test.imageinput;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.imageinput.MblPickImageActivity;
import com.datdo.mobilib.imageinput.MblPickImageActivity.MblPickImageCallback;
import com.datdo.mobilib.imageinput.MblTakeImageActivity;
import com.datdo.mobilib.imageinput.MblTakeImageActivity.MblTakeImageCallback;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblUtils;

public class ImageInputTestActivity extends MblBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_input_test);

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
    }

    private void takeImage() {
        MblTakeImageActivity.start(null, -1, -1, new MblTakeImageCallback() {

            @Override
            public void onFinish(String path) {
                MblUtils.showToast(path, Toast.LENGTH_SHORT);
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }

    private void pickImage() {
        MblPickImageActivity.start(5, -1, -1, new MblPickImageCallback() {

            @Override
            public void onFinish(String[] paths) {
                MblUtils.showToast(TextUtils.join("\n", paths), Toast.LENGTH_SHORT);
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }

    private void takeAndCropImage() {
        MblTakeImageActivity.start(null, 200, 200, new MblTakeImageCallback() {

            @Override
            public void onFinish(String path) {
                MblUtils.showToast(path, Toast.LENGTH_SHORT);
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }

    private void pickAndCropImage() {
        MblPickImageActivity.start(1, 200, 200, new MblPickImageCallback() {

            @Override
            public void onFinish(String[] paths) {
                MblUtils.showToast(TextUtils.join("\n", paths), Toast.LENGTH_SHORT);
            }

            @Override
            public void onCancel() {
                MblUtils.showToast("Canceled", Toast.LENGTH_SHORT);
            }
        });
    }
}
