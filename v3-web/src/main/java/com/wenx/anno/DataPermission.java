package com.wenx.anno;

import java.lang.annotation.*;

/**
 * Service层数据权限控制注解
 * 用于在业务服务方法上声明数据权限过滤规则
 * 
 * @author wenx
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    
    /**
     * 需要进行数据权限控制的表名
     * 支持多表关联查询的权限控制
     */
    String[] tables() default {};
    
    /**
     * 权限条件类型
     */
    PermissionType type() default PermissionType.USER_SCOPE;
    
    /**
     * 自定义权限条件表达式
     * 支持SpEL表达式，可以使用用户上下文变量
     * 例如: "user_id = #{userId} OR dept_id = #{deptId}"
     */
    String condition() default "";
    
    /**
     * 是否启用权限控制
     * 可以用于临时禁用某个方法的权限控制
     */
    boolean enabled() default true;
    
    /**
     * 权限控制的描述信息
     */
    String description() default "";
    
    /**
     * 权限类型枚举
     */
    enum PermissionType {
        /**
         * 用户范围：只能查看自己创建的数据
         */
        USER_SCOPE,
        
        /**
         * 部门范围：只能查看本部门的数据
         */
        DEPT_SCOPE,
        
        /**
         * 部门及下级：可以查看本部门及下级部门的数据
         */
        DEPT_AND_SUB,
        
        /**
         * 自定义条件：使用condition属性指定的条件
         */
        CUSTOM,
        
        /**
         * 无限制：不进行数据权限控制
         */
        NONE
    }
}