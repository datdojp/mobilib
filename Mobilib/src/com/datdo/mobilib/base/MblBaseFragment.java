package com.datdo.mobilib.base;

import android.support.v4.app.Fragment;

import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblEventListener;
import com.datdo.mobilib.util.MblUtils;

public class MblBaseFragment extends Fragment {

    @Override
    public void onDestroy() {
        if (this instanceof MblEventListener) {
            MblEventCenter.removeListenerFromAllEvents((MblEventListener) this);
        }
        MblUtils.cleanupView(getView());
        super.onDestroy();
    }
}
