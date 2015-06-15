package com.datdo.mobilib.api;

import android.text.TextUtils;

import com.datdo.mobilib.util.MblUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by datdvt on 2015/06/15.
 */
public class MblResponse {

    private MblRequest          mRequest;
    private int                 mStatusCode;
    private String              mStatusCodeReason;
    private Map<String, String> mHeaders;
    private byte[]              mData;

    public MblResponse() {}

    @Override
    public String toString() {
        List<String> tokens = new ArrayList<>();
        tokens.add("STATUS_CODE="           + mStatusCode);
        tokens.add("STATUS_CODE_REASON="    + mStatusCodeReason);
        tokens.add("HEADERS="               + mHeaders);
        tokens.add("DATA="                  + (!MblUtils.isEmpty(mData) ? new String(mData) : ""));
        return "{" + TextUtils.join(", ", tokens) + "}";
    }

    public MblResponse setRequest(MblRequest request) {
        mRequest = request;
        return this;
    }

    public MblResponse setStatusCode(int statusCode) {
        mStatusCode = statusCode;
        return this;
    }

    public MblResponse setStatusCodeReason(String statusCodeReason) {
        mStatusCodeReason = statusCodeReason;
        return this;
    }

    public MblResponse setHeaders(Map<String, String> headers) {
        mHeaders = headers;
        return this;
    }

    public MblResponse setData(byte[] data) {
        mData = data;
        return this;
    }

    public MblRequest getRequest() {
        return mRequest;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public String getStatusCodeReason() {
        return mStatusCodeReason;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public byte[] getData() {
        return mData;
    }
}
