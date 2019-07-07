package com.datdo.mobilib.util;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

//ref: https://stackoverflow.com/a/46166223/3030522
public class MblAsyncTask extends AsyncTask<Void, Void, Void> {

    private WeakReference<Context> contextWeakReference;
    private Runnable action;

    // only retain a weak reference to the activity
    MblAsyncTask(Context context, Runnable action) {
        contextWeakReference = new WeakReference<>(context);
        this.action = action;
    }

    @Override
    protected Void doInBackground(Void... params) {
        action.run();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // get a reference to the activity if it is still there
        Context context = contextWeakReference.get();
        if (context == null ||
                (context instanceof Activity && ((Activity) context).isFinishing())) {
            return;
        }
        super.onPostExecute(result);
    }
}
