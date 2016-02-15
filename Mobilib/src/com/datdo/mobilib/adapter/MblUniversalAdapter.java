package com.datdo.mobilib.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.datdo.mobilib.base.MblBaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by datdvt on 2015/06/08.
 */
public class MblUniversalAdapter extends MblBaseAdapter<MblUniversalItem> {

    private Context mContext;
    private Map<View, Class> mViewAndItemClass = new HashMap<>();

    public MblUniversalAdapter(Context context) {
        mContext = context;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        final MblUniversalItem item = (MblUniversalItem) getItem(i);
        final View view;
        Class convertViewItemClass;
        if (convertView == null
                || (convertViewItemClass = mViewAndItemClass.get(convertView)) == null
                || convertViewItemClass != item.getClass()) {
            view = item.create(mContext);
        } else {
            view = convertView;
        }
        mViewAndItemClass.put(view, item.getClass());
        item.display(view);
        return view;
    }

    public void clearData() {
        changeData(new ArrayList<MblUniversalItem>());
    }
}
