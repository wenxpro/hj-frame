package com.wenx.v3secure.utils;

import com.wenx.v3core.constant.CommonConstants;
import com.wenx.v3secure.user.UserDetail;
import com.wenx.v3secure.enums.PlatformPermission;
import com.wenx.v3secure.enums.PlatformRoleType;
import com.wenx.v3secure.enums.SystemPermission;
import com.wenx.v3secure.enums.SystemRoleType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

/**
 * 登录用户工具类
 * 
 * @author wenx
 * @description 提供获取当前登录用户信息的静态方法
 */
public class LoginUser {

    /**
     * 获取当前登录用户
     * 返回SecurityContext中的UserDetail对象
     */
    public static UserDetail getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetail userDetail) {
                return userDetail;
            }
        }
        return new UserDetail();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        UserDetail user = getUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        UserDetail user = getUser();
        return user != null ? user.getUsername() : null;
    }
    

    
    /**
     * 获取当前用户部门ID
     */
    public static Long getDepartmentId() {
        UserDetail user = getUser();
        return user != null ? user.getDepartmentId() : null;
    }
    
    /**
     * 判断当前用户是否为超级管理员
     * 综合多种条件判断：superAdmin字段、用户名、用户ID、角色权限
     */
    public static boolean isSuperAdmin() {
        UserDetail user = getUser();
        if (user == null) {
            return false;
        }
        
        // 方式1：检查superAdmin字段标识
        if (user.getSuperAdmin() != null && user.getSuperAdmin() == 1) {
            return true;
        }
        
        // 方式2：检查用户名（数据库和缓存中的admin用户确实是超管）
        if ("admin".equals(user.getUsername()) || "superadmin".equals(user.getUsername())) {
            return true;
        }
        
        // 方式3：检查用户ID（超级管理员通常是ID为1的用户）
        if (user.getId() != null && user.getId().equals(CommonConstants.SUPER_ADMIN_ID)) {
            return true;
        }
        
        // 方式4：检查角色权限（拥有超级管理员角色）
        Set<String> authorities = user.getAuthoritySet();
        if (authorities != null) {
            if (authorities.contains("ROLE_SUPER_ADMIN") || 
                authorities.contains("ROLE_super_admin") ||
                authorities.contains("super_admin")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断当前用户是否为平台管理员
     * 支持通配符权限检查，如platform:*、platform:tenant:*等
     */
    public static boolean isPlatformAdmin() {
        Set<String> authorities = getAuthorities();
        
        // 检查角色权限
        if (authorities.contains("ROLE_PLATFORM_ADMIN")) {
            return true;
        }
        
        // 检查平台通配符权限
        if (authorities.contains(PlatformPermission.PLATFORM_ALL_CODE)) {
            return true;
        }
        
        // 检查具体的平台管理权限
        String[] platformPermissions = {
            PlatformPermission.TENANT_ALL_CODE,
            PlatformPermission.PLATFORM_USER_ALL_CODE,
            PlatformPermission.SYSTEM_ALL_CODE,
            PlatformPermission.PLATFORM_ROLE_ALL_CODE
        };
        
        for (String permission : platformPermissions) {
            if (authorities.contains(permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查权限是否匹配（支持通配符）
     */
    public static boolean hasPermission(String requiredPermission) {
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return false;
        }
        
        Set<String> authorities = getAuthorities();
        
        // 使用PlatformPermission中的权限匹配逻辑
        for (String authority : authorities) {
            if (PlatformPermission.hasPermission(authority, requiredPermission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查权限是否匹配
     * 支持通配符权限检查
     */
    public static boolean hasPermission(String userPermission, String requiredPermission) {
        return PlatformPermission.hasPermission(userPermission, requiredPermission);
    }
    
    /**
     * 检查当前用户是否拥有指定的平台角色
     */
    public static boolean hasPlatformRole(PlatformRoleType roleType) {
        UserDetail userDetail = getUser();
        return userDetail != null && userDetail.hasPlatformRole(roleType);
    }
    
    /**
     * 检查当前用户是否拥有指定角色代码的平台角色
     */
    public static boolean hasPlatformRole(String roleCode) {
        UserDetail userDetail = getUser();
        return userDetail != null && userDetail.hasPlatformRole(roleCode);
    }
    
    /**
     * 获取当前用户的平台角色类型
     */
    public static PlatformRoleType getPlatformRoleType() {
        UserDetail userDetail = getUser();
        return userDetail != null ? userDetail.getPlatformRoleType() : null;
    }
    
    /**
     * 检查当前用户是否拥有指定的系统角色
     */
    public static boolean hasSystemRole(SystemRoleType roleType) {
        UserDetail userDetail = getUser();
        return userDetail != null && userDetail.hasSystemRole(roleType);
    }
    
    /**
     * 检查当前用户是否拥有指定角色代码的系统角色
     */
    public static boolean hasSystemRole(String roleCode) {
        UserDetail userDetail = getUser();
        return userDetail != null && userDetail.hasSystemRole(roleCode);
    }
    
    /**
     * 获取当前用户的系统角色类型
     */
    public static SystemRoleType getSystemRoleType() {
        UserDetail userDetail = getUser();
        return userDetail != null ? userDetail.getSystemRoleType() : null;
    }
    

    
    /**
     * 检查系统权限是否匹配（支持通配符）
     */
    public static boolean hasSystemPermission(String requiredPermission) {
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return false;
        }
        
        Set<String> authorities = getAuthorities();
        
        // 使用SystemPermission中的权限匹配逻辑
        for (String authority : authorities) {
            if (SystemPermission.hasPermission(authority, requiredPermission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取当前用户权限集合
     */
    public static Set<String> getAuthorities() {
        UserDetail user = getUser();
        return user != null && user.getAuthoritySet() != null 
                ? user.getAuthoritySet() 
                : Collections.emptySet();
    }
    
    /**
     * 判断当前用户是否拥有指定权限
     */
    public static boolean hasAuthority(String authority) {
        if (authority == null || authority.isEmpty()) {
            return false;
        }
        return getAuthorities().contains(authority);
    }
    
    /**
     * 判断当前用户是否拥有任一指定权限
     */
    public static boolean hasAnyAuthority(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            return false;
        }
        Set<String> userAuthorities = getAuthorities();
        for (String authority : authorities) {
            if (userAuthorities.contains(authority)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断当前用户是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null 
                && authentication.isAuthenticated() 
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
    
    /**
     * 获取当前用户数据权限范围
     */
    public static List<Long> getDataScopeList() {
        UserDetail user = getUser();
        return user != null ? user.getDataScopeList() : Collections.emptyList();
    }
    
    /**
     * 判断是否有全部数据权限
     */
    public static boolean hasAllDataScope() {
        UserDetail user = getUser();
        return user != null && user.getDataScopeList() == null;
    }
}