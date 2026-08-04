package com.wenx.v3core.error;

import lombok.Getter;

/**
 * @author wenx
 * @description 业务异常（D8：携带统一错误码，默认 BUSINESS_ERROR）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException() {
        this(ErrorCode.BUSINESS_ERROR, null, null);
    }

    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_ERROR, message, null);
    }

    public BusinessException(String message, Throwable cause) {
        this(ErrorCode.BUSINESS_ERROR, message, cause);
    }

    public BusinessException(Throwable cause) {
        this(ErrorCode.BUSINESS_ERROR, null, cause);
    }

    public BusinessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = ErrorCode.BUSINESS_ERROR;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.getMsg(), cause);
        this.errorCode = errorCode;
    }
}
