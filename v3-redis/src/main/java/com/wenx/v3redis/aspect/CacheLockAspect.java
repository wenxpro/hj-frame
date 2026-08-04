package com.wenx.v3redis.aspect;

import com.wenx.v3redis.annotation.CacheLock;
import com.wenx.v3redis.exception.RedisLockException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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

import java.util.concurrent.TimeUnit;

/**
 * 缓存锁切面（P2.3：自研锁换 Redisson）
 * 
 * 语义（与自研版一致，业务零改动）：
 * - waitTime < 0  → lock()（Redisson 看门狗托管，租期 30s 自动续期）；
 * - waitTime == 0 → tryLock() 立即尝试，拿不到立即失败；
 * - waitTime > 0  → tryLock(waitTime, expire, unit)，指定租期；
 * - fair() = true → 公平锁；
 * - 释放用 Redisson RLock.unlock()（仅持有者可释放，天然防误删他人锁）。
 * 
 * 删除的自研实现：AbstractRedisLock / RedisDistributedLock / RedisReentrantLock /
 * RedisSingleLock / RedisFairLock / RedisMultiLock / RedisReadWriteLock 及看门狗调度。
 *
 * @author wenx
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class CacheLockAspect {

    private final RedissonClient redissonClient;
    private final ApplicationContext applicationContext;

    @Value("${v3.redis.lock.key-prefix:v3:lock:}")
    private String defaultKeyPrefix;

    private final ExpressionParser parser = new SpelExpressionParser();

    public CacheLockAspect(RedissonClient redissonClient, ApplicationContext applicationContext) {
        this.redissonClient = redissonClient;
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(cacheLock)")
    public Object around(ProceedingJoinPoint joinPoint, CacheLock cacheLock) throws Throwable {
        String lockKey = getLockKey(joinPoint, cacheLock);
        long expire = cacheLock.expire();
        TimeUnit timeUnit = cacheLock.timeUnit();
        long waitTime = cacheLock.waitTime();

        RLock lock = cacheLock.fair()
                ? redissonClient.getFairLock(lockKey)
                : redissonClient.getLock(lockKey);

        boolean lockAcquired = false;
        try {
            if (waitTime > 0) {
                lockAcquired = lock.tryLock(waitTime, expire, timeUnit);
            } else if (waitTime == 0) {
                lockAcquired = lock.tryLock(0, expire, timeUnit);
            } else {
                // waitTime < 0：一直等待，Redisson 看门狗托管（默认 30s 自动续期）
                lock.lock();
                lockAcquired = true;
            }

            if (!lockAcquired) {
                log.warn("获取锁失败，key: {}, method: {}", lockKey, getMethodName(joinPoint));
                throw new RedisLockException(cacheLock.message());
            }

            log.debug("成功获取锁，key: {}, expire: {} {}", lockKey, expire, timeUnit);
            return joinPoint.proceed();

        } finally {
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("成功释放锁，key: {}", lockKey);
            }
        }
    }

    /**
     * 获取锁的key（含全局前缀 + 注解前缀/类名:方法名 + SpEL 或参数）
     */
    private String getLockKey(ProceedingJoinPoint joinPoint, CacheLock cacheLock) {
        StringBuilder keyBuilder = new StringBuilder();

        if (StringUtils.hasText(defaultKeyPrefix)) {
            keyBuilder.append(defaultKeyPrefix);
        }

        String prefix = cacheLock.prefix();
        if (StringUtils.hasText(prefix)) {
            keyBuilder.append(prefix);
            if (!prefix.endsWith(":")) {
                keyBuilder.append(":");
            }
        } else {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            keyBuilder.append(className).append(":").append(methodName).append(":");
        }

        String keyExpression = cacheLock.key();
        if (StringUtils.hasText(keyExpression)) {
            keyBuilder.append(parseExpression(keyExpression, joinPoint));
        } else {
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
     * 解析SpEL表达式
     */
    private String parseExpression(String expression, ProceedingJoinPoint joinPoint) {
        try {
            Expression spelExpression = parser.parseExpression(expression);
            EvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            Object value = spelExpression.getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("SpEL 表达式解析失败: {}, error: {}", expression, e.getMessage());
            return expression;
        }
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
    }
}
