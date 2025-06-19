package com.wenx.v3redis.aspect;

import com.wenx.v3redis.annotation.CacheLock;
import com.wenx.v3redis.exception.RedisLockException;
import com.wenx.v3redis.lock.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 缓存锁切面
 * 
 * @author wenx
 * @description 处理@CacheLock注解的AOP切面，支持多种锁实现和自动续期
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class CacheLockAspect {
    
    @Autowired
    @Qualifier("redisDistributedLock")
    private RedisLock defaultRedisLock;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // 默认配置值，可通过配置文件覆盖
    @Value("${v3.redis.lock.key-prefix:v3:lock:}")
    private String defaultKeyPrefix;
    
    @Value("${v3.redis.lock.enable-watchdog:false}")
    private boolean enableWatchdog;
    
    @Value("${v3.redis.lock.watchdog-interval:10}")
    private long watchdogInterval;
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    // 锁续期的调度器
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1, r -> {
        Thread thread = new Thread(r, "redis-lock-watchdog");
        thread.setDaemon(true);
        return thread;
    });
    
    // 存储需要续期的锁信息
    private final Map<String, LockInfo> activeLocks = new ConcurrentHashMap<>();
    
    @Around("@annotation(cacheLock)")
    public Object around(ProceedingJoinPoint joinPoint, CacheLock cacheLock) throws Throwable {
        // 获取锁的key
        String lockKey = getLockKey(joinPoint, cacheLock);
        
        // 生成或获取请求ID
        String requestId = getRequestId(joinPoint, cacheLock);
        
        // 获取锁配置，优先使用注解中的配置
        long expire = cacheLock.expire();
        TimeUnit timeUnit = cacheLock.timeUnit();
        long waitTime = cacheLock.waitTime();
        
        // 获取锁实现
        RedisLock redisLock = getRedisLock(cacheLock);
        
        boolean lockAcquired = false;
        LockInfo lockInfo = null;
        
        try {
            // 尝试获取锁
            if (waitTime > 0) {
                lockAcquired = redisLock.tryLock(lockKey, requestId, waitTime, expire, timeUnit);
            } else if (waitTime == 0) {
                lockAcquired = redisLock.tryLock(lockKey, requestId, expire, timeUnit);
            } else {
                // waitTime < 0 表示一直等待
                redisLock.lock(lockKey, requestId, expire, timeUnit);
                lockAcquired = true;
            }
            
            if (!lockAcquired) {
                log.warn("获取锁失败，key: {}, method: {}, requestId: {}", 
                        lockKey, getMethodName(joinPoint), requestId);
                throw new RedisLockException(cacheLock.message());
            }
            
            log.debug("成功获取锁，key: {}, requestId: {}, expire: {} {}", 
                    lockKey, requestId, expire, timeUnit);
            
            // 如果启用了看门狗，开始自动续期
            if (shouldEnableWatchdog(cacheLock) && expire > 0) {
                lockInfo = new LockInfo(lockKey, requestId, expire, timeUnit, redisLock);
                activeLocks.put(lockKey, lockInfo);
                scheduleRenewal(lockInfo);
            }
            
            // 执行目标方法
            return joinPoint.proceed();
            
        } finally {
            // 释放锁
            if (lockAcquired) {
                try {
                    // 停止续期
                    if (lockInfo != null) {
                        activeLocks.remove(lockKey);
                    }
                    
                    boolean unlocked = redisLock.unlock(lockKey, requestId);
                    if (unlocked) {
                        log.debug("成功释放锁，key: {}, requestId: {}", lockKey, requestId);
                    } else {
                        log.warn("释放锁失败，key: {}, requestId: {}", lockKey, requestId);
                    }
                } catch (Exception e) {
                    log.error("释放锁异常，key: {}, requestId: {}", lockKey, requestId, e);
                }
            }
        }
    }
    
    /**
     * 获取锁的key
     */
    private String getLockKey(ProceedingJoinPoint joinPoint, CacheLock cacheLock) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加全局前缀
        if (StringUtils.hasText(defaultKeyPrefix)) {
            keyBuilder.append(defaultKeyPrefix);
        }
        
        // 添加注解指定的前缀
        String prefix = cacheLock.prefix();
        if (StringUtils.hasText(prefix)) {
            keyBuilder.append(prefix);
            if (!prefix.endsWith(":")) {
                keyBuilder.append(":");
            }
        } else {
            // 默认前缀：类名:方法名
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            keyBuilder.append(className).append(":").append(methodName).append(":");
        }
        
        // 解析key表达式
        String keyExpression = cacheLock.key();
        if (StringUtils.hasText(keyExpression)) {
            String key = parseExpression(keyExpression, joinPoint);
            keyBuilder.append(key);
        } else {
            // 默认使用所有参数
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    keyBuilder.append(":");
                }
                if (args[i] != null) {
                    keyBuilder.append(args[i]);
                }
            }
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 获取请求ID
     */
    private String getRequestId(ProceedingJoinPoint joinPoint, CacheLock cacheLock) {
        // 如果注解中指定了请求ID的SpEL表达式
        String requestIdExpression = getRequestIdExpression(cacheLock);
        if (StringUtils.hasText(requestIdExpression)) {
            String requestId = parseExpression(requestIdExpression, joinPoint);
            if (StringUtils.hasText(requestId)) {
                return requestId;
            }
        }
        
        // 默认生成UUID
        return UUID.randomUUID().toString();
    }
    
    /**
     * 获取请求ID表达式（通过反射获取，因为注解中可能没有这个属性）
     */
    private String getRequestIdExpression(CacheLock cacheLock) {
        try {
            Method method = cacheLock.getClass().getMethod("requestId");
            return (String) method.invoke(cacheLock);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 获取锁实现
     */
    private RedisLock getRedisLock(CacheLock cacheLock) {
        // 检查是否指定了特定的锁实现
        String lockType = getLockType(cacheLock);
        if (StringUtils.hasText(lockType)) {
            try {
                return applicationContext.getBean(lockType, RedisLock.class);
            } catch (Exception e) {
                log.warn("获取指定的锁实现失败: {}, 使用默认实现", lockType);
            }
        }
        
        return defaultRedisLock;
    }
    
    /**
     * 获取锁类型
     */
    private String getLockType(CacheLock cacheLock) {
        return cacheLock.lockType();
    }
    
    /**
     * 是否启用看门狗
     */
    private boolean shouldEnableWatchdog(CacheLock cacheLock) {
        // 优先使用注解中的配置
        if (cacheLock.fair()) {
            // 如果fair为true，我们可以将其解释为启用看门狗
            return true;
        }
        return enableWatchdog;
    }
    
    /**
     * 解析SpEL表达式
     */
    private String parseExpression(String expression, ProceedingJoinPoint joinPoint) {
        try {
            // 获取方法参数
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames();
            
            // 创建SpEL上下文
            EvaluationContext context = new StandardEvaluationContext();
            
            // 添加目标对象
            context.setVariable("target", joinPoint.getTarget());
            
            // 添加方法
            context.setVariable("method", method);
            
            // 添加参数到上下文
            if (paramNames != null && args != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            
            // 解析表达式
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.error("解析SpEL表达式失败: {}", expression, e);
            return expression;
        }
    }
    
    /**
     * 获取方法名称
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        return joinPoint.getTarget().getClass().getSimpleName() + 
               "." + joinPoint.getSignature().getName();
    }
    
    /**
     * 调度锁续期
     */
    private void scheduleRenewal(LockInfo lockInfo) {
        scheduler.schedule(() -> {
            try {
                // 检查锁是否还在活跃列表中
                if (!activeLocks.containsKey(lockInfo.key)) {
                    return;
                }
                
                // 续期
                boolean renewed = lockInfo.redisLock.renewLock(
                        lockInfo.key, 
                        lockInfo.requestId, 
                        lockInfo.expire, 
                        lockInfo.timeUnit
                );
                
                if (renewed) {
                    log.debug("锁续期成功，key: {}, requestId: {}", lockInfo.key, lockInfo.requestId);
                    // 继续调度下一次续期
                    scheduleRenewal(lockInfo);
                } else {
                    log.warn("锁续期失败，key: {}, requestId: {}", lockInfo.key, lockInfo.requestId);
                    activeLocks.remove(lockInfo.key);
                }
            } catch (Exception e) {
                log.error("锁续期异常，key: {}, requestId: {}", lockInfo.key, lockInfo.requestId, e);
                activeLocks.remove(lockInfo.key);
            }
        }, watchdogInterval, TimeUnit.SECONDS);
    }
    
    /**
     * 锁信息
     */
    private static class LockInfo {
        final String key;
        final String requestId;
        final long expire;
        final TimeUnit timeUnit;
        final RedisLock redisLock;
        
        LockInfo(String key, String requestId, long expire, TimeUnit timeUnit, RedisLock redisLock) {
            this.key = key;
            this.requestId = requestId;
            this.expire = expire;
            this.timeUnit = timeUnit;
            this.redisLock = redisLock;
        }
    }
} 