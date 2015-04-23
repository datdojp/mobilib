package com.datdo.mobilib.carrier;

import com.datdo.mobilib.api.MblException;

/**
 * Thrown when the interceptor does not belongs to carrier.
 * @see com.datdo.mobilib.carrier.MblCarrier#finishInterceptor(MblInterceptor)
 */
@SuppressWarnings("serial")
public class MblInterceptorNotBelongToCarrierException extends MblException {
    public MblInterceptorNotBelongToCarrierException(String msg) {
        super(msg);
    }
}
