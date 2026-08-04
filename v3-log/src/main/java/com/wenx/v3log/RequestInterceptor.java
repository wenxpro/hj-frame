package com.wenx.v3log;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;

/**
 * 请求拦截器 - 业务请求上下文管理（log 标准化改造）
 *
 * <p>与 ControllerAspect 的职责分工：</p>
 * <ul>
 *   <li>RequestInterceptor：负责业务请求 ID 生成、client-ip / 开始时间等 MDC 上下文</li>
 *   <li>ControllerAspect：负责 HTTP 请求日志记录、性能监控</li>
 * </ul>
 *
 * <p>链路追踪（traceId/spanId）不再由此处理：由 Micrometer Tracing 标准方案
 * 自动写入 MDC（键 traceId/spanId）并通过 W3C traceparent 跨服务传播（log-starter）。</p>
 *
 * @author wenx
 * @version 4.0
 */
@Slf4j
public class RequestInterceptor implements HandlerInterceptor {

    /**
     * 业务请求 ID 的 MDC 键名
     */
    public static final String REQUEST_ID_KEY = "request-id";

    /**
     * 请求开始时间的 MDC 键名
     */
    public static final String REQUEST_START_TIME_KEY = "request-start-time";

    /**
     * 客户端IP的MDC键名
     */
    public static final String CLIENT_IP_KEY = "client-ip";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 业务请求 ID（支持跨服务透传的 header 与参数）
        String requestId = generateRequestId(request);

        // 获取客户端IP
        String clientIp = extractClientIp(request);

        // 设置MDC上下文
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(REQUEST_START_TIME_KEY, String.valueOf(System.currentTimeMillis()));
        MDC.put(CLIENT_IP_KEY, clientIp);

        if (log.isDebugEnabled()) {
            log.debug("请求上下文 - ID: {}, IP: {}, URI: {}", requestId, clientIp, request.getRequestURI());
        }

        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            String requestId = MDC.get(REQUEST_ID_KEY);

            // 业务请求 ID 回写响应头，便于联调排查
            if (StrUtil.isNotBlank(requestId)) {
                response.addHeader(REQUEST_ID_KEY, requestId);
            }
        } finally {
            // 清理MDC上下文，避免内存泄漏（traceId 等由 micrometer 统一管理）
            MDC.clear();
        }
    }

    /**
     * 生成或获取业务请求 ID
     */
    private String generateRequestId(HttpServletRequest request) {
        // 优先从请求头透传（跨服务保留同一业务请求 ID）
        String headerId = request.getHeader(REQUEST_ID_KEY);
        if (StrUtil.isNotBlank(headerId)) {
            return headerId;
        }
        String paramId = request.getParameter(REQUEST_ID_KEY);
        if (StrUtil.isNotBlank(paramId)) {
            return paramId;
        }
        return generateNewRequestId();
    }

    /**
     * 生成新的业务请求 ID
     * 格式: yyyyMMddHHmmssSSS + 5位随机字符
     */
    private String generateNewRequestId() {
        String timestamp = DateUtil.format(new Date(), "yyyyMMddHHmmssSSS");
        String randomSuffix = IdUtil.simpleUUID().substring(0, 5).toUpperCase();
        return timestamp + randomSuffix;
    }

    /**
     * 提取客户端 IP
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 获取当前业务请求 ID
     */
    public static String getCurrentRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }

    /**
     * 获取当前客户端 IP
     */
    public static String getCurrentClientIp() {
        return MDC.get(CLIENT_IP_KEY);
    }

    /**
     * 获取当前请求开始时间
     */
    public static long getCurrentStartTime() {
        String startTime = MDC.get(REQUEST_START_TIME_KEY);
        if (StrUtil.isBlank(startTime)) {
            return 0L;
        }
        try {
            return Long.parseLong(startTime);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
