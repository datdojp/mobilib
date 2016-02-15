package com.datdo.mobilib.test.utils;

import android.os.Bundle;
import android.widget.ScrollView;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblViewUtil;

public class EditTextAutoscrollTestActivity extends MblBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actitivy_editext_autoscroll_test);
        MblViewUtil.makeEditTextAutoScrollOnFocused(
                getDecorView(),
                (ScrollView) findViewById(R.id.scroll));
    }
}
