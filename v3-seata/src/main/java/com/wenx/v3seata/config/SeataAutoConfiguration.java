package com.wenx.v3seata.config;

import com.wenx.v3seata.filter.SeataXidFilter;
import com.wenx.v3seata.interceptor.SeataFeignInterceptor;
import com.wenx.v3seata.interceptor.SeataRestTemplateInterceptor;
import io.seata.spring.annotation.datasource.EnableAutoDataSourceProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

/**
 * Seata 自动配置
 * 使用 Seata 默认配置，无需额外配置
 * 
 * @author v3-cloud
 * @since 1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAutoDataSourceProxy
public class SeataAutoConfiguration {
    
    /**
     * Seata XID 过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public SeataXidFilter seataXidFilter() {
        return new SeataXidFilter();
    }
    
    /**
     * RestTemplate 拦截器配置
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnMissingBean
    public SeataRestTemplateInterceptor seataRestTemplateInterceptor() {
        return new SeataRestTemplateInterceptor();
    }
    
    /**
     * 配置支持 Seata 的 RestTemplate
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnMissingBean(name = "seataRestTemplate")
    @LoadBalanced
    public RestTemplate seataRestTemplate(SeataRestTemplateInterceptor interceptor) {
        var restTemplate = new RestTemplate();
        var interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.add(interceptor);
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
    
    /**
     * Feign 拦截器配置
     */
    @Bean
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    @ConditionalOnMissingBean
    public SeataFeignInterceptor seataFeignInterceptor() {
        return new SeataFeignInterceptor();
    }
} 