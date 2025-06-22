package com.wenx.anno;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.annotation.*;

import static com.wenx.v3core.consts.GlobalConfigConsts.*;

/**
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
@ComponentScan
@EnableAsync
public @interface GlobalConfig {

    @AliasFor(annotation = MapperScan.class, attribute = "basePackages")
    String[] mapperScan() default {MAPPER_PACKAGES};

    @AliasFor(annotation = EnableFeignClients.class, attribute = "basePackages")
    String[] feignPackages() default {FEIGN_SCAN};

    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String[] componentPackages() default {SCAN_PACKAGES};
    
    @AliasFor(annotation = EnableAsync.class, attribute = "proxyTargetClass")
    boolean asyncProxyTargetClass() default true;
}
