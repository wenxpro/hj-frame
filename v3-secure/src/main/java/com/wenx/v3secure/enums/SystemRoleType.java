package com.wenx.v3secure.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 系统角色类型枚举
 * 定义系统内部角色层级和权限
 * 
 * @author wenx
 */
public enum SystemRoleType {
    
    SYSTEM_ADMIN("system_admin", "系统管理员", "负责系统配置和管理", 1,
            Arrays.asList(SystemPermission.SYSTEM_ALL_CODE)),
    
    TENANT_ADMIN("tenant_admin", "租户管理员", "负责租户内部管理", 2,
            Arrays.asList(SystemPermission.TENANT_MANAGE_CODE, SystemPermission.USER_ALL_CODE, 
                         SystemPermission.ROLE_ALL_CODE, SystemPermission.MENU_ALL_CODE)),
    
    DEPT_ADMIN("dept_admin", "部门管理员", "负责部门用户和权限管理", 3,
            Arrays.asList(SystemPermission.DEPT_MANAGE_CODE, SystemPermission.USER_MANAGE_CODE,
                         SystemPermission.ROLE_READ_CODE)),
    
    BUSINESS_ADMIN("business_admin", "业务管理员", "负责业务功能管理", 4,
            Arrays.asList(SystemPermission.BUSINESS_ALL_CODE, SystemPermission.DATA_MANAGE_CODE)),
    
    OPERATOR("operator", "操作员", "普通业务操作人员", 5,
            Arrays.asList(SystemPermission.BUSINESS_READ_CODE, SystemPermission.DATA_READ_CODE)),
    
    VIEWER("viewer", "查看者", "只读权限用户", 6,
            Arrays.asList(SystemPermission.SYSTEM_READ_CODE));
    
    private final String code;
    private final String name;
    private final String description;
    private final Integer level;
    private final List<String> permissions;
    
    SystemRoleType(String code, String name, String description, Integer level, List<String> permissions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.level = level;
        this.permissions = permissions;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    /**
     * 根据代码获取角色类型
     */
    public static SystemRoleType fromCode(String code) {
        for (SystemRoleType roleType : values()) {
            if (roleType.getCode().equals(code)) {
                return roleType;
            }
        }
        return null;
    }
    
    /**
     * 检查是否拥有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || 
               permissions.stream().anyMatch(p -> SystemPermission.hasPermission(p, permission));
    }
    
    /**
     * 获取所有角色代码
     */
    public static List<String> getAllRoleCodes() {
        return Arrays.stream(values())
                .map(SystemRoleType::getCode)
                .toList();
    }
}