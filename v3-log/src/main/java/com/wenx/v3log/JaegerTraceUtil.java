package com.wenx.v3log;

import cn.hutool.core.util.StrUtil;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Jaeger链路追踪工具类
 * 
 * <p>提供统一的Jaeger trace和span管理功能</p>
 * <p>与现有的RequestInterceptor协同工作，优先使用Jaeger的trace ID</p>
 * 
 * @author wenx
 * @version 1.0
 */
@Slf4j
@Component
public class JaegerTraceUtil {

    /**
     * Jaeger trace ID的MDC键名
     */
    public static final String JAEGER_TRACE_ID_KEY = "jaeger-trace-id";
    
    /**
     * Jaeger span ID的MDC键名
     */
    public static final String JAEGER_SPAN_ID_KEY = "jaeger-span-id";
    
    /**
     * 统一的trace ID键名（优先使用Jaeger）
     */
    public static final String UNIFIED_TRACE_ID_KEY = "trace-id";
    
    /**
     * 统一的span ID键名
     */
    public static final String UNIFIED_SPAN_ID_KEY = "span-id";

    private static Tracer tracer;

    @Autowired(required = false)
    public void setTracer(Tracer tracer) {
        JaegerTraceUtil.tracer = tracer;
    }

    /**
     * 获取当前的Jaeger trace ID
     * 优先从请求传递的trace ID中获取，其次从Jaeger span中获取
     * 
     * @return Jaeger trace ID，如果不存在则返回null
     */
    public static String getCurrentJaegerTraceId() {
        // 优先从MDC中获取请求传递的trace ID
        String requestTraceId = MDC.get(RequestInterceptor.REQUEST_ID_KEY);
        if (StrUtil.isNotBlank(requestTraceId)) {
            return requestTraceId;
        }
        
        // 其次从统一trace ID中获取
        String unifiedTraceId = MDC.get(UNIFIED_TRACE_ID_KEY);
        if (StrUtil.isNotBlank(unifiedTraceId)) {
            return unifiedTraceId;
        }
        
        // 最后从Jaeger当前span中获取
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                TraceContext context = currentSpan.context();
                if (context != null) {
                    return context.traceId();
                }
            }
        }
        return null;
    }

    /**
     * 获取当前的Jaeger span ID
     * 
     * @return Jaeger span ID，如果不存在则返回null
     */
    public static String getCurrentJaegerSpanId() {
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                TraceContext context = currentSpan.context();
                if (context != null) {
                    return context.spanId();
                }
            }
        }
        return null;
    }

    /**
     * 获取统一的trace ID（优先使用请求传递的trace ID）
     * 
     * @return 统一的trace ID
     */
    public static String getUnifiedTraceId() {
        // 优先从MDC中获取请求传递的trace ID
        String requestTraceId = MDC.get(RequestInterceptor.REQUEST_ID_KEY);
        if (StrUtil.isNotBlank(requestTraceId)) {
            return requestTraceId;
        }
        
        // 其次从统一trace ID中获取
        String unifiedTraceId = MDC.get(UNIFIED_TRACE_ID_KEY);
        if (StrUtil.isNotBlank(unifiedTraceId)) {
            return unifiedTraceId;
        }
        
        // 最后直接从Jaeger span中获取trace ID（避免循环调用）
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                TraceContext context = currentSpan.context();
                if (context != null) {
                    String jaegerTraceId = context.traceId();
                    if (StrUtil.isNotBlank(jaegerTraceId)) {
                        return jaegerTraceId;
                    }
                }
            }
        }
        
        // 回退到自定义的request ID
        String requestId = RequestInterceptor.getCurrentRequestId();
        return StrUtil.isNotBlank(requestId) ? requestId : "unknown";
    }

    /**
     * 获取统一的span ID
     * 
     * @return 统一的span ID
     */
    public static String getUnifiedSpanId() {
        String jaegerSpanId = getCurrentJaegerSpanId();
        return StrUtil.isNotBlank(jaegerSpanId) ? jaegerSpanId : "unknown";
    }

    /**
     * 设置MDC中的trace和span信息
     * 优先使用请求传递的trace ID，其次使用Jaeger的trace ID和span ID
     */
    public static void setMDCTraceInfo() {
        // 获取请求传递的trace ID（已经在RequestInterceptor中设置）
        String requestTraceId = MDC.get(RequestInterceptor.REQUEST_ID_KEY);
        
        // 获取Jaeger的trace ID和span ID（从实际的span中获取）
        String jaegerTraceId = null;
        String jaegerSpanId = null;
        
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                TraceContext context = currentSpan.context();
                if (context != null) {
                    jaegerTraceId = context.traceId();
                    jaegerSpanId = context.spanId();
                }
            }
        }
        
        // 设置Jaeger专用的MDC键
        if (StrUtil.isNotBlank(jaegerTraceId)) {
            MDC.put(JAEGER_TRACE_ID_KEY, jaegerTraceId);
        }
        
        if (StrUtil.isNotBlank(jaegerSpanId)) {
            MDC.put(JAEGER_SPAN_ID_KEY, jaegerSpanId);
            MDC.put(UNIFIED_SPAN_ID_KEY, jaegerSpanId);
        }
        
        // 设置统一的trace ID（优先使用请求传递的trace ID）
        if (StrUtil.isNotBlank(requestTraceId)) {
            MDC.put(UNIFIED_TRACE_ID_KEY, requestTraceId);
        } else if (StrUtil.isNotBlank(jaegerTraceId)) {
            MDC.put(UNIFIED_TRACE_ID_KEY, jaegerTraceId);
        } else {
            // 如果都不可用，使用自定义的request ID
            String requestId = RequestInterceptor.getCurrentRequestId();
            if (StrUtil.isNotBlank(requestId)) {
                MDC.put(UNIFIED_TRACE_ID_KEY, requestId);
            }
        }
    }

    /**
     * 创建新的span
     * 
     * @param operationName 操作名称
     * @return 新创建的span，如果tracer不可用则返回null
     */
    public static Span createSpan(String operationName) {
        if (tracer != null) {
            return tracer.nextSpan().name(operationName).start();
        }
        return null;
    }

    /**
     * 为span添加标签
     * 
     * @param span span对象
     * @param key 标签键
     * @param value 标签值
     */
    public static void addSpanTag(Span span, String key, String value) {
        if (span != null && StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
            span.tag(key, value);
        }
    }

    /**
     * 为span添加事件
     * 
     * @param span span对象
     * @param event 事件名称
     */
    public static void addSpanEvent(Span span, String event) {
        if (span != null && StrUtil.isNotBlank(event)) {
            span.event(event);
        }
    }

    /**
     * 结束span
     * 
     * @param span span对象
     */
    public static void finishSpan(Span span) {
        if (span != null) {
            span.end();
        }
    }

    /**
     * 检查Jaeger是否可用
     * 
     * @return true如果Jaeger可用，否则false
     */
    public static boolean isJaegerAvailable() {
        return tracer != null;
    }

    /**
     * 获取当前span的上下文信息（用于日志）
     * 
     * @return 格式化的上下文信息
     */
    public static String getCurrentSpanContext() {
        String traceId = getUnifiedTraceId();
        String spanId = getUnifiedSpanId();
        String userId = RequestInterceptor.getCurrentUserId();
        String clientIp = RequestInterceptor.getCurrentClientIp();
        
        return String.format("[%s|%s|%s|%s]",
            traceId,
            spanId,
            StrUtil.isNotBlank(userId) ? userId : "anonymous",
            StrUtil.isNotBlank(clientIp) ? clientIp : "unknown"
        );
    }

    /**
     * 从MDC获取trace ID
     * 
     * @return trace ID
     */
    public static String getTraceIdFromMDC() {
        return Optional.ofNullable(MDC.get(UNIFIED_TRACE_ID_KEY))
                .orElse(MDC.get(RequestInterceptor.REQUEST_ID_KEY));
    }

    /**
     * 从MDC获取span ID
     * 
     * @return span ID
     */
    public static String getSpanIdFromMDC() {
        return MDC.get(UNIFIED_SPAN_ID_KEY);
    }
}