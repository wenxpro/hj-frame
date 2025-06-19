package com.wenx.v3core.consts;

/**
 * 配置顺序 顺序越小越靠前
 *
 * @author wenx
 * @date 2021-03-15
 */
public interface SortConstant {

    /**
     * 接口权限
     */
    int REQUIRED_ORDER = -100;


    /**
     * 全局异常拦截器
     */
    int GLOBAL_ERROR_ORDER = -120;

    /**
     * 过滤器
     */
    int WEB_FILTER_ORDER = -200;

    /**
     * cors
     */
    int CORS_ORDER = -250;
}