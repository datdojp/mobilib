package com.datdo.mobilib.api;

import android.os.Handler;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.datdo.mobilib.api.MblApi.*;
import com.datdo.mobilib.util.MblUtils;

/**
 * <pre>
 * Request specification.
 * </pre>
 * @see com.datdo.mobilib.api.MblApi#run(MblRequest)
 */
public class MblRequest {

    static abstract class MblStatusCodeValidator {
        abstract boolean isSuccess(int statusCode);
    }

    static MblStatusCodeValidator sDefaultStatusCodeValidator = new MblStatusCodeValidator() {
        @Override
        boolean isSuccess(int statusCode) {
            return statusCode >= 200 && statusCode <= 299;
        }
    };

    static MblApiCallback sDefaultCallback = new MblApiCallback() {
        @Override
        public void onSuccess(MblResponse response) {

        }

        @Override
        public void onFailure(MblResponse response) {

        }
    };

    private String                          mUrl;
    private Method                          mMethod;
    private Map<String, ? extends Object>   mParams                 = new HashMap<>();
    private Map<String, String>             mHeaderParams           = new HashMap<>();
    private long                            mCacheDuration          = -1;
    private boolean                         mVerifySSL              = false;
    private MblApiCallback                  mCallback               = sDefaultCallback;
    private Handler                         mCallbackHandler        = MblUtils.getMainThreadHandler();
    private MblStatusCodeValidator          mStatusCodeValidator    = sDefaultStatusCodeValidator;
    private String                          mData;
    private boolean                         mRedirectEnabled         = false;

    public MblRequest() {}

    @Override
    public String toString() {
        List<String> tokens = new ArrayList<>();
        tokens.add("URL="               + mUrl);
        tokens.add("METHOD="            + mMethod.name());
        tokens.add("HEADERS="           + mHeaderParams);
        tokens.add("PARAMS="            + mParams);
        tokens.add("CACHE_DURATION="    + mCacheDuration);
        tokens.add("VERIFY_SSL="        + mVerifySSL);
        tokens.add("DATA="              + mData);
        tokens.add("REDIRECT_ENABLED="  + mRedirectEnabled);
        return "{" + TextUtils.join(", ", tokens) + "}";
    }

    public MblRequest setUrl(String url) {
        mUrl = url;
        return this;
    }

    public MblRequest setMethod(Method method) {
        mMethod = method;
        return this;
    }

    public MblRequest setParams(Map<String, ? extends Object> params) {
        mParams = params;
        return this;
    }

    public MblRequest setParams(Object... args) {
        if (args.length % 2 != 0) {
            throw new RuntimeException("Number of arguments must be even");
        }
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (args[i] == null) {
                throw new RuntimeException(
                        "Argument at event index must not be NULL. Argument at index " + i + " is NULL");
            }
            if (!(args[i] instanceof String)) {
                throw new RuntimeException(
                        "Argument at event index must be instance of String. Argument at index " + i + " is instance of " + args[i].getClass().getSimpleName());
            }
            String key = (String) args[i];
            Object val = args[i+1];
            params.put(key, val);
        }
        mParams = params;
        return this;
    }

    public MblRequest setHeaderParams(Map<String, String> headerParams) {
        mHeaderParams = headerParams;
        return this;
    }

    public MblRequest setHeaderParams(String... args) {
        if (args.length % 2 != 0) {
            throw new RuntimeException("Number of arguments must be even");
        }
        Map<String, String> header = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (args[i] == null) {
                throw new RuntimeException(
                        "Argument at event index must not be NULL. Argument at index " + i + " is NULL");
            }
            String key = args[i];
            String val = args[i+1];
            header.put(key, val);
        }
        mHeaderParams = header;
        return this;
    }


    public MblRequest setCacheDuration(long cacheDuration) {
        mCacheDuration = cacheDuration;
        return this;
    }

    public MblRequest setVerifySSL(boolean verifySSL) {
        mVerifySSL = verifySSL;
        return this;
    }

    public MblRequest setCallback(MblApiCallback callback) {
        mCallback = callback;
        return this;
    }

    public MblRequest setCallbackHandler(Handler callbackHandler) {
        mCallbackHandler = callbackHandler;
        return this;
    }

    public MblRequest setSuccessStatusCodes(final String regex) {
        mStatusCodeValidator = new MblStatusCodeValidator() {
            @Override
            boolean isSuccess(int statusCode) {
                return regex == null || String.valueOf(statusCode).matches(regex);
            }
        };
        return this;
    }

    public MblRequest setSuccessStatusCodes(final int... statusCodes) {
        mStatusCodeValidator = new MblStatusCodeValidator() {
            @Override
            boolean isSuccess(int statusCode) {
                for (int i = 0; i < statusCodes.length; i++) {
                    if (statusCode == statusCodes[i]) {
                        return true;
                    }
                }
                return false;
            }
        };
        return this;
    }

    public MblRequest setData(String data) {
        mData = data;
        return this;
    }

    public MblRequest setRedirectEnabled(boolean redirectEnabled) {
        mRedirectEnabled = redirectEnabled;
        return this;
    }

    public String getUrl() {
        return mUrl;
    }

    public Method getMethod() {
        return mMethod;
    }

    public Map<String, ? extends Object> getParams() {
        return mParams;
    }

    public Map<String, String> getHeaderParams() {
        return mHeaderParams;
    }

    public long getCacheDuration() {
        return mCacheDuration;
    }

    public boolean isVerifySSL() {
        return mVerifySSL;
    }

    public MblApiCallback getCallback() {
        return mCallback;
    }

    public Handler getCallbackHandler() {
        return mCallbackHandler;
    }

    MblStatusCodeValidator getStatusCodeValidator() {
        return mStatusCodeValidator;
    }

    public String getData() {
        return mData;
    }

    public boolean isRedirectEnabled() {
        return mRedirectEnabled;
    }
}
