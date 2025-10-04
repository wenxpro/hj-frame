package com.wenx.permission.context;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 权限条件信息封装
 * 用于在权限验证和SQL注入之间传递数据
 * 
 * @author wenx
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionConditionInfo {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 权限类型 (USER_SCOPE, DEPT_SCOPE, CUSTOM等)
     */
    private String permissionType;
    
    /**
     * 原始权限条件模板
     */
    private String originalCondition;
    
    /**
     * 构建后的最终SQL条件
     */
    private String finalCondition;
    
    /**
     * 权限验证状态
     */
    private VerificationStatus status;
    
    /**
     * 权限上下文参数
     */
    private Map<String, Object> contextParams;
    
    /**
     * 数据库配置的权限条件列表
     */
    private List<DatabaseCondition> databaseConditions;
    
    /**
     * 错误信息（如果验证失败）
     */
    private String errorMessage;
    
    /**
     * 权限验证状态枚举
     */
    public enum VerificationStatus {
        /**
         * 待验证
         */
        PENDING,
        
        /**
         * 验证通过
         */
        VERIFIED,
        
        /**
         * 验证失败
         */
        FAILED,
        
        /**
         * 跳过验证（如超级管理员）
         */
        SKIPPED
    }
    
    /**
     * 数据库配置的权限条件
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatabaseCondition {
        private String field;
        private String operator;
        private String value;
        private String contextField;
        private Integer status;
        private String type;
        private String description;
    }
}