package com.wenx.v3core.aspect;

import com.alibaba.fastjson2.JSON;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author wenx
 * @description action aspect
 */
@Aspect
@Component
@Slf4j
public class ControllerAspect {

    @Pointcut("execution(* com.wenx..*Controller.*(..)) || execution(* com.wenx..*RestController.*(..))")
    public void sysAccessLog() {
    }

    @Around(value = "sysAccessLog()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        long stime = System.currentTimeMillis();
        
        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        // 记录切面生效
        log.debug("[AOP] Controller切面生效: {}.{}", 
                pjp.getSignature().getDeclaringTypeName(), 
                pjp.getSignature().getName());
        
        //运行方法
        Object o = pjp.proceed();

        long etime = System.currentTimeMillis();
        
        if (attributes != null) {
            ControllerLog controllerLog = handlerRequest(attributes.getRequest(), pjp, o, (etime - stime) + "ms");
            log.info("[HTTP_>>>]: {}", controllerLog);
        } else {
            // 非HTTP请求的Controller方法调用
            log.info("[CONTROLLER_>>>]: {}.{} 执行时间: {}ms", 
                    pjp.getSignature().getDeclaringTypeName(), 
                    pjp.getSignature().getName(), 
                    (etime - stime));
        }
        
        return o;
    }

    ControllerLog handlerRequest(HttpServletRequest req, ProceedingJoinPoint pjp, Object o, String time) {
        ControllerLog controllerLog = new ControllerLog();
        controllerLog.setUrl(req.getRequestURL().toString());
        controllerLog.setMethod(req.getMethod());
        controllerLog.setAction(pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName());
        controllerLog.setParams(JSON.toJSONString(getMethodArg(pjp.getArgs())));
        controllerLog.setIp(getIpAddress(req));
        controllerLog.setTime(time);
        return controllerLog;
    }

    private Object[] getMethodArg(Object[] args) {
        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServletRequest || args[i] instanceof ServletResponse || args[i] instanceof MultipartFile || ((args[i] instanceof List) && ((List<?>) args[i]).get(0) instanceof MultipartFile)) {
                //ServletRequest不能序列化，从入参里排除，否则报异常：java.lang.IllegalStateException: It is illegal to call this method if the current request is not in asynchronous mode (i.e. isAsyncStarted() returns false)
                //ServletResponse不能序列化 从入参里排除，否则报异常：java.lang.IllegalStateException: getOutputStream() has already been called for this response
                continue;
            }
            arguments[i] = args[i];
        }
        return arguments;
    }

    @Data
    public static class ControllerLog {
        private String url;
        private String method;
        private String action;
        private String params;
        private String ip;
        private String time;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，那么取第一个ip为客户端ip
        if (ip != null && ip.indexOf(",") != -1) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }

        return ip;
    }
}