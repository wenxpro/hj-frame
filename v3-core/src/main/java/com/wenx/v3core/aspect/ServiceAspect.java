package com.wenx.v3core.aspect;

import com.wenx.v3core.util.HttpServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import static com.wenx.v3core.intercepter.RequestInterceptor.REQUEST_ID_KEY;

@Aspect
@Component
@Slf4j
public class ServiceAspect {

    @Pointcut("execution(* com.wenx..*Service.*(..)) || execution(* com.wenx..*ServiceImpl.*(..))")
    public void anyMethod() {
    }

    @Around("anyMethod()")
    public Object aroundMethod(ProceedingJoinPoint pjp) throws Throwable {
        String serviceName = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
        
        log.debug("[AOP] Service切面生效: {}", serviceName);
        
        long startTime = System.currentTimeMillis();
        Object result = pjp.proceed();
        long endTime = System.currentTimeMillis();
        
        log.info("[Service] {} 执行时间: {}ms", serviceName, (endTime - startTime));
        
        try {
            HttpServletRequest request = HttpServletUtil.getRequest();
            if (request != null) {
                String requestId = request.getHeader(REQUEST_ID_KEY);
                if (requestId != null) {
                    log.debug("[Service] {} 请求ID: {}", serviceName, requestId);
                }
            }
        } catch (Exception e) {
            log.debug("[Service] {} 非Web环境调用", serviceName);
        }
        
        return result;
    }

}
