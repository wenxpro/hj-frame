package com.wenx.v3secure.enums;

/**
 * 系统权限枚举
 * 定义系统级权限常量
 * 
 * @author wenx
 */
public class SystemPermission {
    
    // 系统级权限常量定义（以数据库 sys_permission.code 为准，统一 system: 前缀）
    public static final String SYSTEM_ALL_CODE = "system:*";
    public static final String SYSTEM_READ_CODE = "system:read";
    public static final String SYSTEM_CONFIG_CODE = "system:config";
    public static final String SYSTEM_MONITOR_CODE = "system:monitor";
    
    // 租户管理权限
    public static final String TENANT_MANAGE_CODE = "system:tenant:manage";
    public static final String TENANT_READ_CODE = "system:tenant:read";
    public static final String TENANT_ALL_CODE = "system:tenant:*";
    
    // 用户管理权限
    public static final String USER_READ_CODE = "system:user:read";
    public static final String USER_MANAGE_CODE = "system:user:manage";
    public static final String USER_ALL_CODE = "system:user:*";
    
    // 角色管理权限
    public static final String ROLE_READ_CODE = "system:role:read";
    public static final String ROLE_MANAGE_CODE = "system:role:manage";
    public static final String ROLE_ALL_CODE = "system:role:*";
    
    // 菜单管理权限
    public static final String MENU_READ_CODE = "system:menu:read";
    public static final String MENU_MANAGE_CODE = "system:menu:manage";
    public static final String MENU_ALL_CODE = "system:menu:*";
    
    // 部门管理权限
    public static final String DEPT_READ_CODE = "system:dept:read";
    public static final String DEPT_MANAGE_CODE = "system:dept:manage";
    public static final String DEPT_ALL_CODE = "system:dept:*";
    
    // 团队管理权限
    public static final String TEAM_READ_CODE = "system:team:read";
    public static final String TEAM_MANAGE_CODE = "system:team:manage";
    public static final String TEAM_ALL_CODE = "system:team:*";
    public static final String TEAM_MEMBER_MANAGE_CODE = "system:team:member:manage";
    public static final String TEAM_ROLE_MANAGE_CODE = "system:team:role:manage";
    
    // 业务功能权限
    public static final String BUSINESS_READ_CODE = "system:business:read";
    public static final String BUSINESS_MANAGE_CODE = "system:business:manage";
    public static final String BUSINESS_ALL_CODE = "system:business:*";
    
    // 数据管理权限
    public static final String DATA_READ_CODE = "system:data:read";
    public static final String DATA_MANAGE_CODE = "system:data:manage";
    public static final String DATA_ALL_CODE = "system:data:*";
    
    // 日志权限
    public static final String LOG_READ_CODE = "system:log:read";
    public static final String LOG_ALL_CODE = "system:log:*";
    
    // 审计权限
    public static final String AUDIT_READ_CODE = "system:audit:read";
    public static final String AUDIT_ALL_CODE = "system:audit:*";
    
    private final String code;
    private final String name;
    private final String description;
    
    SystemPermission(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
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
    
    /**
     * 检查权限是否匹配
     * 支持通配符权限检查
     */
    public static boolean hasPermission(String userPermission, String requiredPermission) {
        if (userPermission == null || requiredPermission == null) {
            return false;
        }
        
        // 系统超级权限
        if ("system:*".equals(userPermission)) {
            return true;
        }
        
        // 精确匹配
        if (userPermission.equals(requiredPermission)) {
            return true;
        }
        
        // 通配符匹配 (如 sys:user:* 包含 sys:user:read)
        if (userPermission.endsWith(":*")) {
            String prefix = userPermission.substring(0, userPermission.length() - 1);
            return requiredPermission.startsWith(prefix);
        }
        
        return false;
    }
}