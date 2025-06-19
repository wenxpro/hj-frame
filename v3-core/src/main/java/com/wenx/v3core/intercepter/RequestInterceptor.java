package com.wenx.v3core.intercepter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;

/**
 * @author wenx
 * @description 请求拦截器
 */
@Slf4j
public class RequestInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_KEY = "request-id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MDC.put(REQUEST_ID_KEY, getRequestId(request));
//        log.info("request-id interceptor generating...");
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 把requestId添加到响应头，以便其它应用使用
        response.addHeader(REQUEST_ID_KEY, MDC.get(REQUEST_ID_KEY));
        // 请求完成，从MDC中移除requestId
        MDC.remove(REQUEST_ID_KEY);
    }

    public static String getRequestId(HttpServletRequest request) {
        String requestId;
        String parameterRequestId = request.getParameter(REQUEST_ID_KEY);
        String headerRequestId = request.getHeader(REQUEST_ID_KEY);
        // 根据请求参数或请求头判断是否有“request-id”，有则使用，无则创建
        if (parameterRequestId == null && headerRequestId == null) {
            requestId = DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + IdUtil.simpleUUID().substring(0, 5);
        } else {
            requestId = parameterRequestId != null ? parameterRequestId : headerRequestId;
        }
        return requestId;
    }
}
