package com.wenx.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.wenx.anno.ApiIdempotent;
import com.wenx.consts.OperationConst;
import com.wenx.v3core.error.BusinessException;
import com.wenx.v3redis.service.CacheService;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author wenx
 * @description 接口幂等性切面
 * 通过AOP拦截带有@ApiIdempotent注解的方法，实现接口幂等性控制
 */
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    @Resource
    private CacheService cacheService;

    /**
     * 定义切点：拦截所有使用@ApiIdempotent注解的方法
     */
    @Pointcut("@annotation(com.wenx.anno.ApiIdempotent)")
    private void idempotentPointcut() {
    }

    /**
     * 前置通知：在目标方法执行前进行幂等性检查
     *
     * @param joinPoint 连接点
     */
    @Before("idempotentPointcut()")
    public void checkIdempotent(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            ApiIdempotent idempotentAnnotation = method.getAnnotation(ApiIdempotent.class);
            
            if (idempotentAnnotation == null) {
                return;
            }

            // 生成幂等性键
            String idempotentKey = generateIdempotentKey(joinPoint, idempotentAnnotation);
            
            // 检查是否存在重复请求
            if (Boolean.TRUE.equals(cacheService.exists(idempotentKey))) {
                String errorMessage = StrUtil.isNotBlank(idempotentAnnotation.message()) 
                    ? idempotentAnnotation.message() 
                    : OperationConst.REPEAT;
                
                log.warn("检测到重复请求，幂等性键: {}, 方法: {}", idempotentKey, getMethodInfo(joinPoint));
                throw new BusinessException(errorMessage);
            }

            // 设置幂等性标记
            setIdempotentMark(idempotentKey, joinPoint, idempotentAnnotation);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("幂等性检查异常", e);
            // 如果幂等性检查出现异常，为了保证系统可用性，这里不抛出异常
        }
    }

    /**
     * 生成幂等性键
     *
     * @param joinPoint 连接点
     * @param annotation 注解
     * @return 幂等性键
     */
    private String generateIdempotentKey(JoinPoint joinPoint, ApiIdempotent annotation) {
        StringBuilder keyBuilder = new StringBuilder(annotation.keyPrefix()).append(":");
        
        // 根据不同的幂等性策略生成键
        switch (annotation.type()) {
            case DEFAULT:
                return buildDefaultKey(joinPoint, annotation, keyBuilder);
            case USER_METHOD:
                return buildUserMethodKey(joinPoint, annotation, keyBuilder);
            case METHOD_ARGS:
                return buildMethodArgsKey(joinPoint, annotation, keyBuilder);
            case METHOD_ONLY:
                return buildMethodOnlyKey(joinPoint, keyBuilder);
            default:
                return buildDefaultKey(joinPoint, annotation, keyBuilder);
        }
    }

    /**
     * 构建默认键：用户+方法+参数
     */
    private String buildDefaultKey(JoinPoint joinPoint, ApiIdempotent annotation, StringBuilder keyBuilder) {
        if (annotation.includeUser()) {
            keyBuilder.append(getUserIdentifier()).append(":");
        }
        
        keyBuilder.append(getMethodSignature(joinPoint));
        
        if (annotation.includeArgs()) {
            keyBuilder.append(":").append(getArgsHash(joinPoint.getArgs()));
        }
        
        return keyBuilder.toString();
    }

    /**
     * 构建用户+方法键
     */
    private String buildUserMethodKey(JoinPoint joinPoint, ApiIdempotent annotation, StringBuilder keyBuilder) {
        if (annotation.includeUser()) {
            keyBuilder.append(getUserIdentifier()).append(":");
        }
        keyBuilder.append(getMethodSignature(joinPoint));
        return keyBuilder.toString();
    }

    /**
     * 构建方法+参数键
     */
    private String buildMethodArgsKey(JoinPoint joinPoint, ApiIdempotent annotation, StringBuilder keyBuilder) {
        keyBuilder.append(getMethodSignature(joinPoint));
        if (annotation.includeArgs()) {
            keyBuilder.append(":").append(getArgsHash(joinPoint.getArgs()));
        }
        return keyBuilder.toString();
    }

    /**
     * 构建仅方法键
     */
    private String buildMethodOnlyKey(JoinPoint joinPoint, StringBuilder keyBuilder) {
        keyBuilder.append(getMethodSignature(joinPoint));
        return keyBuilder.toString();
    }

    /**
     * 获取用户标识
     *
     * @return 用户标识
     */
    private String getUserIdentifier() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            // 优先从请求头获取用户ID
            String userId = request.getHeader("X-User-Id");
            if (StrUtil.isNotBlank(userId)) {
                return userId;
            }
            
            // 从Cookie获取会话标识
            String cookie = request.getHeader("Cookie");
            if (StrUtil.isNotBlank(cookie)) {
                return DigestUtil.md5Hex(cookie);
            }
            
            // 从IP地址获取标识（最后的备选方案）
            String clientIp = getClientIpAddress(request);
            return StrUtil.isNotBlank(clientIp) ? DigestUtil.md5Hex(clientIp) : "anonymous";
            
        } catch (Exception e) {
            log.warn("获取用户标识失败", e);
            return "anonymous";
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 获取方法签名
     *
     * @param joinPoint 连接点
     * @return 方法签名
     */
    private String getMethodSignature(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        // 只保留类名，不包含完整包路径
        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
        return simpleClassName + "." + methodName;
    }

    /**
     * 获取参数哈希值
     *
     * @param args 方法参数
     * @return 参数哈希值
     */
    private String getArgsHash(Object[] args) {
        if (args == null || args.length == 0) {
            return "no-args";
        }

        List<Object> serializableArgs = new ArrayList<>();
        
        // 过滤不可序列化的参数
        for (Object arg : args) {
            if (isSerializable(arg)) {
                serializableArgs.add(arg);
            }
        }

        try {
            String argsJson = JSON.toJSONString(serializableArgs);
            return DigestUtil.md5Hex(argsJson);
        } catch (Exception e) {
            log.warn("参数序列化失败，使用toString方法", e);
            return DigestUtil.md5Hex(Arrays.toString(serializableArgs.toArray()));
        }
    }

    /**
     * 判断对象是否可序列化
     *
     * @param obj 对象
     * @return 是否可序列化
     */
    private boolean isSerializable(Object obj) {
        if (obj == null) {
            return true;
        }
        
        // 排除不可序列化的类型
        return !(obj instanceof ServletRequest) 
            && !(obj instanceof ServletResponse) 
            && !(obj instanceof MultipartFile);
    }

    /**
     * 设置幂等性标记
     *
     * @param key 幂等性键
     * @param joinPoint 连接点
     * @param annotation 注解
     */
    private void setIdempotentMark(String key, JoinPoint joinPoint, ApiIdempotent annotation) {
        try {
            // 使用一个简单的标记值，减少内存占用
            String markValue = System.currentTimeMillis() + ":" + getMethodSignature(joinPoint);
            cacheService.put(key, markValue, annotation.expire(), annotation.timeUnit());
            
            log.debug("设置幂等性标记成功，键: {}, 过期时间: {}{}",
                key, annotation.expire(), annotation.timeUnit().name().toLowerCase());
                
        } catch (Exception e) {
            log.error("设置幂等性标记失败，键: {}", key, e);
        }
    }

    /**
     * 获取方法信息（用于日志）
     *
     * @param joinPoint 连接点
     * @return 方法信息
     */
    private String getMethodInfo(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
    }
}
