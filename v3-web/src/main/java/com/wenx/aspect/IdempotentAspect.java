package com.wenx.aspect;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSON;
import com.wenx.anno.ApiIdempotent;
import com.wenx.consts.OperationConst;
import com.wenx.v3core.error.BusinessException;
import com.wenx.v3redis.service.CacheService;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author wenx
 * @decription 接口幂等切片
 */
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    @Resource
    private CacheService redisUtil;

    @Pointcut("@annotation(com.wenx.anno.ApiIdempotent)")
    private void pointCut() {
    }

    @SneakyThrows
    @Before("pointCut()")
    public void doPoint(JoinPoint joinPoint) {
        String action = analyzeAction(joinPoint);
        String cookie = analyzeToken();
        //未登录 添加临时cookie 根据传递参数判断重复
        if (CharSequenceUtil.isBlank(cookie)) {
            cookie = "temp-cookie";
        } else {
            cookie = cookie.replace("JSESSIONID=", "");
        }
        String idempotentKey = "api::idempotent::" + cookie + "::" + action;
        MethodSignature sign = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServletRequest || args[i] instanceof ServletResponse || args[i] instanceof MultipartFile) {
                //ServletRequest不能序列化，从入参里排除，否则报异常：java.lang.IllegalStateException: It is illegal to call this method if the current request is not in asynchronous mode (i.e. isAsyncStarted() returns false)
                //ServletResponse不能序列化 从入参里排除，否则报异常：java.lang.IllegalStateException: getOutputStream() has already been called for this response
                continue;
            }
            arguments[i] = args[i];
        }
        String paramter = "";
        try {
            paramter = JSON.toJSONString(arguments);
        } catch (Exception e) {
            paramter = Arrays.toString(arguments);
        }
        //短时间内没进行相似操作
        if (Boolean.TRUE.equals(redisUtil.exists(idempotentKey))) {
            //接口参数是否一致
            String idempotentValue = redisUtil.get(idempotentKey);
            log.info("params : {}", idempotentValue);
            if (paramter.equals(idempotentValue)) {
                throw new BusinessException(OperationConst.REPEAT);
            }
        } else {
            ApiIdempotent api = sign.getMethod().getAnnotation(ApiIdempotent.class);
            redisUtil.expire(idempotentKey, api.expire(), TimeUnit.SECONDS);
        }

    }

    /**
     * 解析 token
     *
     * @return
     */
    private String analyzeToken() {
        ServletRequestAttributes attributes = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes()));
        HttpServletRequest request = attributes.getRequest();
        log.info("headers:{}", JSON.toJSONString(request.getHeaderNames()));
        return request.getHeader("cookie");
    }

    /**
     * 解析 action
     *
     * @param joinPoint
     * @return
     */
    private String analyzeAction(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringTypeName()
                .substring(joinPoint.getSignature().getDeclaringTypeName().lastIndexOf(".") + 1)
                + "::" + joinPoint.getSignature().getName();
    }
}
