package com.wenx.v3core.error;

import lombok.Getter;

/**
 * 服务器内部相关异常枚举
 *
 * @author wenx
 */
@Getter
public enum ServerExceptionEnum {

    /**
     * 当前请求参数为空或数据缺失
     */
    REQUEST_EMPTY("当前请求参数为空或数据缺失，请联系管理员"),

    /**
     * 服务器出现未知异常
     */
    SERVER_ERROR("服务器出现异常，请联系管理员"),

    /**
     * 常量获取存在空值
     */
    CONSTANT_EMPTY("常量获取存在空值，请检查config中是否配置");


    private final String message;

    ServerExceptionEnum(String message) {
        this.message = message;
    }

}