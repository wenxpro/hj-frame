package com.wenx.v3secure.utils;

import com.wenx.v3secure.user.UserDetail;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class LoginUser {

    /**
     * 获取当前登录用户
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
     * 获取当前用户账号
     */
    public static String getAccount() {
        UserDetail user = getUser();
        return user != null ? user.getAccount() : null;
    }
    
    /**
     * 获取当前用户组织ID
     */
    public static Long getOrgId() {
        UserDetail user = getUser();
        return user != null ? user.getOrgId() : null;
    }
    
    /**
     * 判断当前用户是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        UserDetail user = getUser();
        return user != null && user.getSuperAdmin() != null && user.getSuperAdmin() == 1;
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