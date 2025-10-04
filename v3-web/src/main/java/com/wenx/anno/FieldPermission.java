package com.wenx.anno;

import java.lang.annotation.*;

/**
 * 字段级权限控制注解
 * 用于在实体类字段上声明字段级权限控制规则
 * 结合现有的数据权限系统，实现精细化的字段级访问控制
 * 
 * 注意：此注解仅负责权限控制，数据脱敏需要额外使用@DataMask注解
 * 
 * @author wenx
 * @since 1.0.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldPermission {
    
    /**
     * 权限编码
     * 对应sys_permission表中的permission_code字段
     * 例如: "user:view:phone", "dept:view:salary"
     */
    String[] value() default {};
    
    /**
     * 字段访问级别
     */
    AccessLevel level() default AccessLevel.PROTECTED;
    
    /**
     * 角色要求
     * 拥有指定角色之一即可访问该字段
     */
    String[] roles() default {};
    
    /**
     * 自定义权限验证条件
     * 支持SpEL表达式，可以使用用户上下文变量
     * 例如: "user.departmentId == target.departmentId"
     */
    String condition() default "";
    
    /**
     * 是否启用字段权限控制
     */
    boolean enabled() default true;
    
    /**
     * 权限控制的描述信息
     */
    String description() default "";
    
    /**
     * 字段访问级别枚举
     */
    enum AccessLevel {
        /**
         * 公开：所有登录用户都可以访问
         */
        PUBLIC,
        
        /**
         * 受保护：需要特定权限才能访问
         */
        PROTECTED,
        
        /**
         * 私有：只有数据所有者或超级管理员才能访问
         */
        PRIVATE,
        
        /**
         * 机密：只有超级管理员才能访问
         */
        CONFIDENTIAL
    }

}