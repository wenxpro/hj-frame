package com.wenx.v3seata.annotation;

import java.lang.annotation.*;

/**
 * 分布式事务注解
 * 对 @GlobalTransactional 的增强封装
 * 
 * @author v3-cloud
 * @since 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DistributedTransaction {
    
    /**
     * 事务名称
     */
    String name() default "";
    
    /**
     * 超时时间（毫秒） - 默认 60 秒
     */
    int timeoutMills() default 60000;
    
    /**
     * 回滚规则（异常类）
     */
    Class<? extends Throwable>[] rollbackFor() default {Exception.class};
    
    /**
     * 不回滚规则（异常类）
     */
    Class<? extends Throwable>[] noRollbackFor() default {};
    
    /**
     * 是否忽略全局事务
     */
    boolean ignoreGlobalTransaction() default false;
} 