package com.datdo.mobilib.base;

import java.io.IOException;

import android.app.Application;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import com.datdo.mobilib.util.MblUtils;

/**
 * <pre>
 * App 's Application class must override this supper class.
 * </pre>
 */
public abstract class MblBaseApplication extends Application {

    private static final String TAG = MblUtils.getTag(MblBaseApplication.class);
    
    private static final String FILE_VERSION_CODE = "mobilib_version_code";
    private static final String FILE_VERSION_NAME = "mobilib_version_name";

    @Override
    public void onCreate() {
        super.onCreate();
        MblUtils.init(this);

        // check version-code changed
        int versionCode = MblUtils.getAppPackageInfo().versionCode;
        byte[] versionCodeData = null;
        try {
            versionCodeData = MblUtils.readInternalFile(FILE_VERSION_CODE);
        } catch (IOException e) {
            Log.e(TAG, "Unable to read file: " + FILE_VERSION_CODE, e);
        }
        int storedVersionCode;
        if (!MblUtils.isEmpty(versionCodeData)) {
            storedVersionCode = Integer.parseInt(new String(versionCodeData));
        } else {
            storedVersionCode = -1;
        }
        if (storedVersionCode < 0 || storedVersionCode != versionCode) {
            onVersionCodeChanged(storedVersionCode, versionCode);
            try {
                MblUtils.saveInternalFile(String.valueOf(versionCode).getBytes(), FILE_VERSION_CODE);
            } catch (IOException e) {
                Log.e(TAG, "Unable to to write file: " + FILE_VERSION_CODE, e);
            }
        }

        // check version-name changed
        String versionName = MblUtils.getAppPackageInfo().versionName;
        byte[] versionNameData = null;
        try {
            versionNameData = MblUtils.readInternalFile(FILE_VERSION_NAME);
        } catch (IOException e) {
            Log.e(TAG, "Unable to read file: " + FILE_VERSION_NAME, e);
        }
        String storedVersionName;
        if (!MblUtils.isEmpty(versionNameData)) {
            storedVersionName = new String(versionNameData);
        } else {
            storedVersionName = null;
        }
        if (storedVersionName == null || !TextUtils.equals(versionName, storedVersionName)) {
            onVersionNameChanged(storedVersionName, versionName);
            try {
                MblUtils.saveInternalFile(versionName.getBytes(), FILE_VERSION_NAME);
            } catch (IOException e) {
                Log.e(TAG, "Unable to to write file: " + FILE_VERSION_NAME, e);
            }
        }

        // register network receiver
        registerReceiver(
                new MblNetworkStatusChangedReceiver(), 
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * <pre>
     * For migration.
     * Invoked when a change of "android:versionCode" in AndroidManifest.xml is detected
     * </pre>
     * @param oldVersionCode
     * @param newVersionCode
     */
    public abstract void onVersionCodeChanged(int oldVersionCode, int newVersionCode);

    /**
     * <pre>
     * For migration.
     * Invoked when a change of "android:versionName" in AndroidManifest.xml is detected
     * </pre>
     * @param oldVersionCode
     * @param newVersionCode
     */
    public abstract void onVersionNameChanged(String oldVersionName, String newVersionName);
}
