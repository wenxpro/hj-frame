package com.wenx.v3secure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解（D3）
 * 标注敏感操作方法，由 {@code AuditLogAspect} 自动记录审计日志。
 *
 * @author wenx
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 操作类型（LOGIN / LOGOUT / CHANGE_PASSWORD / UPDATE_USER / DISABLE_USER / DELETE_USER / CHANGE_PERMISSION ...）
     */
    String action();

    /**
     * 资源类型（可选，如 USER / ROLE / PERMISSION）
     */
    String resourceType() default "";
}
