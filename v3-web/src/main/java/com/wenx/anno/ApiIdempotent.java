package com.wenx.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author wenx
 * @description 接口幂等注解
 * 用于防止接口重复调用，支持多种幂等性策略
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiIdempotent {

    /**
     * 幂等性过期时间，默认60秒
     * @return 过期时间
     */
    int expire() default 60;

    /**
     * 时间单位，默认秒
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 幂等性键的前缀，默认为 "api:idempotent"
     * @return 键前缀
     */
    String keyPrefix() default "api:idempotent";

    /**
     * 是否包含请求参数在幂等性判断中，默认true
     * @return 是否包含参数
     */
    boolean includeArgs() default true;

    /**
     * 是否包含用户标识在幂等性判断中，默认true
     * @return 是否包含用户标识
     */
    boolean includeUser() default true;

    /**
     * 自定义错误消息
     * @return 错误消息
     */
    String message() default "";

    /**
     * 幂等性策略
     * @return 幂等性策略
     */
    IdempotentType type() default IdempotentType.DEFAULT;

    /**
     * 幂等性类型枚举
     */
    enum IdempotentType {
        /**
         * 默认策略：基于用户+方法+参数
         */
        DEFAULT,
        /**
         * 基于用户+方法（忽略参数）
         */
        USER_METHOD,
        /**
         * 仅基于方法+参数（忽略用户）
         */
        METHOD_ARGS,
        /**
         * 仅基于方法（忽略用户和参数）
         */
        METHOD_ONLY
    }
}
