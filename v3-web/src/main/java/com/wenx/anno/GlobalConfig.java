package com.wenx.anno;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.AliasFor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.annotation.*;

import static com.wenx.v3core.consts.GlobalConfigConsts.*;

/**
 * 微服务启动统一组合注解：扫描 / 注册发现 / 缓存 / Feign / 异步 / 调度 / 切面 / Spring Boot 自动装配
 *
 * @author wenx
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@MapperScan
@EnableDiscoveryClient
@EnableCaching
@EnableFeignClients
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableScheduling
@EnableAsync
public @interface GlobalConfig {

    @AliasFor(annotation = MapperScan.class, attribute = "basePackages")
    String[] mapperScan() default {MAPPER_PACKAGES};

    @AliasFor(annotation = EnableFeignClients.class, attribute = "basePackages")
    String[] feignPackages() default {FEIGN_SCAN};

    @AliasFor(annotation = SpringBootApplication.class, attribute = "scanBasePackages")
    String[] scanPackages() default {SCAN_PACKAGES};

    @AliasFor(annotation = EnableAsync.class, attribute = "proxyTargetClass")
    boolean asyncProxyTargetClass() default true;
}