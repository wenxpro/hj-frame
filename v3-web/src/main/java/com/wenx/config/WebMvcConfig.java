package com.wenx.config;

import com.wenx.v3log.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wenx
 * @description mvc 配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 解决静态资源无法访问
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        // 解决swagger无法访问
        registry.addResourceHandler("/swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        // 解决swagger的js文件无法访问
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 拦截器配置
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 无需拦截的接口集合
        List<String> ignorePath = new ArrayList<>();
        // swagger
        ignorePath.add("/swagger-resources/**");
        ignorePath.add("/doc.html");
        ignorePath.add("/favicon.ico");
        ignorePath.add("/v3/**");
        ignorePath.add("/webjars/**");
        ignorePath.add("/springdoc/**");
        ignorePath.add("/static/**");
        ignorePath.add("/templates/**");
        ignorePath.add("/error");
        ignorePath.add("/cipher/check");
        ignorePath.add("/manager/login");
        ignorePath.add("/swagger-ui.html");
        //先拦截认证，再拦截授权
        registry.addInterceptor(requestInterceptor()).addPathPatterns("/**").excludePathPatterns(ignorePath);
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor();
    }

    @Bean
    public FormContentFilter formContentFilter() {
        return new FormContentFilter();
    }
}