package com.wenx.v3core.aspect;

import com.alibaba.fastjson2.JSON;
import com.wenx.v3core.intercepter.RequestInterceptor;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Controller层HTTP请求监控切面
 * 
 * <p>核心职责：</p>
 * <ul>
 *   <li>记录HTTP请求详细日志</li>
 *   <li>监控Controller方法性能</li>
 *   <li>记录请求参数和响应信息</li>
 *   <li>性能预警和分析</li>
 * </ul>
 * 
 * <p>与RequestInterceptor的职责分工：</p>
 * <ul>
 *   <li>RequestInterceptor：负责追踪ID生成、MDC管理</li>
 *   <li>ControllerAspect：负责HTTP请求日志记录、性能监控</li>
 * </ul>
 * 
 * @author wenx
 * @version 2.0
 */
@Aspect
@Component
@Slf4j
public class ControllerAspect {

    /**
     * 定义Controller层方法切点
     * 只拦截业务包下的Controller类
     */
    @Pointcut("execution(* com.wenx..*Controller.*(..))")
    public void controllerMethod() {
    }

    /**
     * 环绕通知：监控Controller方法执行
     */
    @Around("controllerMethod()")
    public Object monitorControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 记录切面生效（仅在debug级别）
        String methodSignature = buildMethodSignature(joinPoint);
        if (log.isDebugEnabled()) {
            log.debug("[AOP] Controller切面生效: {}", methodSignature);
        }
        
        // 执行方法
        Object result = joinPoint.proceed();
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // 处理请求日志记录
        handleRequestLogging(joinPoint, result, executionTime);
        
