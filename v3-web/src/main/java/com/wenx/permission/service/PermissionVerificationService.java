package com.wenx.permission.service;

import com.wenx.permission.context.PermissionConditionInfo;

import java.util.List;

/**
 * 权限验证服务接口
 * 负责权限条件的验证和处理逻辑
 * 支持权限验证与SQL注入分离的架构设计
 * 
 * @author wenx
 * @since 1.0.0
 */
public interface PermissionVerificationService {
    
    /**
     * 验证并构建权限条件
     * 
     * @param tableName 表名
     * @param permissionType 权限类型
     * @param customCondition 自定义条件模板
     * @return 权限条件信息
     */
    PermissionConditionInfo verifyAndBuildCondition(String tableName, String permissionType, String customCondition);
    
    /**
     * 批量验证权限条件
     * 
     * @param tableNames 表名列表
     * @param permissionType 权限类型
     * @param customCondition 自定义条件模板
     * @return 权限条件信息列表
     */
    List<PermissionConditionInfo> verifyAndBuildConditions(String[] tableNames, String permissionType, String customCondition);
    
    /**
     * 获取数据库配置的权限条件
     * 
     * @param tableName 表名
     * @return 数据库权限条件列表
     */
    List<PermissionConditionInfo.DatabaseCondition> getDatabaseConditions(String tableName);
    
    /**
     * 构建最终的SQL条件
     * 
     * @param conditionInfo 权限条件信息
     * @return 最终的SQL条件字符串
     */
    String buildFinalSqlCondition(PermissionConditionInfo conditionInfo);
    
    /**
     * 验证用户是否有权限访问指定表
     * 
     * @param tableName 表名
     * @return 是否有权限
     */
    boolean hasTablePermission(String tableName);
}