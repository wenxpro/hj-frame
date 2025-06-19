package com.wenx.v3redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 * 
 * @author wenx
 * @description 基于Redis的限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流的键前缀
     */
    String prefix() default "";
    
    /**
     * 限流的键（支持SpEL表达式）
     */
    String key() default "";
    
    /**
     * 时间窗口内的最大请求数
     */
    int limit() default 10;
    
    /**
     * 时间窗口大小
     */
    long window() default 1;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    
    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.IP;
    
    /**
     * 限流时的提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
    
    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 根据IP限流
         */
        IP,
        
        /**
         * 根据用户限流
         */
        USER,
        
        /**
         * 根据自定义键限流
         */
        CUSTOM,
        
        /**
         * 全局限流
         */
        GLOBAL
    }
} 