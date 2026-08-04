package com.wenx.v3secure.utils;

/**
 * 权限工具类
 * 统一权限匹配、解析逻辑，消除 SystemPermission 和 PlatformPermission 的重复代码
 *
 * @author wenx
 * @since 1.0.0
 */
public final class PermissionUtils {

    private PermissionUtils() {}

    /**
     * 检查权限是否匹配（支持通配符）
     *
     * @param userPermission     用户持有的权限码
     * @param requiredPermission 所需的权限码
     * @return 是否匹配
     */
    public static boolean hasPermission(String userPermission, String requiredPermission) {
        if (userPermission == null || requiredPermission == null) {
            return false;
        }

        // 精确匹配
        if (userPermission.equals(requiredPermission)) {
            return true;
        }

        // 通配符匹配 (如 system:user:* 包含 system:user:read)
        if (userPermission.endsWith(":*")) {
            String prefix = userPermission.substring(0, userPermission.length() - 1);
            return requiredPermission.startsWith(prefix);
        }

        return false;
    }

    /**
     * 判断权限码是否属于指定命名空间
     */
    public static boolean belongsToNamespace(String permission, String namespace) {
        return permission != null && permission.startsWith(namespace + ":");
    }

    /**
     * 解析权限模块 (如 "system:user:read" → "user")
     */
    public static String parseModule(String permission) {
        if (permission == null || !permission.contains(":")) {
            return null;
        }
        String[] parts = permission.split(":");
        return parts.length >= 2 ? parts[1] : null;
    }

    /**
     * 解析权限操作 (如 "system:user:read" → "read")
     */
    public static String parseAction(String permission) {
        if (permission == null || !permission.contains(":")) {
            return null;
        }
        String[] parts = permission.split(":");
        return parts.length >= 3 ? parts[2] : null;
    }
}
