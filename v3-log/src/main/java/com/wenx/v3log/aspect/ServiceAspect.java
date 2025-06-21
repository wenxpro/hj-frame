package com.wenx.v3log.aspect;

import com.wenx.v3log.utl.HttpServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import static com.wenx.v3log.RequestInterceptor.REQUEST_ID_KEY;


/**
 * Service层方法执行监控切面
 * 
 * @author wenx
 * @description 监控Service层方法的执行时间和请求信息
 */
@Aspect
@Component
@Slf4j
public class ServiceAspect {

    /**
     * 定义Service层方法切点
     * 只拦截业务包下的Service类，排除框架内部类和代理类
     */
    @Pointcut("(execution(* com.wenx..*Service.*(..)) " +
            "|| execution(* com.wenx..*ServiceImpl.*(..)))" +
            "|| execution(* com.wenx..*Handler.*(..)))")
    public void serviceMethod() {
    }

    /**
     * 环绕通知：监控Service方法执行
     */
    @Around("serviceMethod()")
    public Object monitorServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // 构建方法签名
        String methodSignature = buildMethodSignature(joinPoint);
        
        // 记录切面生效（仅在debug级别）
        if (log.isDebugEnabled()) {
            log.debug("[AOP] Service切面生效: {}", methodSignature);
        }
        
        // 执行方法并计时
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录执行时间
            logExecutionTime(methodSignature, executionTime);
            
            // 记录请求信息（如果在Web环境中）
            logRequestInfo(methodSignature);
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[Service] {} 执行失败，耗时: {}ms", methodSignature, executionTime, e);
            throw e;
        }
    }

    /**
     * 构建方法签名字符串
     */
    private String buildMethodSignature(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
    }

    /**
     * 记录方法执行时间
     */
    private void logExecutionTime(String methodSignature, long executionTime) {
        if (executionTime > 1000) {
            // 超过1秒的方法用warn级别记录
            log.warn("[Service] {} 执行时间: {}ms (性能警告)", methodSignature, executionTime);
        } else if (executionTime > 500) {
            // 超过500ms的方法用info级别记录
            log.info("[Service] {} 执行时间: {}ms", methodSignature, executionTime);
        } else if (log.isDebugEnabled()) {
            // 其他情况在debug级别记录
            log.debug("[Service] {} 执行时间: {}ms", methodSignature, executionTime);
        }
    }

    /**
     * 记录Web请求相关信息
     */
    private void logRequestInfo(String methodSignature) {
        if (!log.isDebugEnabled()) {
            return;
        }
        
        try {
            HttpServletRequest request = HttpServletUtil.getRequest();
            if (request != null) {
                String requestId = request.getHeader(REQUEST_ID_KEY);
                if (requestId != null) {
                    log.debug("[Service] {} 请求ID: {}", methodSignature, requestId);
                }
            }
        } catch (Exception e) {
            // 非Web环境调用，静默处理
            log.debug("[Service] {} 非Web环境调用", methodSignature);
        }
    }
}
