package com.wenx.v3core.response;

import com.wenx.v3core.response.impl.ApiErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * response
 *
 * @author wenx
 */
@Data
@Schema(name = "R", description = "返回结果")
public class R implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "code", description = "业务错误码")
    private long code;

    @Schema(name = "data", description = "结果集")
    private Object data;

    @Schema(name = "msg", description = "描述")
    private String msg;

    public R() {
        // to do nothing
    }

    public R(IErrorCode errorCode) {
        errorCode = Optional.ofNullable(errorCode).orElse(ApiErrorCode.FAILED);
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public static R render(ApiErrorCode apiErrorCode) {
        return restResult(null, apiErrorCode.getCode(), apiErrorCode.getMsg());
    }

    public static R ok() {
        return render(ApiErrorCode.SUCCESS);
    }

    public static R failed() {
        return render(ApiErrorCode.FAILED);
    }

    public static R failed(Object data) {
        ApiErrorCode aec = ApiErrorCode.FAILED;
        return restResult(data, aec);
    }

    public static R failed(Object data, String msg) {
        ApiErrorCode aec = ApiErrorCode.FAILED;
        return restResult(data, aec.getCode(), msg);
    }

    public static R ok(Object data) {
        ApiErrorCode aec = ApiErrorCode.SUCCESS;
        if (data instanceof Boolean && Boolean.FALSE.equals(data)) {
            aec = ApiErrorCode.FAILED;
        }
        return restResult(data, aec);
    }

    public static R restResult(Object data, IErrorCode errorCode) {
        return restResult(data, errorCode.getCode(), errorCode.getMsg());
    }

    private static R restResult(Object data, long code, String msg) {
        R r = new R();
        r.setCode(code);
        r.setData(data);
        r.setMsg(msg);
        return r;
    }

    public static R ok(String msg, Object data) {
        ApiErrorCode aec = ApiErrorCode.SUCCESS;
        R r = new R();
        r.setCode(aec.getCode());
        r.setData(data);
        r.setMsg(msg);
        return r;

    }
}