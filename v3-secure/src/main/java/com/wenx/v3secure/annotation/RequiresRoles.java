package com.wenx.v3secure.annotation;

import java.lang.annotation.*;

/**
 * 角色验证注解
 * 
 * @author wenx
 * @description 用于标记需要特定角色才能访问的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRoles {
    
    /**
     * 需要的角色值
     */
    String[] value();
    
    /**
     * 角色之间的逻辑关系
     */
    Logical logical() default Logical.AND;
    
    /**
     * 逻辑关系枚举
     */
    enum Logical {
        /**
         * 必须拥有所有角色
         */
        AND,
        
        /**
         * 只需拥有其中一个角色
         */
        OR
    }
} 