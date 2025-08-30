package com.wenx.v3log;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 请求拦截器 - 专注于链路追踪ID管理
 * <p>与ControllerAspect的职责分工：</p>
 * <ul>
 *   <li>RequestInterceptor：负责追踪ID生成、MDC管理</li>
 *   <li>ControllerAspect：负责HTTP请求日志记录、性能监控</li>
 * </ul>
 * 
 * @author wenx
 * @version 3.0
 */
@Slf4j
public class RequestInterceptor implements HandlerInterceptor {

    /**
     * 请求ID的MDC键名
     */
    public static final String REQUEST_ID_KEY = "request-id";
    
    /**
     * 请求开始时间的MDC键名
     */
    public static final String REQUEST_START_TIME_KEY = "request-start-time";
    
    /**
     * 用户ID的MDC键名
     */
    public static final String USER_ID_KEY = "user-id";
    
    /**
     * 客户端IP的MDC键名
     */
    public static final String CLIENT_IP_KEY = "client-ip";
    
    /**
     * 支持的追踪ID请求头名称（按优先级排序）
     */
    private static final String[] TRACE_HEADERS = {
        REQUEST_ID_KEY,
        "X-Trace-Id",
        "X-Request-Id", 
        "traceId",
        "trace-id"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 生成或获取请求追踪ID
        String requestId = generateRequestId(request);
        
        // 获取用户信息（如果存在）
        String userId = extractUserId(request);
        
        // 获取客户端IP
        String clientIp = extractClientIp(request);
        
        // 设置MDC上下文
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(REQUEST_START_TIME_KEY, String.valueOf(System.currentTimeMillis()));
        MDC.put(CLIENT_IP_KEY, clientIp);
        
        if (StrUtil.isNotBlank(userId)) {
            MDC.put(USER_ID_KEY, userId);
        }
        
        // 设置Jaeger trace信息到MDC（如果可用）
        JaegerTraceUtil.setMDCTraceInfo();
        
        // 简单的追踪日志（仅DEBUG级别）
        if (log.isDebugEnabled()) {
            log.debug("链路追踪开始 - ID: {}, IP: {}, URI: {}", requestId, clientIp, request.getRequestURI());
        }
        
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        try {
            String requestId = MDC.get(REQUEST_ID_KEY);
            
            // 将追踪ID添加到响应头，支持跨服务调用
            if (StrUtil.isNotBlank(requestId)) {
                response.addHeader(REQUEST_ID_KEY, requestId);
                response.addHeader("X-Trace-Id", requestId);
            }
            
            // 异常情况的简单记录（详细日志由ControllerAspect处理）
            if (ex != null && log.isDebugEnabled()) {
                log.debug("链路追踪异常完成 - ID: {}, 异常: {}", requestId, ex.getClass().getSimpleName());
            } else if (log.isDebugEnabled()) {
                log.debug("链路追踪正常完成 - ID: {}", requestId);
            }
            
        } finally {
            // 清理MDC上下文，避免内存泄漏
            MDC.clear();
        }
    }

    /**
     * 生成或获取请求追踪ID
     * 
     * @param request HTTP请求对象
     * @return 请求追踪ID
     */
    private String generateRequestId(HttpServletRequest request) {
        // 优先从请求头中获取现有的追踪ID（支持链路传递）
        for (String headerName : TRACE_HEADERS) {
            String traceId = request.getHeader(headerName);
            if (StrUtil.isNotBlank(traceId)) {
                log.debug("使用现有追踪ID: {} = {}", headerName, traceId);
                return traceId;
            }
        }
        
        // 从请求参数中获取
        String paramTraceId = request.getParameter(REQUEST_ID_KEY);
        if (StrUtil.isNotBlank(paramTraceId)) {
            log.debug("使用参数中的追踪ID: {}", paramTraceId);
            return paramTraceId;
        }
        
        // 生成新的追踪ID
        String newRequestId = generateNewRequestId();
        log.debug("生成新追踪ID: {}", newRequestId);
        return newRequestId;
    }
    
