package com.wenx.v3core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import jakarta.annotation.PostConstruct;

/**
 * AOP配置类
 * 确保AOP功能正确启用
 */
@Configuration
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class AopConfig {

    @PostConstruct
    public void init() {
        log.info("=== AOP配置已启用 ===");
    }
} 