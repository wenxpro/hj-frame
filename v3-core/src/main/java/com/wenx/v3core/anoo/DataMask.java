package com.wenx.v3core.anoo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解
 * 
 * <p>用于标记需要脱敏的字段，支持多种脱敏策略</p>
 * 
 * @author wenx
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataMask {

    /**
     * 脱敏策略
     * 
     * @return 脱敏策略枚举
     */
    MaskStrategy strategy() default MaskStrategy.CUSTOM;

    /**
     * 自定义脱敏规则（当strategy为CUSTOM时使用）
     * 支持正则表达式替换
     * 
     * @return 自定义脱敏规则
     */
    String customRule() default "";

    /**
     * 保留前几位字符（用于部分策略）
     * 
     * @return 保留前几位字符数
     */
    int prefixKeep() default 3;

    /**
     * 保留后几位字符（用于部分策略）
     * 
     * @return 保留后几位字符数
     */
    int suffixKeep() default 4;

    /**
     * 脱敏字符（默认使用*）
     * 
     * @return 脱敏字符
     */
    char maskChar() default '*';

    /**
     * 是否启用脱敏（可用于动态控制）
     * 
     * @return 是否启用
     */
    boolean enabled() default true;

    /**
     * 脱敏策略枚举
     */
    enum MaskStrategy {
        /**
         * 手机号脱敏：138****1234
         */
        PHONE,
        
        /**
         * 身份证号脱敏：110101********1234
         */
        ID_CARD,
        
        /**
         * 邮箱脱敏：abc***@example.com
         */
        EMAIL,
        
        /**
         * 银行卡号脱敏：6222 **** **** 1234
         */
        BANK_CARD,
        
        /**
         * 姓名脱敏：张*、李**
         */
        NAME,
        
        /**
         * 地址脱敏：保留前6位，其余用*代替
         */
        ADDRESS,
        
        /**
         * 密码脱敏：全部用*代替
         */
        PASSWORD,
        
        /**
         * IP地址脱敏：192.168.*.*
         */
        IP_ADDRESS,
        
        /**
         * 自定义脱敏规则
         */
        CUSTOM,
        
        /**
         * 不脱敏
         */
        NONE
    }
} 