    /**
     * 生成新的请求ID
     * 格式: yyyyMMddHHmmssSSS + 5位随机字符
     * 
     * @return 新的请求ID
     */
    private String generateNewRequestId() {
        String timestamp = DateUtil.format(new Date(), "yyyyMMddHHmmssSSS");
        String randomSuffix = IdUtil.simpleUUID().substring(0, 5).toUpperCase();
        return timestamp + randomSuffix;
    }
    
    /**
     * 提取用户ID（从请求头或参数中）
     * 
     * @param request HTTP请求对象
     * @return 用户ID，可能为null
     */
    private String extractUserId(HttpServletRequest request) {
        // 尝试从多个可能的位置获取用户ID
        String[] userIdSources = {
            request.getHeader("X-User-Id"),
            request.getHeader("userId"),
            request.getParameter("userId")
        };
        
        for (String source : userIdSources) {
            if (StrUtil.isNotBlank(source)) {
                return source;
            }
        }
        
        // JWT解析逻辑
        String authorization = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(authorization) && authorization.startsWith("Bearer ")) {
            try {
                return parseUserIdFromJWT(authorization.substring(7));
            } catch (Exception e) {
                log.warn("JWT解析失败: {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 提取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String extractClientIp(HttpServletRequest request) {
        String[] ipHeaders = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 多级代理的情况，取第一个IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                if (isValidIp(ip)) {
                    return ip;
                }
            }
        }
        
        // 最后尝试从RemoteAddr获取
        String remoteAddr = request.getRemoteAddr();
        return StrUtil.isNotBlank(remoteAddr) ? remoteAddr : "unknown";
    }
    
    /**
     * 验证IP地址格式是否有效
     * 
     * @param ip IP地址字符串
     * @return 是否为有效IP
     */
    private boolean isValidIp(String ip) {
        if (StrUtil.isBlank(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return false;
        }
        
        // 简单的IPv4格式验证
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            
            try {
                for (String part : parts) {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        return false;
                    }
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // IPv6或其他格式认为有效（简化处理）
        return true;
    }
    
    /**
     * 静态方法：获取当前请求的追踪ID
     * 
     * @return 当前请求的追踪ID，如果不存在则返回null
     */
    public static String getCurrentRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }
    
    /**
     * 静态方法：获取当前请求的用户ID
     * 
     * @return 当前请求的用户ID，如果不存在则返回null
     */
    public static String getCurrentUserId() {
        return MDC.get(USER_ID_KEY);
    }
    
    /**
     * 静态方法：获取当前请求的客户端IP
     * 
     * @return 当前请求的客户端IP，如果不存在则返回null
     */
    public static String getCurrentClientIp() {
        return MDC.get(CLIENT_IP_KEY);
    }
    
    /**
     * 静态方法：获取当前请求的开始时间
     * 
     * @return 当前请求的开始时间戳，如果不存在则返回0
     */
    public static long getCurrentStartTime() {
        String startTimeStr = MDC.get(REQUEST_START_TIME_KEY);
        if (StrUtil.isNotBlank(startTimeStr)) {
            try {
                return Long.parseLong(startTimeStr);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
    
    /**
     * 从JWT token中解析用户ID
     * 
     * @param token JWT token字符串
     * @return 用户ID，解析失败返回null
     */
    private String parseUserIdFromJWT(String token) {
        try {
            // 注意：这里使用了一个默认的密钥，实际项目中应该从配置中获取
            // 或者使用公钥验证（如果使用RSA算法）
            String secretKey = "v3-cloud-jwt-secret-key-for-user-authentication-2024";
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // 尝试从多个可能的字段中获取用户ID
            Object userId = claims.get("userId");
            if (userId != null) {
                return userId.toString();
            }
            
            userId = claims.get("user_id");
            if (userId != null) {
                return userId.toString();
            }
            
            userId = claims.get("sub"); // JWT标准的subject字段
            if (userId != null) {
                return userId.toString();
            }
            
            userId = claims.get("id");
            if (userId != null) {
                return userId.toString();
            }
            
            log.debug("JWT中未找到用户ID字段，可用字段: {}", claims.keySet());
            return null;
            
        } catch (Exception e) {
            log.debug("JWT解析异常: {}", e.getMessage());
            return null;
        }
    }
}
