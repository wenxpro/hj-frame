package com.wenx.v3core.error;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务器异常
 *
 * @author wenx
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceException extends RuntimeException {

    private Integer code;

    private String errorMessage;

    public ServiceException(Integer code, String errorMessage) {
        super(errorMessage);
        this.code = code;
        this.errorMessage = errorMessage;
    }

    public ServiceException(ServerExceptionEnum exception) {
        super(exception.getMessage());
        this.errorMessage = exception.getMessage();
    }
}