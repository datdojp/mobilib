package com.datdo.mobilib.base;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/**
 * <pre>
 * Super class for all {@link ActionBarActivity}.
 * </pre>
 */
public class MblBaseActionBarActivity extends ActionBarActivity {

    private MblActivityPlugin mPlugin = new MblActivityPlugin();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlugin.onCreate(this, savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlugin.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlugin.onPause(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPlugin.onConfigurationChanged(this, newConfig);
    }

    /**
     * @see MblActivityPlugin#isTopActivity(Activity)
     */
    public boolean isTopActivity() {
        return mPlugin.isTopActivity(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(mPlugin.getContentView(this, layoutResID));
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(mPlugin.getContentView(this, view));
    }

    @Override
    public void setContentView(View view, LayoutParams params) {
        super.setContentView(mPlugin.getContentView(this, view, params));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlugin.onDestroy(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPlugin.onActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * @see MblActivityPlugin#getDecorView(Activity)
     */
    public MblDecorView getDecorView() {
        return mPlugin.getDecorView(this);
    }

    /**
     * @see MblActivityPlugin#setMaxAllowedTrasitionBetweenActivity(Activity, long)
     */
    public void setMaxAllowedTrasitionBetweenActivity(long duration) {
        mPlugin.setMaxAllowedTrasitionBetweenActivity(this, duration);
    }

    /**
     * @see MblActivityPlugin#resetDefaultMaxAllowedTrasitionBetweenActivity(Activity)
     */
    public void resetDefaultMaxAllowedTrasitionBetweenActivity() {
        mPlugin.resetDefaultMaxAllowedTrasitionBetweenActivity(this);
    }
}
