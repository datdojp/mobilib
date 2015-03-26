package com.datdo.mobilib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

import junit.framework.Assert;

import org.json.JSONArray;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.datdo.mobilib.base.MblDecorView;
import com.datdo.mobilib.event.MblCommonEvents;
import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblStrongEventListener;

public class MblUtils {
    private static final String TAG = getTag(MblUtils.class);
    private static float density = 0;
    private static float scaledDensity = 0;
    private static final String EMAIL_TYPE = "message/rfc822";

    private static Handler sMainThread = new Handler(Looper.getMainLooper());
    private static Map<String, Object> sCommonBundle = new ConcurrentHashMap<String, Object>();

    private static SharedPreferences sPrefs;
    private static Context sCurrentContext;
    private static LayoutInflater sLayoutInflater;

    public static void init(Context context) {
        sCurrentContext = context;
    }

    /**
     * <pre>
     * Get {@link Handler} for main thread.
     * </pre>
     */
    public static Handler getMainThreadHandler() {
        return sMainThread;
    }

    /**
     * <pre>
     * Get default {@link SharedPreferences} of the app.
     * </pre>
     */
    public static SharedPreferences getPrefs() {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(getCurrentContext());
        }
        return sPrefs;
    }

    /**
     * <pre>
     * Get current context of the app. This method resolves the inconvenience of Android which requires context for most of its API.
     * If no activity is resumed, this method returns application context. Otherwise, this method returns last resumed activity.
     * </pre>
     */
    public static Context getCurrentContext() {
        return sCurrentContext;
    }

    public static void setCurrentContext(Context context) {
        sCurrentContext = context;
    }

    /**
     * <pre>
     * Get {@link Locale} from device 's configuration.
     * Return {@link Locale#JAPAN} if configuration is not found.
     * </pre>
     */
    public static Locale getLocale() {
        if (sCurrentContext != null) {
            return sCurrentContext.getResources().getConfiguration().locale;
        } else {
            return Locale.JAPAN;
        }
    }

    /**
     * <pre>
     * Get {@link LayoutInflater} instance which is essential for adapters.
     * </pre>
     */
    public static LayoutInflater getLayoutInflater() {
        if (sLayoutInflater == null) {
            sLayoutInflater = LayoutInflater.from(getCurrentContext());
        }
        return sLayoutInflater;
    }

    /**
     * <pre>
     * Execute the action in a thread which is not main thread.
     * If current thread is not main thread, execute the action immediately.
     * Otherwise, create new {@link AsyncTask} to execute the action. {@link AsyncTask} is created using {@link AsyncTask#THREAD_POOL_EXECUTOR}.
     * If max number of threads exceeds, wait 1000 milliseconds and call this method again to ensure that the action will be executed.
     * </pre>
     */
    @SuppressLint("NewApi")
    public static void executeOnAsyncThread(final Runnable action) {
        Assert.assertNotNull(action);
        if (!MblUtils.isMainThread()) {
            action.run();
            return;
        }
        MblAsyncTask task = new MblAsyncTask() {
            @Override
            protected Void doInBackground(Void... params) {
                action.run();
                return null;
            }
        };
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Fail to execute on async thread", e);
            getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    executeOnAsyncThread(action);
                }
            }, 1000);
        }
    }

    /**
     * Execute the action on a {@link HandlerThread}
     * @param handler {@link Handler} object bound with {@link HandlerThread} on which action will be executed
     */
    public static void executeOnHandlerThread(Handler handler, Runnable action) {
        Assert.assertNotNull(action);
        Assert.assertNotNull(handler);
        if (Looper.myLooper() == handler.getLooper()) {
            action.run();
        } else {
            handler.post(action);
        }
    }

    /**
     * <pre>
     * Execute the action in main thread.
     * If current thread is main thread, action is executed immediately.
     * Otherwise, post the action to main thread 's looper to execute later.
     * </pre>
     */
    public static void executeOnMainThread(Runnable action) {
        executeOnHandlerThread(getMainThreadHandler(), action);
    }

    /**
     * <pre>
     * Repeat an action every specified milliseconds.
     * </pre>
     * @param action action to run
     * @param delayMillis delay interval in milliseconds
     * @return {@link Runnable} object to stop the repeating. Just call its run() method
     */
    public static Runnable repeatDelayed(final Runnable action, final long delayMillis) {

        if (action == null || delayMillis <= 0) {
            return new Runnable() {
                @Override
                public void run() {}
            };
        }

        final Runnable hookedAction = new Runnable() {
            @Override
            public void run() {
                action.run();
                getMainThreadHandler().postDelayed(this, delayMillis);
            }
        };

        getMainThreadHandler().postDelayed(hookedAction, delayMillis);

        return new Runnable() {
            @Override
            public void run() {
                getMainThreadHandler().removeCallbacks(hookedAction);
            }
        };
    }

    /**
     * <pre>
     * Put an object to temporary bundle to transfer data between objects (typically between activities)
     * This method resolves inconvenience of {@link Intent} which does not allow to put any data into its extra.
     * </pre>
     */
    public static void putToCommonBundle(String key, Object value) {
        if (key != null) {
            sCommonBundle.put(key, value);
        }
    }

    /**
     * <pre>
     * Like {@link #putToCommonBundle(String, Object)} but does not require a key. The key is generated uniquely and returned.
     * </pre>
     */
    public static String putToCommonBundle(Object value) {
        String key = UUID.randomUUID().toString();
        sCommonBundle.put(key, value);
        return key;
    }

    /**
     * <pre>
     * Get data stored in temporary bundle by {@link #putToCommonBundle(Object)} and {@link #putToCommonBundle(String, Object)}.
     * This method is not recommended because it is exposed to potential memory leaks. {@link #removeFromCommonBundle(String)} is recommended.
     * </pre>
     */
    @Deprecated
    public static Object getFromCommonBundle(String key) {
        if (key != null) {
            return sCommonBundle.get(key);
        } else {
            return null;
        }
    }

    /**
     * <pre>
     * Link {@link #getFromCommonBundle(String)} but remove the data from temporary bundle right away.
     * </pre>
     */
    public static Object removeFromCommonBundle(String key) {
        if (key != null) {
            return sCommonBundle.remove(key);
        } else {
            return null;
        }
    }

    /**
     * <pre>
     * Show keyboard, typically in an activity having {@link EditText}.
     * </pre>
     * @param focusedView typically an {@link EditText}
     */
    public static void showKeyboard(View focusedView) {
        focusedView.requestFocus();
        InputMethodManager inputMethodManager = ((InputMethodManager)getCurrentContext().getSystemService(Context.INPUT_METHOD_SERVICE));
        inputMethodManager.showSoftInput(focusedView, InputMethodManager.SHOW_FORCED);
    }

    /**
     * <pre>
     * Hide keyboard.
     * </pre>
     */
    public static void hideKeyboard() {
        Activity activity = (Activity) getCurrentContext();
        View currentFocusedView = activity.getCurrentFocus();
        if (currentFocusedView != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), 0);
        }
    }

    /**
     * <pre>
     * Get name of a class.
     * </pre>
     */
    @SuppressWarnings("rawtypes")
    public static String getTag(Class c) {
        return c.getSimpleName();
    }

    /**
     * <pre>
     * Convert from DP to Pixel.
     * </pre>
     */
    public static int pxFromDp(int dp) {
        if (density == 0) {
            density = getCurrentContext().getResources().getDisplayMetrics().density;
        }
        return (int) (dp * density);
    }

    /**
     * <pre>
     * Convert from SP to Pixel.
     * </pre>
     */
    public static int pxFromSp(int sp) {
        if (scaledDensity == 0) {
            scaledDensity = getCurrentContext().getResources().getDisplayMetrics().scaledDensity;
        }
        return (int) (sp * scaledDensity);
    }

    /**
     * <pre>
     * Determine whether current thread is main thread.
     * </pre>
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * <pre>
     * Determine whether current orientation is portrait.
     * </pre>
     */
    public static boolean isPortraitDisplay() {
        return getCurrentContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * <pre>
     * Determine whether network is currently connected.
     * </pre>
     */
    public static boolean isNetworkConnected() {
        ConnectivityManager conMan = (ConnectivityManager) getCurrentContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    static {
        MblEventCenter.addListener(new MblStrongEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                if (MblCommonEvents.GO_TO_BACKGROUND.equals(name)) {
                    sAppInForeGround = false;
                } else if (MblCommonEvents.GO_TO_FOREGROUND.equals(name)) {
                    sAppInForeGround = true;
                }
            }
        }, new String[] {
                MblCommonEvents.GO_TO_BACKGROUND,
                MblCommonEvents.GO_TO_FOREGROUND
        });
    }

    private static boolean sAppInForeGround = false;

    /**
     * <pre>
     * Determine whether app is in foreground.
     * </pre>
     */
    public static boolean isAppInForeGround() {
        return sAppInForeGround;
    }

    /**
     * <pre>
     * Determine whether keyboard is shown.
     * </pre>
     */
    public static boolean isKeyboardOn() {
        return MblDecorView.isKeyboardOn();
    }

    /**
     * <pre>
     * Create {@link Bitmap} object from byte array, scaling to size targetW x targetH.
     * </pre>
     * @param targetW width to scale. Pass a value <= 0 to ignored width
     * @param targetH height to scale. Pass a value <= 0 to ignored height
     * @param bmData bitmap byte array data
     */
    public static Bitmap loadBitmapMatchSpecifiedSize(final int targetW, final int targetH, final byte[] bmData) {
        return new LoadBitmapMatchSpecifiedSizeTemplate<byte[]>() {

            @Override
            public int[] getBitmapSizes(byte[] bmData) {
                return MblUtils.getBitmapSizes(bmData);
            }

            @Override
            public Bitmap decodeBitmap(byte[] bmData, BitmapFactory.Options options) {
                return BitmapFactory.decodeByteArray(bmData, 0, bmData.length, options);
            }

        }.load(targetW, targetH, bmData);
    }

    /**
     * <pre>
     * Create {@link Bitmap} object from file, scaling to size targetW x targetH.
     * Automatically correct orientation.
     * </pre>
     * @param targetW width to scale. Pass a value <= 0 to ignored width
     * @param targetH height to scale. Pass a value <= 0 to ignored height
     * @param path path to file
     */
    public static Bitmap loadBitmapMatchSpecifiedSize(int targetW, int targetH, final String path) {

        try {
            int angle = MblUtils.getImageRotateAngle(path);
            if (angle == 90 || angle == 270) {
                int temp = targetW;
                targetW = targetH;
                targetH = temp;
            }

            Bitmap bitmap = new LoadBitmapMatchSpecifiedSizeTemplate<String>() {

                @Override
                public int[] getBitmapSizes(String path) {
                    try {
                        return MblUtils.getBitmapSizes(path);
                    } catch (IOException e) {
                        return new int[] {0, 0};
                    }
                }

                @Override
                public Bitmap decodeBitmap(String path, BitmapFactory.Options options) {
                    return BitmapFactory.decodeFile(path, options);
                }

            }.load(targetW, targetH, path);

            if (angle != 0) {
                bitmap = MblUtils.correctBitmapOrientation(path, bitmap);
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap: targetW=" + targetW + ", targetW=" + targetW + ", path=" + path);
            return null;
        }
    }

    private static abstract class LoadBitmapMatchSpecifiedSizeTemplate<T> {

        public abstract int[] getBitmapSizes(T input);
        public abstract Bitmap decodeBitmap(T input, BitmapFactory.Options options);

        public Bitmap load(final int targetW, final int targetH, T input) {

            int scaleFactor = 1;
            int photoW = 0;
            int photoH = 0;
            int[] photoSizes = getBitmapSizes(input);
            photoW = photoSizes[0];
            photoH = photoSizes[1];
            if (targetW > 0 || targetH > 0) {
                // figure out which way needs to be reduced less
                if (photoW > 0 && photoH > 0) {
                    if (targetW > 0 && targetH > 0) {
                        scaleFactor = Math.min(photoW / targetW, photoH / targetH);
                    } else if (targetW > 0) {
                        scaleFactor = photoW / targetW;
                    } else if (targetH > 0) {
                        scaleFactor = photoH / targetH;
                    }
                }
            }

            // ensure sizes not exceed 4096
            final int MAX_SIZE = 4096;
            while (true) {
                int resultWidth     = scaleFactor <= 1 ? photoW : (photoW / scaleFactor);
                int resultHeight    = scaleFactor <= 1 ? photoH : (photoH / scaleFactor);
                if (resultWidth > MAX_SIZE || resultHeight > MAX_SIZE) {
                    scaleFactor++;
                } else {
                    break;
                }
            }

            // set bitmap options to scale the image decode target
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;
            bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            bmOptions.inDither = true;

            // decode the bitmap
            Bitmap bm = decodeBitmap(input, bmOptions);

            // ensure bitmap match exact size
            if (bm != null && bm.getWidth() > 0 && bm.getHeight() > 0) {
                float s = -1;
                if (targetW > 0 && targetH > 0) {
                    if (bm.getWidth() > targetW || bm.getHeight() > targetH) {
                        s = Math.min(1.0f * targetW / bm.getWidth(), 1.0f * targetH / bm.getHeight());
                    }
                } else if (targetW > 0) {
                    if (bm.getWidth() > targetW) {
                        s = 1.0f * targetW / bm.getWidth();
                    }
                } else if (targetH > 0) {
                    if (bm.getHeight() > targetH) {
                        s = 1.0f * targetH / bm.getHeight();
                    }
                }

                if (s > 0) {
                    Matrix matrix = new Matrix();
                    matrix.postScale(s, s);
                    Bitmap scaledBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);
                    bm.recycle();
                    bm = scaledBm;
                }
            }

            return bm;
        }
    }

    /**
     * <pre>
     * Get width and height of bitmap from byte array.
     * </pre>
     * @param bmData bitmap binary data
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(byte[] bmData) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bmData, 0, bmData.length, bmOptions);
        return new int[]{ bmOptions.outWidth, bmOptions.outHeight };
    }

    /**
     * <pre>
     * Get width and height of bitmap from resource.
     * </pre>
     * @param resId resource id of bitmap
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(int resId) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getCurrentContext().getResources(), resId, bmOptions);
        return new int[]{ bmOptions.outWidth, bmOptions.outHeight };
    }

    /**
     * <pre>
     * Get width and height of bitmap from file.
     * </pre>
     * @param path path to file
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(String path) throws IOException {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        FileInputStream is = new FileInputStream(path);
        BitmapFactory.decodeStream(is, null, bmOptions);
        is.close();
        return new int[] { bmOptions.outWidth, bmOptions.outHeight };
    }

    /**
     * <pre>
     * Get width and height of bitmap from InputStream.
     * </pre>
     * @param is the stream
     * @return integer array with 2 elements: width and height
     */
    public static int[] getBitmapSizes(InputStream is) throws IOException {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, bmOptions);
        is.close();
        return new int[] { bmOptions.outWidth, bmOptions.outHeight };
    }

    /**
     * <pre>
     * Recycle a {@link Bitmap} object
     * </pre>
     * @return true if bitmap was recycled successfully
     */
    public static boolean recycleBitmap(Bitmap bm) {
        if (bm != null && !bm.isRecycled()) {
            bm.recycle();
            return true;
        }
        return false;
    }

    /**
     * <pre>
     * Recycle bitmap rendered by {@link ImageView}.
     * </pre>
     * @return true if bitmap was recycled successfully
     */
    public static boolean recycleImageView(ImageView imageView) {
        Bitmap bm = extractBitmap(imageView);
        imageView.setImageBitmap(null);
        return recycleBitmap(bm);
    }

    /**
     * <pre>
     * Clean up view and its children.
     * For ImageView, ImageButton: set image to null.
     * For all views: set background to null.
     * This method is used when an activity/fragment is no longer used.
     * </pre>
     */
    public static void cleanupView(View view) {
        if (view != null) {
            if (view instanceof ImageButton) {
                ImageButton ib = (ImageButton) view;
                ib.setImageDrawable(null);
            } else if (view instanceof ImageView) {
                ImageView iv = (ImageView) view;
                iv.setImageDrawable(null);
            }

            MblUtils.setBackgroundDrawable(view, null);

            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                int size = vg.getChildCount();
                for (int i = 0; i < size; i++) {
                    cleanupView(vg.getChildAt(i));
                }
            }
        }
    }

    /**
     * <pre>
     * Extract {@link Bitmap} object rendered by {@link ImageView}
     * </pre>
     */
    public static Bitmap extractBitmap(ImageView imageView) {
        if (imageView == null) return null;
        Drawable drawable = imageView.getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            Bitmap bm = ((BitmapDrawable)drawable).getBitmap();
            return bm;
        }
        return null;
    }

    /**
     * <pre>
     * Check if android:debuggable is set to true
     * </pre>
     */
    public static boolean getAppFlagDebug() {
        ApplicationInfo appInfo = getCurrentContext().getApplicationInfo();
        int appFlags = appInfo.flags;
        boolean b = (appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        return b;
    }

    /**
     * <pre>
     * Determine whether byte array is empty or null.
     * </pre>
     */
    public static boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }

    /**
     * <pre>
     * Determine whether object array is empty or null.
     * </pre>
     */
    public static boolean isEmpty(Object[] a) {
        return a == null || a.length == 0;
    }

    /**
     * <pre>
     * Determine whether a {@link String} is empty or null.
     * </pre>
     */
    public static boolean isEmpty(String s) {
        return TextUtils.isEmpty(s);
    }

    /**
     * <pre>
     * Determine whether a {@link Collection} is empty or null.
     * </pre>
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Collection c) {
        return c == null || c.isEmpty();
    }

    /**
     * <pre>
     * Determine whether a {@link Map} is empty or null.
     * </pre>
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Map m) {
        return m == null || m.isEmpty();
    }

    /**
     * <pre>
     * Determine whether a {@link JSONArray} is empty or null.
     * </pre>
     */
    public static boolean isEmpty(JSONArray a) {
        return a == null || a.length() == 0;
    }

    /**
     * <pre>
     * Determine whether 2 instances of {@link Collection} contain the same set of objects.
     * </pre>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean equals(Collection c1, Collection c2) {
        if (isEmpty(c1) && isEmpty(c2)) return true;
        if (isEmpty(c1) || isEmpty(c2)) return false;
        if (c1.size() != c2.size()) return false;
        Set s1 = new HashSet(c1);
        Set s2 = new HashSet(c2);
        return s1.containsAll(s2);
    }

    /**
     * <pre>
     * Print a very long {@link String} to logcat by splitting {@link String} object to smaller {@link String} of 4000 characters.
     * </pre>
     */
    public static void logLongString(final String tag, final String str) {
        if(str.length() > 4000) {
            Log.d(tag, str.substring(0, 4000));
            logLongString(tag, str.substring(4000));
        } else {
            Log.d(tag, str);
        }
    }

    /**
     * <prev>
     * Print current stack trace to logcat.
     * </prev>
     */
    public static void logStackTrace(String tag) {
        Log.d(tag, "====================================================");
        logLongString(tag, TextUtils.join("\n", Thread.currentThread().getStackTrace()));
        Log.d(tag, "====================================================");
    }

    /**
     * <pre>
     * Extract domain part of an email address.
     * </pre>
     * @return domain if email is valid, otherwise return null
     */
    public static String extractEmailDomain(String email) {
        String[] splitted = email.split("@");
        return splitted != null && splitted.length == 2 ? splitted[1] : null;
    }

    /**
     * <prev>
     * Get root view of an activity.
     * </prev>
     */
    public static View getRootView(Activity activity) {
        return activity.getWindow().getDecorView().findViewById(android.R.id.content);
    }

    /**
     * <prev>
     * Remove focus on every child views of an activity.
     * </prev>
     */
    public static void focusNothing(Activity activity) {
        focusNothing(getRootView(activity));
    }

    private static void focusNothing(View rootView) {
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
    }

    /**
     * <pre>
     * Get sizes of screen in pixels.
     * </pre>
     * @return interger array with 2 elements: width and height
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public static int[] getDisplaySizes() {
        Context context = getCurrentContext();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        if (Build.VERSION.SDK_INT < 13) {
            return new int[] {display.getWidth(), display.getHeight() };
        } else {
            Point point = new Point();
            display.getSize(point);
            return new int[] { point.x, point.y };
        }
    }

    /**
     * <pre>
     * Get MD5-hashed code for a {@link String}.
     * </pre>
     */
    public static String md5(final String name) {
        try {
            // create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(name.getBytes());
            byte messageDigest[] = digest.digest();

            // create hex string
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Unable to hash name in md5", e);
            return null;
        }
    }

    /**
    private static String encodeFileName(String name) {
        if (name == null) return "default";
        String s = name;
        s = s.replaceAll("/", "_");
        s = s.replaceAll(":", "_");
        s = s.replaceAll("\\?", "_");
        return s;
    }
     **/

    /**
     * <pre>
     * Get absolute path in app 's cache folder from relative path.
     * </pre>
     */
    public static String getCacheAsbPath(String relativePath) {
        File cacheDir = getCurrentContext().getCacheDir();
        return cacheDir.getAbsolutePath().concat("/").concat(relativePath);
    }

    /**
     * <pre>
     * Save byte array to ap 's cache folder.
     * </pre>
     * @param in byte array
     * @param relativePath relative path to destination file
     */
    public static void saveCacheFile(byte[] in, String relativePath) throws IOException {
        saveFile(in, getCacheAsbPath(relativePath));
    }

    /**
     * <pre>
     * Save byte array to arbitrary file.
     * </pre>
     * @param in byte array
     * @param absolutePath absolute path to destination file
     */
    public static void saveFile(byte[] in, String absolutePath) throws IOException {
        File file = new File(absolutePath);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(absolutePath);
        out.write(in);
        out.close();
    }

    /**
     * <pre>
     * Read binary data from file stored in app 's cache folder.
     * </pre>
     * @param relativePath relative path to source file
     * @return binary data
     */
    public static byte[] readCacheFile(String relativePath) throws IOException {
        return readFile(getCacheAsbPath(relativePath));
    }

    /**
     * <pre>
     * Read binary data from arbitrary file.
     * </pre>
     * @param absolutePath absolute path to source file
     * @return binary data
     */
    public static byte[] readFile(String absolutePath) throws IOException {
        File file = new File(absolutePath);
        if (!file.exists()) {
            return null;
        }

        FileInputStream in = new FileInputStream(file);
        byte[] b = new byte[in.available()];
        in.read(b);
        in.close();

        return b;
    }

    /**
     * <pre>
     * Save binary data to file in app 's internal memory.
     * </pre>
     * @param in byte array
     * @param absolutePath absolute path to destination file
     * @throws IOException
     */
    public static void saveInternalFile(byte[] in, String absolutePath) throws IOException {
        FileOutputStream out = getCurrentContext().openFileOutput(absolutePath, Context.MODE_PRIVATE);
        out.write(in);
        out.close();
    }

    /**
     * <pre>
     * Read binary data from file stored in app 's internal memory.
     * </pre>
     * @param absolutePath absolute path to source file
     * @return binary data
     */
    public static byte[] readInternalFile(String absolutePath) throws IOException {
        FileInputStream in = getCurrentContext().openFileInput(absolutePath);
        byte[] b = new byte[in.available()];
        in.read(b);
        in.close();

        return b;
    }

    /**
     * <pre>
     * Convenient method to create and show alert in main thread.
     * </pre>
     * @param title alert 's title
     * @param message alert 's message
     * @param postTask action to execute after user presses "OK" button
     */
    public static void showAlert(final String title, final String message, final Runnable postTask) {
        executeOnMainThread(new Runnable() { 
            @Override
            public void run() {
                new AlertDialog.Builder(getCurrentContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (postTask != null) getMainThreadHandler().post(postTask);
                    }
                })
                .show();
            }
        });
    }

    /**
     * <pre>
     * Like {@link #showAlert(String, String, Runnable)}
     * </pre>
     */
    public static void showAlert(final int titleResId, final int messageResId, final Runnable postTask) {
        showAlert(
                getCurrentContext().getString(titleResId),
                getCurrentContext().getString(messageResId),
                postTask);
    }

    private static ProgressDialog sProgressDialog;

    /**
     * <pre>
     * Convenient method to create and show progress dialog in main thread.
     * </pre>
     * @param cancelable whether progress dialog can be canceled by pressing back button
     */
    public static void showProgressDialog(final String message, final boolean cancelable) {
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (sProgressDialog != null && sProgressDialog.isShowing()) {
                    sProgressDialog.dismiss();
                }
                sProgressDialog = new ProgressDialog(getCurrentContext());
                sProgressDialog.setMessage(message);
                sProgressDialog.setCancelable(cancelable);
                sProgressDialog.show();
            }
        });
    }

    /**
     * <pre>
     * Like {@link #showProgressDialog(String, boolean)}
     * </pre>
     */
    public static void showProgressDialog(final int messageResId, final boolean cancelable) {
        showProgressDialog(getCurrentContext().getString(messageResId), cancelable);
    }

    /**
     * <pre>
     * Hide progress dialog shown by {@link #showProgressDialog(int, boolean)} and {@link #showProgressDialog(String, boolean)}
     * </pre>
     */
    public synchronized static void hideProgressDialog() {
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (sProgressDialog != null && sProgressDialog.isShowing()) {
                    sProgressDialog.hide();
                }
                sProgressDialog = null;
            }
        });
    }

    /**
     * <pre>
     * Convenient method to show toast in main thread.
     * </pre>
     * @param duration {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}
     */
    public static void showToast(final String text, final int duration) {
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getCurrentContext(), text, duration).show();
            }
        });
    }

    /**
     * <pre>
     * Like {@link #showToast(String, int)}
     * </pre>
     */
    public static void showToast(int messageResId, int duration) {
        showToast(getCurrentContext().getString(messageResId), duration);
    }

    /**
     * <pre>
     * Convenient method to show confirmation dialog with message, positive button, negative button.
     * </pre>
     * @param message
     * @param positiveButtonText
     * @param negativeButtonText
     * @param action action to be executed when user press positive button
     */
    public static void showConfirm(
            final String message,
            final String positiveButtonText,
            final String negativeButtonText,
            final Runnable action) {

        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(getCurrentContext())
                .setMessage(message)
                .setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        action.run();
                    }
                })
                .setNegativeButton(negativeButtonText, null)
                .show();
            }
        });
    }

    /**
     * <pre>
     * Like {@link #showConfirm(String, String, String, Runnable)
     * </pre>
     */
    public static void showConfirm(
            final int messageResId,
            final int positiveButtonResId,
            final int negativeButtonResId,
            final Runnable action) {

        showConfirm(
                getCurrentContext().getString(messageResId),
                getCurrentContext().getString(positiveButtonResId),
                getCurrentContext().getString(negativeButtonResId),
                action);
    }

    /**
     * <pre>
     * Remove {@link OnGlobalLayoutListener} object from view 's {@link ViewTreeObserver}, which is different between API < 16 and API >=16.
     * 
     * Here is sample usage:
     * <code>
     * view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
     *     {@literal @}Override
     *     public void onGlobalLayout() {
     *         MblUtils.removeOnGlobalLayoutListener(view, this);
     *         // ...
     *     }
     * });
     * </code>
     * </pre>
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void removeOnGlobalLayoutListener(View view, OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < 16) {
            view.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        } else {
            view.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    /**
     * <pre>
     * Convenient method to send email.
     * </pre>
     * @param subject email 's subject
     * @param emails target email addresses
     * @param text email 's body text
     * @param title message displayed when user selects app to send email
     * @param attachmentFilenames paths to attachment files
     * @return true if email app is opened successfully
     */
    public static boolean sendEmail(
            String subject,
            String[] emails,
            String[] cc,
            String[] bcc,
            Object text,
            String title,
            List<String> attachmentFilenames) {
        Intent intent;
        if (isEmpty(attachmentFilenames)) {
            intent = new Intent(Intent.ACTION_SEND);
        } else {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        }
        intent.setType(EMAIL_TYPE);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_EMAIL, emails);
        if (!isEmpty(cc)) {
            intent.putExtra(Intent.EXTRA_CC, cc);
        }
        if (!isEmpty(bcc)) {
            intent.putExtra(Intent.EXTRA_BCC, bcc);
        }
        if (text instanceof String) {
            intent.putExtra(Intent.EXTRA_TEXT, (String)text);
        } else if (text instanceof Spanned) {
            intent.putExtra(Intent.EXTRA_TEXT, (Spanned)text);
        }
        if (!isEmpty(attachmentFilenames)) {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            for (String name : attachmentFilenames) {
                uris.add(Uri.fromFile(new File(name)));
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        try {
            getCurrentContext()
            .startActivity(Intent.createChooser(intent, title));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Cannot send email", e);
            return false;
        }
        return true;
    }

    /*
    public static void copyAssetFiles(Pattern pattern) throws IOException {
        AssetManager assetManager = getCurrentContext().getAssets();
        String[] fileList = assetManager.list("");
        for (String path : fileList) {
            if (pattern == null || pattern.matcher(path).matches()) {
                copyAssetFile(path, path);
            }
        }
    }
     */

    /*
    public static void copyAssetFile(String src, String dst) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        AssetManager assets = getCurrentContext().getAssets();
        in = assets.open(src);
        out = getCurrentContext().openFileOutput(dst, Context.MODE_PRIVATE);

        copyFile(in, out);
    }
     */

    /**
     * <pre>
     * Convenient method to copy file.
     * </pre>
     * @param in {@link InputStream} of source file
     * @param out {@link OutputStream} of destination file
     */
    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }

        in.close();
        out.flush();
        out.close();
    }

    /**
     * <pre>
     * Determine whether a {@link MotionEvent} is on a {@link View}
     * </pre>
     */
    public static boolean motionEventOnView(MotionEvent event, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int w = view.getWidth();
        int h = view.getHeight();
        Rect rect = new Rect(x, y, x+w, y+h);
        return rect.contains((int)event.getRawX(), (int)event.getRawY());
    }

    /*
    public static Bitmap loadBitmapFromInternalStorage(String path) {
        if (isEmpty(path)) return null;
        FileInputStream is = null;
        Bitmap bm = null;
        try {
            is = getCurrentContext().openFileInput(path);
            bm = BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + path, e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                // ignored
            }
        }
        return bm;
    }
     */

    /**
     * <pre>
     * Determine whether an app is installed on device.
     * </pre>
     * @param packageName app 's package name
     */
    public static boolean isAppInstalled(String packageName) {

        if (MblUtils.isEmpty(packageName)) return false;

        PackageManager pm = getCurrentContext().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            // do nothing
        }
        return false;
    }

    /**
     * <pre>
     * Copy text to clipboard.
     * Different implementation for API < 11 and API >= 11.
     * </pre>
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void copyTextToClipboard(String text) {
        if (Build.VERSION.SDK_INT < 11) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getCurrentContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getCurrentContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("", text));
        }
    }

    /**
     * <pre>
     * Start other app by its package name.
     * </pre>
     * @param packageName app 's package name
     */
    public static void openApp(String packageName) {
        Context context = getCurrentContext();
        PackageManager manager = context.getPackageManager();
        Intent intent = manager.getLaunchIntentForPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(intent);
    }

    /**
     * <pre>
     * Open other app to view URL of an app (typically browser or Google Play)
     * </pre>
     * @param downloadUrl
     */
    public static void openDownloadPage(String downloadUrl) {
        Context context = getCurrentContext();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(downloadUrl));
        context.startActivity(intent);
    }

    /**
     * <pre>
     * Set {@link Drawable} as background of a view, which is different in API < 16 and API >= 16.
     * </pre>
     * @param view target view
     * @param drawable background drawable
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void setBackgroundDrawable(View view, Drawable drawable) {
        if (view == null) return;

        if (Build.VERSION.SDK_INT >= 16) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable); 
        }
    }

    /**
     * <pre>
     * Delete a file stored in app 's internal memory.
     * </pre>
     * @param path absolute path to file
     */
    public static void deleteInternalFile(String path) {
        Context context = getCurrentContext();
        context.deleteFile(path);
    }

    /**
     * <pre>
     * Add "0" to head of number string so that length >= minLength
     * </pre>
     */
    public static String fillZero(String numberString, int minLength) {
        if (numberString == null) numberString = "";

        int diff = minLength - numberString.length();
        for (int i = 0; i < diff; i++) {
            numberString = "0" + numberString;
        }

        return numberString;
    }

    /**
     * <pre>
     * Get rotation angle of an image.
     * This information is stored in image file. Therefore, this method needs path to file, not a {@link Bitmap} object or byte array.
     * </pre>
     * @param imagePath absolute path to image file
     * @return one of 0, 90, 180, 270
     */
    public static int getImageRotateAngle(String imagePath) throws IOException {
        ExifInterface exif = new ExifInterface(imagePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int angle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            angle = 90;
        } 
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            angle = 180;
        } 
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            angle = 270;
        }

        return angle;
    }

    /**
     * <pre>
     * Rotate bitmap to its correct orientation if needed.
     * WARNING: {@link Bitmap} is immutable. Therefore, when new {@link Bitmap} is created, old {@link Bitmap} object is recycled to prevent {@link OutOfMemoryError}.
     * </pre>
     * @param path absolute path to image file
     * @param bm {@link Bitmap} object
     * @return rotated {@link Bitmap} object if angel != 0, otherwise return original {@link Bitmap} object
     */
    public static Bitmap correctBitmapOrientation(String path, Bitmap bm) {
        if (path != null && bm != null) {
            int angle = 0;
            try {
                angle = getImageRotateAngle(path);
                if (angle != 0) {

                    Matrix matrix = new Matrix();
                    matrix.postRotate(angle);
                    Bitmap rotatedBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);

                    bm.recycle();
                    bm = rotatedBm;
                }
            } catch (IOException e) {
                Log.e(TAG, "Can not rotate bitmap path: " + path + ", angle:" + angle, e);
            }
        }
        return bm;
    }

    /**
     * <pre>
     * Scroll {@link ListView} to its bottom item.
     * </pre>
     */
    public static void scrollListViewToBottom(final ListView listView) {
        if (listView == null || listView.getAdapter() == null) {
            return;
        }

        final Runnable action = new Runnable() {
            @Override
            public void run() {
                int count = listView.getAdapter().getCount();
                if (count > 0) {
                    listView.setSelectionFromTop(count, -listView.getHeight());
                }
            }
        };

        if (listView.getHeight() > 0) {
            executeOnMainThread(action);
        } else {
            listView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    removeOnGlobalLayoutListener(listView, this);
                    action.run();
                }
            });
        }
    }

    /**
     * <pre>
     * Generate Unique Device ID.
     * 
     * I did some research on the topic "Unique device Id for android", and found some remarkable articles about it:
     *   http://android-developers.blogspot.com/2011/03/identifying-app-installations.html
     *   http://developer.samsung.com/android/technical-docs/How-to-retrieve-the-Device-Unique-ID-from-android-device
     * 
     * These articles propose many solutions for device id:
     *   1 - Phone Device ID (IMEI, MEID, ESN, IMSI) ==> device must be a phone
     *   2 - Serial Number ==> device must be a non-phone device (although some phone devices also have this value)
     *   3 - Mac Address ==> changed frequently (do not use)
     *   4 - ANDROID_ID ==> duplicated on some devices (some Motorola device, Froyo, or custom ROMs...)
     *   5 - Generate a UUID and store it in external storage ==> this is unsafe because user can copy the login_info file and uuid file to other device, do not use
     * 
     * As you can see, none of them is totally reliable. Therefore, I decided to combine 1,2 and 4 to generate a custom device ID which is totally secured in all cases.
     * The drawback is that we need to require for READ_PHONE_STATE permission.
     * </pre>
     * @return the generated unique device ID
     */
    @SuppressLint("NewApi")
    public static String generateDeviceId() {
        Context context = MblUtils.getCurrentContext();
        StringBuilder builder = new StringBuilder();

        // android id
        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID); 
        if (!TextUtils.isEmpty(androidId)) builder.append(androidId);

        // serial
        if (Build.VERSION.SDK_INT >= 9) {
            String serial = Build.SERIAL;
            if (!TextUtils.isEmpty(serial) && !Build.UNKNOWN.equals(serial)) builder.append(serial);
        }

        // phone device id
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getDeviceId();
        if (!TextUtils.isEmpty(deviceId)) builder.append(deviceId);

        // combine & hash
        return md5(builder.toString());
    }

    /*
    public static void loadInternalImage(
            final String path,
            final ImageView target) {

        if (TextUtils.isEmpty(path)) return;

        executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bm = loadInternalImage(path);
                executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        target.setImageBitmap(bm);
                    }
                });
            }
        });
    }
     */

    /*
    public static Bitmap loadInternalImage(String path) {
        if (TextUtils.isEmpty(path)) return null;

        FileInputStream is = null;
        Bitmap bm = null;
        try {
            is = getCurrentContext().openFileInput(path);
            bm = BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Can not load image from internal storage: path=" + path, e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                // ignored
            }
        }
        return bm;
    }
     */

    /*
    public static void copyAssetFileToExternalMemory(String src, String dst) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        AssetManager assets = getCurrentContext().getAssets();
        in = assets.open(src);
        out = new FileOutputStream(dst);

        copyFile(in, out);
    }
     */

    /**
     * <pre>
     * Kill app.
     * Reference: http://stackoverflow.com/questions/6330200/how-to-quit-android-application-programmatically
     * </pre>
     * @param mainActivityClass {@link Class} object of app 's main activity
     */
    public static void closeApp(final Class<? extends Activity> mainActivityClass) {
        closeApp(mainActivityClass, null);
    }

    public static void closeApp(
            final Class<? extends Activity> mainActivityClass,
            final Runnable beforeCloseAction) {

        // start main activity
        Context context = getCurrentContext();
        Intent intent = new Intent(context, mainActivityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        // wait until main activity is resumed
        MblEventCenter.addListener(new MblStrongEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                Activity activity = (Activity) MblEventCenter.getArgAt(0, args);
                if (activity == null) {
                    return;
                }
                if (mainActivityClass.isInstance(activity)) {
                    terminate();
                    activity.finish();
                    if (beforeCloseAction != null) {
                        beforeCloseAction.run();
                    }
                    System.exit(0);
                }
            }
        }, MblCommonEvents.ACTIVITY_CREATED);
    }

    /**
     * <pre>
     * Kill app and restart app after 500ms
     * </pre>
     * @param mainActivityClass {@link Class} object of app 's main activity
     */
    public static void restartApp(final Class<? extends Activity> mainActivityClass) {
        closeApp(mainActivityClass, new Runnable() {
            @Override
            public void run() {
                Context context = MblUtils.getCurrentContext();
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context,
                        1424287352,
                        new Intent(context, mainActivityClass),
                        PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);
            }
        });
    }

    /**
     * <pre>
     * Get app 's PackageInfo.
     * </pre>
     */
    public static PackageInfo getAppPackageInfo() {
        try {
            Context context = MblUtils.getCurrentContext();
            String packageName = context.getPackageName();
            return context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            Log.i(TAG, "Could not get app name and version", e);
        }
        return null;
    }

    /**
     * <pre>
     * Determine whether a {@link String} object is a link.
     * </pre>
     */
    public static boolean isLink(String s) {
        return MblUrlRecognizer.isLink(s);
    }

    /**
     * <pre>
     * Android do not understand prefixes like "HTtP" or "hTtP".
     * Therefore, we need to make all http/https prefixes lower-case
     * </pre>
     */
    public static String lowerCaseHttpxPrefix(String link) {
        return MblUrlRecognizer.lowerCaseHttpxPrefix(link);
    }

    /**
     * <pre>
     * Open other app to view a link.
     * </pre>
     */
    public static void openLink(String link) {
        if (isEmpty(link) || !isLink(link)) {
            return;
        }
        link = lowerCaseHttpxPrefix(link);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        getCurrentContext().startActivity(browserIntent);
    }
    
    private static final String URI_FILE_PREFIX             = "file://";
    private static final String URI_CONTENT_PREFIX          = "content://";

    /**
     * <pre>
     * Extract file path from URI, which can be used when handling {@link Intent#ACTION_SEND} or {@link Intent#ACTION_SEND_MULTIPLE}
     * </pre>
     * @param uri {@link Uri} object to extract
     * @return path to the file
     */
    public static String extractFilePathFromUri(Uri uri) {
        String uriString = uri.toString();
        if (uriString != null && uriString.startsWith(URI_FILE_PREFIX)) {
            return uriString.substring(URI_FILE_PREFIX.length());
        }
        if (uriString != null && uriString.startsWith(URI_CONTENT_PREFIX)) {
            Cursor cursor = getCurrentContext().getContentResolver().query(uri, null, null, null, null);
            String filePath = null;
            if (cursor != null && cursor.moveToFirst()) {
                filePath = cursor.getString(cursor.getColumnIndexOrThrow(Images.Media.DATA));
            }
            return filePath;
        }
        return null; // invalid URI
    }

    /**
     * <pre>
     * Get app 's key hash.
     * </pre>
     * @return key hash
     */
    public static String getKeyHash() {
        try {
            Context context = getCurrentContext();
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                return new String(keyHash);
            }
            Log.e(TAG, "getKeyHash: no signature found");
            return null;
        } catch (Throwable e) {
            Log.e(TAG, "getKeyHash: error occurred", e);
            return null;
        }
    }

    /**
     * <pre>
     * When uploading a file to server, normally client app must scale image so that its sizes {@literal <}= limited size specified by server.
     * This method help you to do that, without worrying about OutOfMemoryError.
     * If OutOfMemoryError occurs, it will retry 2 times more (each after 2 seconds).
     * Note that in case image size is already {@literal <}= limited size, the original path will be returned in callback method.
     * </pre> 
     * @param path absolute path to original image
     * @param maxSizeLimit limited size specified by server
     * @param callback callback to receive result (path to scaled image)
     */
    public static void createImageFileForUpload(
            final String path,
            final int maxSizeLimit,
            final MblCreateImageFileForUploadCallback callback) {

        final int   N_RETRIES   = 3;
        final int[] nRetries    = new int[] { 0 };
        final long  RETRY_AFTER = 2000l;

        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                nRetries[0]++;
                String scaledImagePath = null;

                try {
                    // load
                    Bitmap bm = loadBitmapMatchSpecifiedSize(maxSizeLimit, maxSizeLimit, path);

                    // write bitmap to file
                    scaledImagePath = MblUtils.getCacheAsbPath(UUID.randomUUID().toString() + ".jpg");
                    FileOutputStream os = new FileOutputStream(scaledImagePath);
                    bm.compress(CompressFormat.JPEG, 100, os);
                    os.flush();
                    os.close();
                    bm.recycle();

                    // return path to generated file
                    if (callback != null) {
                        final String fScaledPath = scaledImagePath;
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(fScaledPath);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error when creating image file for upload", e);
                    if (scaledImagePath != null && !TextUtils.equals(path, scaledImagePath)) {
                        new File(scaledImagePath).delete();
                    }

                    if (callback != null) {
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError();
                            }
                        });
                    }
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Error when creating image file for upload", e);
                    if (nRetries[0] < N_RETRIES) {
                        Log.d(TAG, "Retry after " + RETRY_AFTER + " ms");
                        System.gc();
                        final Runnable fThis = this;
                        MblUtils.getMainThreadHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                MblUtils.executeOnAsyncThread(fThis);
                            }
                        }, RETRY_AFTER);
                    } else {
                        if (scaledImagePath != null && !TextUtils.equals(path, scaledImagePath)) {
                            new File(scaledImagePath).delete();
                        }

                        if (callback != null) {
                            MblUtils.executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError();
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    /**
     * <pre>
     * Callback to receive result in {@link MblUtils#createImageFileForUpload(String, int, MblCreateImageFileForUploadCallback)}
     * </pre>
     */
    public static interface MblCreateImageFileForUploadCallback {
        /**
         * @param scaledImagePath absolute path to scaled image.
         */
        public void onSuccess(String scaledImagePath);
        public void onError();
    }

    /**
     * <pre>
     * Load bitmap from file in async thread, then set bitmap data to {@link ImageView} object in main thread.
     * Also support scaling to specific sizes.
     * </pre>
     * @param path path to image file
     * @param imageView {@link ImageView} object to display image
     * @param width specific width to scale. -1 to ignore
     * @param height specific height to scale. -1 to ignore
     * @param callback callback to receive result
     */
    public static void loadBitmapForImageView(
            final String path,
            final ImageView imageView,
            final int width,
            final int height,
            final MblLoadBitmapForImageViewCallback callback) {

        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                try {

                    final Bitmap bm = MblUtils.loadBitmapMatchSpecifiedSize(width, height, path);

                    MblUtils.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bm);
                            if (callback != null) {
                                if (bm != null) {
                                    callback.onSuccess();
                                } else {
                                    callback.onError();
                                }
                            }
                        }
                    });
                } catch (Throwable e) {
                    Log.e(TAG, "Error occurred when loading bitmap for image view: path=" + path, e);
                    if (callback != null) {
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError();
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * <pre>
     * Load bitmap from file in async thread, then set bitmap data to {@link ImageView} object in main thread.
     * Automatically scale bitmap to ImageView sizes.
     * </pre>
     * @param path path to image file
     * @param imageView {@link ImageView} object to display image
     * @param callback callback to receive result
     */
    public static void loadBitmapForImageView(
            final String path,
            final ImageView imageView,
            final MblLoadBitmapForImageViewCallback callback) {

        if (imageView.getWidth() == 0 || imageView.getHeight() == 0) {
            imageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    removeOnGlobalLayoutListener(imageView, this);
                    loadBitmapForImageView(path, imageView, callback);
                }
            });
            return;
        }

        loadBitmapForImageView(
                path,
                imageView,
                imageView.getWidth(),
                imageView.getHeight(),
                callback);
    }

    /**
     * <pre>
     * Callback to receive result in {@link MblUtils#loadBitmapForImageView(String, ImageView, int, int, MblLoadBitmapForImageViewCallback)}
     * </pre>
     */
    public static interface MblLoadBitmapForImageViewCallback {
        public void onSuccess();
        public void onError();
    }
}
