package com.wenx.v3secure.service;

import com.wenx.v3secure.user.UserDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author wenx
 * @description 登录用户信息工具类
 */
@Slf4j
public class LoginUser {

    /**
     * 获取当前登录用户
     * 
     * @return 用户详情，如果未登录返回空的UserDetail对象
     */
    public static UserDetail getUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                && !(authentication instanceof AnonymousAuthenticationToken)) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetail) {
                    return (UserDetail) principal;
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
        }
        return new UserDetail();
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，未登录返回null
     */
    public static Long getUserId() {
        UserDetail user = getUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 用户名，未登录返回null
     */
    public static String getUsername() {
        UserDetail user = getUser();
        return user != null ? user.getUsername() : null;
    }
    
    /**
     * 获取当前用户账号
     * 
     * @return 账号，未登录返回null
     */
    public static String getAccount() {
        UserDetail user = getUser();
        return user != null ? user.getAccount() : null;
    }
    
    /**
     * 获取当前用户组织ID
     * 
     * @return 组织ID，未登录返回null
     */
    public static Long getOrgId() {
        UserDetail user = getUser();
        return user != null ? user.getOrgId() : null;
    }
    
    /**
     * 获取当前用户邮箱
     * 
     * @return 邮箱，未登录返回null
     */
    public static String getEmail() {
        UserDetail user = getUser();
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * 获取当前用户手机号
     * 
     * @return 手机号，未登录返回null
     */
    public static String getMobile() {
        UserDetail user = getUser();
        return user != null ? user.getMobile() : null;
    }
    
    /**
     * 判断当前用户是否为超级管理员
     * 
     * @return true表示是超级管理员
     */
    public static boolean isSuperAdmin() {
        UserDetail user = getUser();
        return user != null && user.getSuperAdmin() != null && user.getSuperAdmin() == 1;
    }
    
    /**
     * 获取当前用户权限集合
     * 
     * @return 权限集合，未登录返回空集合
     */
    public static Set<String> getAuthorities() {
        UserDetail user = getUser();
        return user != null && user.getAuthoritySet() != null 
                ? user.getAuthoritySet() 
                : Collections.emptySet();
    }
    
    /**
     * 获取Spring Security的权限集合
     * 
     * @return GrantedAuthority集合
     */
    public static Collection<? extends GrantedAuthority> getGrantedAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities();
        }
        return Collections.emptyList();
    }
    
    /**
     * 判断当前用户是否拥有指定权限
     * 
     * @param authority 权限标识
     * @return true表示拥有该权限
     */
    public static boolean hasAuthority(String authority) {
        if (authority == null || authority.isEmpty()) {
            return false;
        }
        return getAuthorities().contains(authority);
    }
    
    /**
     * 判断当前用户是否拥有任一指定权限
     * 
     * @param authorities 权限标识集合
     * @return true表示拥有其中任一权限
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
     * 判断当前用户是否拥有所有指定权限
     * 
     * @param authorities 权限标识集合
     * @return true表示拥有所有权限
     */
    public static boolean hasAllAuthorities(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            return false;
        }
        Set<String> userAuthorities = getAuthorities();
        for (String authority : authorities) {
            if (!userAuthorities.contains(authority)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 判断当前用户是否已登录
     * 
     * @return true表示已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null 
                && authentication.isAuthenticated() 
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
    
    /**
     * 获取当前用户数据权限范围
     * 
     * @return 数据权限范围列表，null表示全部数据权限
     */
    public static List<Long> getDataScopeList() {
        UserDetail user = getUser();
        return user != null ? user.getDataScopeList() : Collections.emptyList();
    }
    
    /**
     * 判断是否有全部数据权限
     * 
     * @return true表示有全部数据权限
     */
    public static boolean hasAllDataScope() {
        UserDetail user = getUser();
        return user != null && user.getDataScopeList() == null;
    }
    
    /**
     * 清除当前用户的认证信息
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
    
    /**
     * 设置认证信息到上下文
     * 
     * @param authentication 认证信息
     */
    public static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    /**
     * 获取当前的认证信息
     * 
     * @return 认证信息
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    /**
     * 判断当前用户是否拥有指定角色
     * 
     * @param role 角色标识（自动添加ROLE_前缀）
     * @return true表示拥有该角色
     */
    public static boolean hasRole(String role) {
        if (role == null || role.isEmpty()) {
            return false;
        }
        String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return hasAuthority(roleAuthority);
    }
    
    /**
     * 判断当前用户是否拥有任一指定角色
     * 
     * @param roles 角色标识集合
     * @return true表示拥有其中任一角色
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
