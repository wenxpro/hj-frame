package com.wenx.v3core.consts;

import lombok.Getter;

/**
 * 应用类型 枚举
 *
 * @author wenx
 */
@Getter
public enum AppTypeEnums {

    WEB(0, "PC网页应用"),
    SERVICE(1, "服务应用"),
    APP(2, "手机应用"),
    WAP(3, "手机网页应用"),
    MINI(4, "小程序应用");

    private final Integer code;
    private final String label;

    AppTypeEnums(Integer code, String value) {
        this.code = code;
        this.label = value;
    }
}
