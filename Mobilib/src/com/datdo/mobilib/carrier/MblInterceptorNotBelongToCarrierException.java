package com.datdo.mobilib.carrier;

import com.datdo.mobilib.api.MblException;

@SuppressWarnings("serial")
public class MblInterceptorNotBelongToCarrierException extends MblException {
    public MblInterceptorNotBelongToCarrierException(String msg) {
        super(msg);
    }
}