        return result;
    }

    /**
     * 处理请求日志记录
     */
    private void handleRequestLogging(ProceedingJoinPoint joinPoint, Object result, long executionTime) {
        ServletRequestAttributes attributes = getRequestAttributes();
        
        if (attributes != null) {
            // HTTP请求场景
            HttpRequestLog requestLog = buildHttpRequestLog(attributes.getRequest(), joinPoint, result, executionTime);
            logHttpRequest(requestLog);
        } else {
            // 非HTTP请求的Controller方法调用（如内部调用）
            logNonHttpRequest(joinPoint, executionTime);
        }
    }

    /**
     * 构建HTTP请求日志对象
     */
    private HttpRequestLog buildHttpRequestLog(HttpServletRequest request, ProceedingJoinPoint joinPoint, 
                                             Object result, long executionTime) {
        HttpRequestLog requestLog = new HttpRequestLog();
        
        // 基本请求信息
        requestLog.setUrl(request.getRequestURL().toString());
        requestLog.setMethod(request.getMethod());
        requestLog.setAction(buildMethodSignature(joinPoint));
        requestLog.setExecutionTime(executionTime);
        
        // 从MDC获取追踪信息（由RequestInterceptor设置）
        requestLog.setRequestId(RequestInterceptor.getCurrentRequestId());
        requestLog.setUserId(RequestInterceptor.getCurrentUserId());
        requestLog.setClientIp(RequestInterceptor.getCurrentClientIp());
        
        // 请求参数序列化
        requestLog.setParams(serializeMethodArgs(joinPoint.getArgs()));
        
        // 用户代理信息
        requestLog.setUserAgent(request.getHeader("User-Agent"));
        
        // 性能分析
        requestLog.setPerformanceLevel(determinePerformanceLevel(executionTime));
        
        // 计算总请求耗时（从RequestInterceptor开始计算）
        long totalTime = calculateTotalRequestTime();
        if (totalTime > 0) {
            requestLog.setTotalTime(totalTime);
        }
        
        return requestLog;
    }

    /**
     * 记录HTTP请求日志
     */
    private void logHttpRequest(HttpRequestLog requestLog) {
        switch (requestLog.getPerformanceLevel()) {
            case CRITICAL -> log.error("[HTTP] {}", requestLog);
            case WARNING -> log.warn("[HTTP] {}", requestLog);
            case NORMAL -> log.info("[HTTP] {}", requestLog);
            case FAST -> {
                if (log.isDebugEnabled()) {
                    log.debug("[HTTP] {}", requestLog);
                }
            }
        }
    }

    /**
     * 记录非HTTP请求日志
     */
    private void logNonHttpRequest(ProceedingJoinPoint joinPoint, long executionTime) {
        String methodSignature = buildMethodSignature(joinPoint);
        PerformanceLevel level = determinePerformanceLevel(executionTime);
        
        switch (level) {
            case CRITICAL -> log.error("[CONTROLLER] {} 执行时间: {}ms (严重性能问题)", methodSignature, executionTime);
            case WARNING -> log.warn("[CONTROLLER] {} 执行时间: {}ms (性能警告)", methodSignature, executionTime);
            case NORMAL -> log.info("[CONTROLLER] {} 执行时间: {}ms", methodSignature, executionTime);
            case FAST -> {
                if (log.isDebugEnabled()) {
                    log.debug("[CONTROLLER] {} 执行时间: {}ms", methodSignature, executionTime);
                }
            }
        }
    }

    /**
     * 确定性能级别
     */
    private PerformanceLevel determinePerformanceLevel(long executionTime) {
        if (executionTime > 5000) {
            return PerformanceLevel.CRITICAL;
        } else if (executionTime > 1000) {
            return PerformanceLevel.WARNING;
        } else if (executionTime > 500) {
            return PerformanceLevel.NORMAL;
        } else {
            return PerformanceLevel.FAST;
        }
    }

    /**
     * 计算总请求耗时
     */
    private long calculateTotalRequestTime() {
        long startTime = RequestInterceptor.getCurrentStartTime();
        if (startTime > 0) {
            return System.currentTimeMillis() - startTime;
        }
        return 0L;
    }

    /**
     * 构建方法签名字符串
     */
    private String buildMethodSignature(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        // 简化类名显示
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        return simpleClassName + "." + methodName;
    }

    /**
     * 获取请求属性
     */
    private ServletRequestAttributes getRequestAttributes() {
        try {
            return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 序列化方法参数
     * 过滤不可序列化的对象
     */
    private String serializeMethodArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            Object[] filteredArgs = Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(this::isSerializable)
                .toArray();
            
            return JSON.toJSONString(filteredArgs);
        } catch (Exception e) {
            log.debug("参数序列化失败: {}", e.getMessage());
            return "[序列化失败]";
        }
    }

    /**
     * 判断对象是否可序列化
     * 排除Servlet相关对象和文件上传对象
     */
    private boolean isSerializable(Object arg) {
        if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
            return false;
        }
        
        if (arg instanceof MultipartFile) {
            return false;
        }
        
        // 处理文件列表
        if (arg instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof MultipartFile) {
            return false;
        }
        
        return true;
    }

    /**
     * 性能级别枚举
     */
    public enum PerformanceLevel {
        FAST,      // < 500ms
        NORMAL,    // 500ms - 1s
        WARNING,   // 1s - 5s
        CRITICAL   // > 5s
    }

    /**
     * HTTP请求日志数据对象
     */
    @Data
    public static class HttpRequestLog {
        /** 请求URL */
        private String url;
        /** HTTP方法 */
        private String method;
        /** 执行的Controller方法 */
        private String action;
        /** 请求参数 */
        private String params;
        /** 请求追踪ID */
        private String requestId;
        /** 用户ID */
        private String userId;
        /** 客户端IP */
        private String clientIp;
        /** Controller方法执行时间 */
        private long executionTime;
        /** 总请求时间（从拦截器开始） */
        private long totalTime;
        /** 用户代理 */
        private String userAgent;
        /** 性能级别 */
        private PerformanceLevel performanceLevel;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            // 基本信息
            sb.append(method).append(" ").append(url);
            sb.append(" | ").append(action);
            
            // 追踪信息
            if (StringUtils.hasText(requestId)) {
                sb.append(" | ID: ").append(requestId);
            }
            if (StringUtils.hasText(userId)) {
                sb.append(" | 用户: ").append(userId);
            }
            if (StringUtils.hasText(clientIp)) {
                sb.append(" | IP: ").append(clientIp);
            }
            
            // 性能信息
            sb.append(" | 执行: ").append(executionTime).append("ms");
            if (totalTime > 0 && totalTime != executionTime) {
                sb.append(" | 总耗时: ").append(totalTime).append("ms");
            }
            
            // 性能警告标记
            switch (performanceLevel) {
                case CRITICAL -> sb.append(" | 严重性能问题");
                case WARNING -> sb.append(" | 性能警告");
                case NORMAL -> sb.append(" | 耗时较长");
            }
            
            // 请求参数
            if (StringUtils.hasText(params) && !"[]".equals(params)) {
                sb.append(" | 参数: ").append(params);
            }
            
            return sb.toString();
        }
    }
}