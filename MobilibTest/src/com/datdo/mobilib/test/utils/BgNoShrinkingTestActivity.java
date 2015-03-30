package com.datdo.mobilib.test.utils;

import android.os.Bundle;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblViewUtil;

public class BgNoShrinkingTestActivity extends MblBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bg_no_shrinking_test);
        MblViewUtil.setBackgroundNoShrinking(getDecorView(), R.drawable.background);
    }
}
