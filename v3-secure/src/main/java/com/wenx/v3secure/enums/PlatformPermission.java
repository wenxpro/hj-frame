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
     * 检查权限是否匹配（委托给 PermissionUtils）
     */
    public static boolean hasPermission(String userPermission, String requiredPermission) {
        return com.wenx.v3secure.utils.PermissionUtils.hasPermission(userPermission, requiredPermission);
    }

    public static boolean isSystemPermission(String permission) {
        return com.wenx.v3secure.utils.PermissionUtils.belongsToNamespace(permission, "system");
    }

    public static boolean isPlatformPermission(String permission) {
        return com.wenx.v3secure.utils.PermissionUtils.belongsToNamespace(permission, "platform");
    }

    public static String parseModule(String permission) {
        return com.wenx.v3secure.utils.PermissionUtils.parseModule(permission);
    }

    public static String parseAction(String permission) {
        return com.wenx.v3secure.utils.PermissionUtils.parseAction(permission);
    }

}