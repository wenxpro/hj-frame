package com.wenx.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 数据源配置
 * 
 * @author wenx
 * @description 配置Druid数据源
 */
@Slf4j
@Configuration
public class DataSourceConfig {
    
    /**
     * 配置主数据源
     * 使用Druid连接池
     */
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.druid")
    public DataSource dataSource() {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        try {
            // 配置监控统计拦截的filters
            dataSource.setFilters("stat,wall,slf4j");
            // 配置连接池参数
            dataSource.setInitialSize(5);
            dataSource.setMinIdle(5);
            dataSource.setMaxActive(20);
            dataSource.setMaxWait(60000);
            dataSource.setTimeBetweenEvictionRunsMillis(60000);
            dataSource.setMinEvictableIdleTimeMillis(300000);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnBorrow(false);
            dataSource.setTestOnReturn(false);
            log.info("Druid数据源初始化成功");
        } catch (SQLException e) {
            log.error("Druid数据源初始化失败", e);
        }
        return dataSource;
    }
} 