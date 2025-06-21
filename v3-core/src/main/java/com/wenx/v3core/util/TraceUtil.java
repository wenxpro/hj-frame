package com.wenx.v3core.util;

import com.wenx.v3core.intercepter.RequestInterceptor;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 链路追踪工具类
 * 
 * <p>提供便捷的方法来获取当前请求的追踪信息</p>
 * <p>专注于MDC信息获取，不包含日志记录功能（由切面负责）</p>
 * 
 * @author wenx
 * @version 2.0
 */
@Slf4j
public class TraceUtil {

    /**
     * 获取当前请求的追踪ID
     * 
     * @return 追踪ID，如果不存在则返回"unknown"
     */
    public static String getRequestId() {
        String requestId = RequestInterceptor.getCurrentRequestId();
        return StrUtil.isNotBlank(requestId) ? requestId : "unknown";
    }
    
    /**
     * 获取当前请求的用户ID
     * 
     * @return 用户ID，如果不存在则返回"anonymous"
     */
    public static String getUserId() {
        String userId = RequestInterceptor.getCurrentUserId();
        return StrUtil.isNotBlank(userId) ? userId : "anonymous";
    }
    
    /**
     * 获取当前请求的客户端IP
     * 
     * @return 客户端IP，如果不存在则返回"unknown"
     */
    public static String getClientIp() {
        String clientIp = RequestInterceptor.getCurrentClientIp();
        return StrUtil.isNotBlank(clientIp) ? clientIp : "unknown";
    }
    
    /**
     * 获取当前请求的开始时间
     * 
     * @return 开始时间戳，如果不存在则返回0
     */
    public static long getStartTime() {
        return RequestInterceptor.getCurrentStartTime();
    }
    
    /**
     * 获取当前请求的执行时长（毫秒）
     * 
     * @return 执行时长，如果无法计算则返回-1
     */
    public static long getDuration() {
        long startTime = getStartTime();
        if (startTime > 0) {
            return System.currentTimeMillis() - startTime;
        }
        return -1L;
    }
    
    /**
     * 创建包含追踪信息的上下文字符串
     * 格式: [requestId|userId|clientIp]
     * 
     * @return 追踪上下文字符串
     */
    public static String getTraceContext() {
        return String.format("[%s|%s|%s]", getRequestId(), getUserId(), getClientIp());
    }
    
    /**
     * 创建简化的追踪信息字符串
     * 格式: requestId@userId
     * 
     * @return 简化追踪信息
     */
    public static String getSimpleTrace() {
        return getRequestId() + "@" + getUserId();
    }
    
    /**
     * 判断当前是否在HTTP请求上下文中
     * 
     * @return 是否存在请求追踪信息
     */
    public static boolean isInRequestContext() {
        return StrUtil.isNotBlank(RequestInterceptor.getCurrentRequestId());
    }
    
    /**
     * 获取追踪信息摘要（用于日志输出）
     * 
     * @return 追踪信息摘要
     */
    public static String getTraceSummary() {
        if (!isInRequestContext()) {
            return "无追踪上下文";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("追踪ID: ").append(getRequestId());
        
        String userId = getUserId();
        if (!"anonymous".equals(userId)) {
            summary.append(", 用户: ").append(userId);
        }
        
        summary.append(", IP: ").append(getClientIp());
        
        long duration = getDuration();
        if (duration > 0) {
            summary.append(", 耗时: ").append(duration).append("ms");
        }
        
        return summary.toString();
    }
} 