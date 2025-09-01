package com.wenx.v3secure.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 平台角色类型枚举
 * 简化为4个核心角色，与前端保持一致
 * 
 * @author wenx
 */
public enum PlatformRoleType {
    
    SUPER_ADMIN("super_admin", "超级管理员", "拥有平台所有权限",
            Arrays.asList(PlatformPermission.PLATFORM_ALL_CODE)),
    
    PLATFORM_ADMIN("platform_admin", "平台管理员", "负责租户和用户管理",
            Arrays.asList(PlatformPermission.TENANT_ALL_CODE, PlatformPermission.PLATFORM_USER_ALL_CODE)),
    
    PLATFORM_SUPPORT("platform_support", "平台支持", "负责租户支持",
            Arrays.asList(PlatformPermission.TENANT_READ_CODE)),
    
    PLATFORM_AUDITOR("platform_auditor", "平台审计员", "负责系统审计",
            Arrays.asList(PlatformPermission.SYSTEM_CONFIG_CODE));
    
    private final String code;
    private final String name;
    private final String description;
    private final List<String> permissions;
    
    PlatformRoleType(String code, String name, String description, List<String> permissions) {
        this.code = code;
        this.name = name;
        this.description = description;
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
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    /**
     * 根据角色代码获取角色类型
     */
    public static PlatformRoleType fromCode(String code) {
        for (PlatformRoleType roleType : values()) {
            if (roleType.getCode().equals(code)) {
                return roleType;
            }
        }
        return null;
    }
    
    /**
     * 检查角色是否有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions.stream()
                .anyMatch(p -> PlatformPermission.hasPermission(p, permission));
    }
    
    /**
     * 获取所有内置角色代码
     */
    public static List<String> getAllRoleCodes() {
        return Arrays.stream(values())
                .map(PlatformRoleType::getCode)
                .toList();
    }
}