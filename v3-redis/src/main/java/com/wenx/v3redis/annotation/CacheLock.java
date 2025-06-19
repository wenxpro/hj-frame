package com.wenx.v3redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存锁注解
 * 
 * @author wenx
 * @description 用于方法级别的分布式锁
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheLock {
    
    /**
     * 锁的键前缀
     */
    String prefix() default "";
    
    /**
     * 锁的键（支持SpEL表达式）
     */
    String key() default "";
    
    /**
     * 锁的过期时间
     */
    long expire() default 30;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 获取锁的等待时间
     */
    long waitTime() default 0;
    
    /**
     * 是否使用公平锁（启用看门狗）
     */
    boolean fair() default false;
    
    /**
     * 锁类型（bean名称）
     * 可选值：redisSingleLock, redisDistributedLock, redisFairLock, 
     *        redisReentrantLock, redisReadWriteLock, redisMultiLock
     * 默认使用 redisDistributedLock
     */
    String lockType() default "";
    
    /**
     * 失败时的提示信息
     */
    String message() default "操作太频繁，请稍后再试";
} 