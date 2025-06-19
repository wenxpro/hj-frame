package com.wenx.v3secure.annotation;

import java.lang.annotation.*;

/**
 * 匿名访问注解
 * 
 * @author wenx
 * @description 标记允许匿名访问的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous {
} 