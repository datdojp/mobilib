package com.datdo.mobilib.adapter;

import android.content.Context;
import android.view.View;

/**
 * Created by datdvt on 2015/06/08.
 */
public interface MblUniversalItem {
    View create(Context context);
    void display(View view);
}
