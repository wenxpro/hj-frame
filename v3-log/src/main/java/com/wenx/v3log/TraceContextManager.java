package com.wenx.v3log;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的Trace上下文管理器
 * 
 * <p>提供统一的trace上下文管理，支持MDC和Jaeger的集成</p>
 * <p>管理请求级别的trace信息，包括用户认证、租户、组织等信息</p>
 * 
 * @author wenx
 * @version 1.0
 */
@Slf4j
public class TraceContextManager {

    /**
     * MDC键名常量
     */
    public static final String REQUEST_ID = "request-id";
    public static final String TRACE_ID = "trace-id";
    public static final String SPAN_ID = "span-id";
    public static final String USER_ID = "user-id";
    public static final String USERNAME = "username";
    public static final String TENANT_ID = "tenant-id";
    public static final String DEPT_ID = "department-id";
    public static final String CLIENT_IP = "client-ip";
    public static final String REQUEST_START_TIME = "request-start-time";
    public static final String USER_ROLES = "user-roles";
    public static final String USER_PERMISSIONS = "user-permissions";
    
    /**
     * 线程本地存储，用于存储当前线程的trace上下文
     */
    private static final ThreadLocal<Map<String, String>> TRACE_CONTEXT = new ThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };
    
    /**
     * 初始化trace上下文
     * 
     * @param traceId 追踪ID
     * @param spanId Span ID
     * @param userId 用户ID
     * @param username 用户名
     * @param tenantId 租户ID
     * @param clientIp 客户端IP
     */
    public static void initTraceContext(String traceId, String spanId, String userId, 
                                      String username, String tenantId, String clientIp) {
        try {
            // 清理现有上下文
            clearTraceContext();
            
            // 设置基本trace信息
            if (StrUtil.isNotBlank(traceId)) {
                setContextValue(REQUEST_ID, traceId);
                setContextValue(TRACE_ID, traceId);
                MDC.put(REQUEST_ID, traceId);
                MDC.put(TRACE_ID, traceId);
            }
            
            if (StrUtil.isNotBlank(spanId)) {
                setContextValue(SPAN_ID, spanId);
                MDC.put(SPAN_ID, spanId);
            }
            
            // 设置用户认证信息
            if (StrUtil.isNotBlank(userId)) {
                setContextValue(USER_ID, userId);
                MDC.put(USER_ID, userId);
            }
            
            if (StrUtil.isNotBlank(username)) {
                setContextValue(USERNAME, username);
                MDC.put(USERNAME, username);
            }
            
            if (StrUtil.isNotBlank(tenantId)) {
                setContextValue(TENANT_ID, tenantId);
                MDC.put(TENANT_ID, tenantId);
            }
            
            if (StrUtil.isNotBlank(clientIp)) {
                setContextValue(CLIENT_IP, clientIp);
                MDC.put(CLIENT_IP, clientIp);
            }
            
            // 设置请求开始时间
            String startTime = String.valueOf(System.currentTimeMillis());
            setContextValue(REQUEST_START_TIME, startTime);
            MDC.put(REQUEST_START_TIME, startTime);
            
            log.debug("Trace上下文初始化完成 - TraceId: {}, UserId: {}, TenantId: {}", traceId, userId, tenantId);
            
        } catch (Exception e) {
            log.error("初始化trace上下文失败", e);
        }
    }
    
    /**
     * 设置用户角色信息
     * 
     * @param roles 用户角色（逗号分隔）
     */
    public static void setUserRoles(String roles) {
        if (StrUtil.isNotBlank(roles)) {
            setContextValue(USER_ROLES, roles);
            MDC.put(USER_ROLES, roles);
        }
    }
    
    /**
     * 设置用户权限信息
     * 
     * @param permissions 用户权限（逗号分隔）
     */
    public static void setUserPermissions(String permissions) {
        if (StrUtil.isNotBlank(permissions)) {
            setContextValue(USER_PERMISSIONS, permissions);
            MDC.put(USER_PERMISSIONS, permissions);
        }
    }
    
    /**
     * 获取当前trace ID
     * 
     * @return trace ID
     */
    public static String getTraceId() {
        // 优先从Jaeger获取
        String jaegerTraceId = JaegerTraceUtil.getCurrentJaegerTraceId();
        if (StrUtil.isNotBlank(jaegerTraceId)) {
            return jaegerTraceId;
        }
        
        // 从MDC获取
        String mdcTraceId = MDC.get(TRACE_ID);
        if (StrUtil.isNotBlank(mdcTraceId)) {
            return mdcTraceId;
        }
        
        // 从线程本地存储获取
        return getContextValue(TRACE_ID);
    }
    
    /**
     * 获取当前span ID
     * 
     * @return span ID
     */
    public static String getSpanId() {
        // 优先从Jaeger获取
        String jaegerSpanId = JaegerTraceUtil.getCurrentJaegerSpanId();
        if (StrUtil.isNotBlank(jaegerSpanId)) {
            return jaegerSpanId;
        }
        
        // 从MDC获取
        String mdcSpanId = MDC.get(SPAN_ID);
        if (StrUtil.isNotBlank(mdcSpanId)) {
            return mdcSpanId;
        }
        
        // 从线程本地存储获取
        return getContextValue(SPAN_ID);
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     */
    public static String getUserId() {
        String userId = MDC.get(USER_ID);
        return StrUtil.isNotBlank(userId) ? userId : getContextValue(USER_ID);
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 用户名
     */
    public static String getUsername() {
        String username = MDC.get(USERNAME);
        return StrUtil.isNotBlank(username) ? username : getContextValue(USERNAME);
    }
    
    /**
     * 获取当前租户ID
     * 
     * @return 租户ID
     */
    public static String getTenantId() {
        String tenantId = MDC.get(TENANT_ID);
        return StrUtil.isNotBlank(tenantId) ? tenantId : getContextValue(TENANT_ID);
    }
    
    /**
     * 获取当前组织ID
     * 
     * @return 组织ID
     */
    public static String getDeptId() {
        String departmentId = MDC.get(DEPT_ID);
        return StrUtil.isNotBlank(departmentId) ? departmentId : getContextValue(DEPT_ID);
    }
    
    /**
     * 获取客户端IP
     * 
     * @return 客户端IP
     */
    public static String getClientIp() {
        String clientIp = MDC.get(CLIENT_IP);
        return StrUtil.isNotBlank(clientIp) ? clientIp : getContextValue(CLIENT_IP);
    }
    
    /**
     * 获取请求开始时间
     * 
     * @return 请求开始时间（毫秒时间戳）
     */
    public static Long getRequestStartTime() {
        String startTime = MDC.get(REQUEST_START_TIME);
        if (StrUtil.isBlank(startTime)) {
            startTime = getContextValue(REQUEST_START_TIME);
        }
        
        if (StrUtil.isNotBlank(startTime)) {
            try {
                return Long.parseLong(startTime);
            } catch (NumberFormatException e) {
                log.debug("解析请求开始时间失败: {}", startTime);
            }
        }
        
        return null;
    }
    
    /**
     * 获取用户角色
     * 
     * @return 用户角色
     */
    public static String getUserRoles() {
        String roles = MDC.get(USER_ROLES);
        return StrUtil.isNotBlank(roles) ? roles : getContextValue(USER_ROLES);
    }
    
    /**
     * 获取用户权限
     * 
     * @return 用户权限
     */
    public static String getUserPermissions() {
        String permissions = MDC.get(USER_PERMISSIONS);
        return StrUtil.isNotBlank(permissions) ? permissions : getContextValue(USER_PERMISSIONS);
    }
    
    /**
     * 获取完整的trace上下文信息
     * 
     * @return trace上下文Map
     */
    public static Map<String, String> getFullTraceContext() {
        Map<String, String> context = new HashMap<>();
        
        // 从MDC获取所有信息
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        if (mdcContext != null) {
            context.putAll(mdcContext);
        }
        
        // 从线程本地存储获取信息
        Map<String, String> localContext = TRACE_CONTEXT.get();
        if (localContext != null) {
            context.putAll(localContext);
        }
        
        // 添加Jaeger信息
        String jaegerTraceId = JaegerTraceUtil.getCurrentJaegerTraceId();
        if (StrUtil.isNotBlank(jaegerTraceId)) {
            context.put("jaeger.trace.id", jaegerTraceId);
        }
        
        String jaegerSpanId = JaegerTraceUtil.getCurrentJaegerSpanId();
        if (StrUtil.isNotBlank(jaegerSpanId)) {
            context.put("jaeger.span.id", jaegerSpanId);
        }
        
        return context;
    }
    
    /**
     * 创建trace上下文摘要字符串
     * 
     * @return 上下文摘要
     */
    public static String getTraceContextSummary() {
        return String.format("[%s|%s|%s|%s|%s]",
                StrUtil.blankToDefault(getTraceId(), "unknown"),
                StrUtil.blankToDefault(getUserId(), "anonymous"),
                StrUtil.blankToDefault(getUsername(), "unknown"),
                StrUtil.blankToDefault(getTenantId(), "default"),
                StrUtil.blankToDefault(getClientIp(), "unknown")
        );
    }
    
    /**
     * 清理trace上下文
     */
    public static void clearTraceContext() {
        try {
            // 清理MDC
            MDC.clear();
            
            // 清理线程本地存储
            TRACE_CONTEXT.remove();
            
            log.debug("Trace上下文已清理");
            
        } catch (Exception e) {
            log.error("清理trace上下文失败", e);
        }
    }
    
    /**
     * 设置上下文值
     * 
     * @param key 键
     * @param value 值
     */
    private static void setContextValue(String key, String value) {
        if (StrUtil.isNotBlank(key) && value != null) {
            TRACE_CONTEXT.get().put(key, value);
        }
    }
    
    /**
     * 获取上下文值
     * 
     * @param key 键
     * @return 值
     */
    private static String getContextValue(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return TRACE_CONTEXT.get().get(key);
    }
    
    /**
     * 复制当前trace上下文到新线程
     * 
     * @return 上下文副本
     */
    public static Map<String, String> copyTraceContext() {
        Map<String, String> context = new HashMap<>();
        
        // 复制MDC
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        if (mdcContext != null) {
            context.putAll(mdcContext);
        }
        
        // 复制线程本地存储
        Map<String, String> localContext = TRACE_CONTEXT.get();
        if (localContext != null) {
            context.putAll(localContext);
        }
        
        return context;
    }
    
    /**
     * 恢复trace上下文（用于异步线程）
     * 
     * @param context 上下文副本
     */
    public static void restoreTraceContext(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            return;
        }
        
        try {
            // 清理现有上下文
            clearTraceContext();
            
            // 恢复MDC
            for (Map.Entry<String, String> entry : context.entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
                setContextValue(entry.getKey(), entry.getValue());
            }
            
            log.debug("Trace上下文已恢复 - TraceId: {}", context.get(TRACE_ID));
            
        } catch (Exception e) {
            log.error("恢复trace上下文失败", e);
        }
    }
}