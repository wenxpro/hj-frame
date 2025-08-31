package com.wenx.v3secure.enums;

/**
 * 平台权限枚举
 * 
 * @author wenx
 */
public class PlatformPermission {
    
    // 权限常量定义，用于注解中的常量表达式
    public static final String PLATFORM_ALL_CODE = "platform:*";
    public static final String TENANT_READ_CODE = "platform:tenant:read";
    public static final String TENANT_ALL_CODE = "platform:tenant:*";
    public static final String PLATFORM_USER_READ_CODE = "platform:user:read";
    public static final String PLATFORM_USER_ALL_CODE = "platform:user:*";
    public static final String PLATFORM_ROLE_READ_CODE = "platform:role:read";
    public static final String PLATFORM_ROLE_ALL_CODE = "platform:role:*";
    public static final String SYSTEM_CONFIG_CODE = "platform:system:config";
    public static final String SYSTEM_ALL_CODE = "platform:system:*";
    public static final String MONITOR_VIEW_CODE = "platform:monitor:view";
    public static final String MONITOR_ALL_CODE = "platform:monitor:*";
    public static final String LOG_READ_CODE = "platform:log:read";
    public static final String LOG_ALL_CODE = "platform:log:*";
    
    private final String code;
    private final String name;
    private final String description;
    
    PlatformPermission(String code, String name, String description) {
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
        
        // 超级权限
        if ("platform:*".equals(userPermission)) {
            return true;
        }
        
        // 精确匹配
        if (userPermission.equals(requiredPermission)) {
            return true;
        }
        
        // 通配符匹配 (如 platform:tenant:* 包含 platform:tenant:read)
        if (userPermission.endsWith(":*")) {
            String prefix = userPermission.substring(0, userPermission.length() - 1);
            return requiredPermission.startsWith(prefix);
        }
        
        return false;
    }
}