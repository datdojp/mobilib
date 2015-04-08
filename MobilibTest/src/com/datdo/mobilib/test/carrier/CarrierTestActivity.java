package com.datdo.mobilib.test.carrier;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.carrier.MblCarrier;
import com.datdo.mobilib.carrier.MblSlidingCarrier;
import com.datdo.mobilib.carrier.MblSlidingCarrier.*;
import com.datdo.mobilib.test.R;

public class CarrierTestActivity extends MblBaseActivity {

    private MblSlidingCarrier mCarrier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrier_test);


        FrameLayout interceptorContainerView = (FrameLayout) findViewById(R.id.interceptor_container);
        mCarrier = new MblSlidingCarrier(this, interceptorContainerView, new MblCarrier.MblCarrierCallback() {
            @Override
            public void onNoInterceptor() {
                finish();
            }
        });
        mCarrier.startInterceptor(Interceptor1.class);

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
                    mCarrier.setSlidingDirection(SlidingDirection.LEFT_RIGHT);
                    break;
                case 1:
                    mCarrier.setSlidingDirection(SlidingDirection.RIGHT_LEFT);
                    break;
                case 2:
                    mCarrier.setSlidingDirection(SlidingDirection.TOP_BOTTOM);
                    break;
                case 3:
                    mCarrier.setSlidingDirection(SlidingDirection.BOTTOM_TOP);
                    break;
                default:
                    break;
                }
            }
        })
        .show();
    }

    @Override
    public void onBackPressed() {
        if (mCarrier.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
