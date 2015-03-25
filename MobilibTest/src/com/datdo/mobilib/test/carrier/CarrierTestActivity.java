package com.datdo.mobilib.test.carrier;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.datdo.mobilib.carrier.MblSlidingCarrier;
import com.datdo.mobilib.test.R;

public class CarrierTestActivity extends MblSlidingCarrier {

    private FrameLayout mInterceptorContainerView;

    @Override
    protected FrameLayout getInterceptorContainerView() {
        return mInterceptorContainerView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrier_test);
        mInterceptorContainerView = (FrameLayout) findViewById(R.id.interceptor_container);
        startInterceptor(Interceptor1.class);
        selectSlidingDirection();
    }

    private void selectSlidingDirection() {
        new AlertDialog.Builder(this)
        .setItems(new String[] {
                "Left Right",
                "Right Left",
                "Top Bottom",
                "Bottom Top"
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0:
                    setSlidingDirection(SlidingDirection.LEFT_RIGHT);
                    break;
                case 1:
                    setSlidingDirection(SlidingDirection.RIGHT_LEFT);
                    break;
                case 2:
                    setSlidingDirection(SlidingDirection.TOP_BOTTOM);
                    break;
                case 3:
                    setSlidingDirection(SlidingDirection.BOTTOM_TOP);
                    break;
                default:
                    break;
                }
            }
        })
        .show();
    }
}
