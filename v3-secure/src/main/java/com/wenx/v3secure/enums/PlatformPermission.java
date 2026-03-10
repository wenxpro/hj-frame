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
    public static final String PLATFORM_MENU_READ_CODE = "platform:menu:read";
    public static final String SYSTEM_CONFIG_CODE = "platform:system:config";
    public static final String SYSTEM_ALL_CODE = "platform:system:*";

    
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
    
    /**
     * 判断是否为系统权限
     * @param permission 权限代码
     * @return 是否为系统权限
     */
    public static boolean isSystemPermission(String permission) {
        return permission != null && permission.startsWith("system:");
    }
    
    /**
     * 判断是否为平台权限
     * @param permission 权限代码
     * @return 是否为平台权限
     */
    public static boolean isPlatformPermission(String permission) {
        return permission != null && permission.startsWith("platform:");
    }
    
    /**
     * 解析权限模块
     * @param permission 权限代码
     * @return 权限模块 (如: user, role, menu等)
     */
    public static String parseModule(String permission) {
        if (permission == null || !permission.contains(":")) {
            return null;
        }
        
        String[] parts = permission.split(":");
        if (parts.length >= 2) {
            return parts[1];
        }
        
        return null;
    }
    
    /**
     * 解析权限操作
     * @param permission 权限代码
     * @return 权限操作 (如: read, write, delete等)
     */
    public static String parseAction(String permission) {
        if (permission == null || !permission.contains(":")) {
            return null;
        }
        
        String[] parts = permission.split(":");
        if (parts.length >= 3) {
            return parts[2];
        }
        
        return null;
    }

}