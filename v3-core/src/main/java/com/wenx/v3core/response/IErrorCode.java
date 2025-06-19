package com.wenx.v3core.response;

/**
 * 错误接口
 *
 * @author wenx
 */
public interface IErrorCode {

    /**
     * 错误编码 -1、失败 0、成功
     *
     * @return
     */
    long getCode();

    /**
     * 错误描述
     *
     * @return
     */
    String getMsg();
}