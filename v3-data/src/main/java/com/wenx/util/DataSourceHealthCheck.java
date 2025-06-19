package com.wenx.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据源健康检查
 * 
 * @author wenx
 * @description 检查数据源连接状态
 */
@Slf4j
@Component
public class DataSourceHealthCheck {
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 检查数据源是否可用
     * 
     * @return true-可用，false-不可用
     */
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(3);
        } catch (SQLException e) {
            log.error("数据源健康检查失败", e);
            return false;
        }
    }
    
    /**
     * 获取数据源连接信息
     * 
     * @return 连接信息
     */
    public String getConnectionInfo() {
        try (Connection connection = dataSource.getConnection()) {
            return String.format("数据库产品: %s, 版本: %s, 驱动: %s",
                    connection.getMetaData().getDatabaseProductName(),
                    connection.getMetaData().getDatabaseProductVersion(),
                    connection.getMetaData().getDriverName());
        } catch (SQLException e) {
            log.error("获取数据源信息失败", e);
            return "无法获取数据源信息";
        }
    }
} 