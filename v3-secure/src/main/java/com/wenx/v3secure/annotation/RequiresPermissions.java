package com.wenx.v3secure.annotation;

import java.lang.annotation.*;

/**
 * 权限验证注解
 * 
 * @author wenx
 * @description 用于标记需要特定权限才能访问的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermissions {
    
    /**
     * 需要的权限值
     */
    String[] value();
    
    /**
     * 权限之间的逻辑关系
     */
    Logical logical() default Logical.AND;
    
    /**
     * 逻辑关系枚举
     */
    enum Logical {
        /**
         * 必须拥有所有权限
         */
        AND,
        
        /**
         * 只需拥有其中一个权限
         */
        OR
    }
} 