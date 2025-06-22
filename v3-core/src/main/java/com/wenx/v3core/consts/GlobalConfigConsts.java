package com.wenx.v3core.consts;

/**
 * @author wenx
 * @description 全局配置常量
 */
public interface GlobalConfigConsts {

    /**
     * component scan
     */
    String SCAN_PACKAGES = "com.wenx";

    /**
     * mapper scan
     */
    String MAPPER_PACKAGES = "com.wenx.**.mapper";

    /**
     * openfeign scan
     */
    String FEIGN_SCAN = "com.wenx.v3openfeign";
}
