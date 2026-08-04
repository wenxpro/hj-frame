package com.wenx.v3secure.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限通配匹配核心逻辑用例（G-4）
 * 覆盖：精确匹配 / 通配匹配 / 跨命名空间隔离 / 空值边界
 */
class PermissionUtilsTest {

    @Test
    void exactMatch() {
        assertTrue(PermissionUtils.hasPermission("system:user:read", "system:user:read"));
        assertTrue(PermissionUtils.hasPermission("platform:user:*", "platform:user:*"));
    }

    @Test
    void wildcardMatch() {
        // system 命名空间通配
        assertTrue(PermissionUtils.hasPermission("system:user:*", "system:user:read"));
        assertTrue(PermissionUtils.hasPermission("system:*", "system:user:add"));
        // platform 命名空间通配（E1/F2：平台权限码）
        assertTrue(PermissionUtils.hasPermission("platform:user:*", "platform:user:read"));
        assertTrue(PermissionUtils.hasPermission("platform:*", "platform:tenant:manage"));
        assertTrue(PermissionUtils.hasPermission("platform:tenant:*", "platform:tenant:read"));
        assertTrue(PermissionUtils.hasPermission("platform:role:*", "platform:role:read"));
    }

    @Test
    void wildcardBoundary() {
        // 通配符必须匹配完整段：system:user:* 不应匹配 system:userx:read
        assertFalse(PermissionUtils.hasPermission("system:user:*", "system:userx:read"));
        // 通配符在中间不生效（只支持段尾通配）
        assertFalse(PermissionUtils.hasPermission("system:*:read", "system:user:read"));
    }

    @Test
    void crossNamespaceIsolated() {
        // 跨命名空间不匹配（F1/E1：系统超管不放行平台接口的前提）
        assertFalse(PermissionUtils.hasPermission("system:*", "platform:user:read"));
        assertFalse(PermissionUtils.hasPermission("platform:*", "system:user:read"));
        assertFalse(PermissionUtils.hasPermission("system:user:*", "platform:user:read"));
    }

    @Test
    void nullAndBlank() {
        assertFalse(PermissionUtils.hasPermission(null, "system:user:read"));
        assertFalse(PermissionUtils.hasPermission("system:user:*", null));
        assertFalse(PermissionUtils.hasPermission("", "system:user:read"));
        assertFalse(PermissionUtils.hasPermission("system:user:*", ""));
    }
}
