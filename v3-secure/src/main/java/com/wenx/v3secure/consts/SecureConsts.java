package com.wenx.v3secure.consts;

import java.util.Map;
import java.util.Set;

public class SecureConsts {

    // 业务配置常量
    public static final int MAX_ROLES_PER_USER = 10;
    public static final Set<String> ADMIN_ROLE_CODES = Set.of("ADMIN", "SUPER_ADMIN");
    public static final Map<String, Set<String>> CONFLICT_ROLE_GROUPS = Map.of(
            "管理员与普通用户", Set.of("ADMIN", "USER"),
            "审计员与操作员", Set.of("AUDITOR", "OPERATOR"),
            "系统角色与业务角色", Set.of("SYSTEM", "BUSINESS")
    );

    // 缓存配置
    public static final String CACHE_USER_PERMISSIONS = "userPermissions";
    public static final String CACHE_USER_ROLES = "userRoles";
    public static final String CACHE_USER_MENUS = "userMenus";
    public static final String CACHE_USER_ORG_PERMISSIONS = "userOrgPermissions";
}
