package com.datdo.mobilib.api;

import android.os.Handler;

import java.util.HashMap;
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
        public void onSuccess(int statusCode, byte[] data) {

        }

        @Override
        public void onFailure(int error, String errorMessage) {

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

    public MblRequest() {}

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

    public MblRequest setSuccessStatusCodes(final int from, final int to) {
        mStatusCodeValidator = new MblStatusCodeValidator() {
            @Override
            boolean isSuccess(int statusCode) {
                return from <= statusCode && statusCode <= to;
            }
        };
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
}
