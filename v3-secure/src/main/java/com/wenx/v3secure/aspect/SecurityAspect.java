package com.wenx.v3secure.aspect;

import com.wenx.v3secure.annotation.RequiresPermissions;
import com.wenx.v3secure.annotation.RequiresRoles;
import com.wenx.v3secure.exception.UnauthorizedException;
import com.wenx.v3secure.utils.LoginUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 安全注解切面
 * 
 * @author wenx
 * @description 处理权限和角色验证注解
 */
@Aspect
@Component
public class SecurityAspect {

    /**
     * 权限注解切点
     */
    @Pointcut("@annotation(com.wenx.v3secure.annotation.RequiresPermissions)")
    public void requiresPermissionsPointcut() {}
    
    /**
     * 角色注解切点
     */
    @Pointcut("@annotation(com.wenx.v3secure.annotation.RequiresRoles)")
    public void requiresRolesPointcut() {}
    
    /**
     * 处理权限验证
     */
    @Around("requiresPermissionsPointcut()")
    public Object checkPermissions(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查用户是否登录
        if (!LoginUser.isAuthenticated()) {
            throw new UnauthorizedException("用户未登录");
        }
        
        // 超级管理员直接放行
        if (LoginUser.isSuperAdmin()) {
            return joinPoint.proceed();
        }
        
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermissions annotation = method.getAnnotation(RequiresPermissions.class);
        
        if (annotation != null) {
            String[] requiredPermissions = annotation.value();
            RequiresPermissions.Logical logical = annotation.logical();
            
            // 验证权限
            boolean hasPermission = false;
            if (logical == RequiresPermissions.Logical.AND) {
                // 必须拥有所有权限
                hasPermission = Arrays.stream(requiredPermissions)
                        .allMatch(permission -> LoginUser.hasPermission(permission) || LoginUser.hasSystemPermission(permission));
            } else {
                // 只需拥有其中一个权限
                hasPermission = Arrays.stream(requiredPermissions)
                        .anyMatch(permission -> LoginUser.hasPermission(permission) || LoginUser.hasSystemPermission(permission));
            }
            
            if (!hasPermission) {
                throw new UnauthorizedException("权限不足: " + Arrays.toString(requiredPermissions));
            }
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 处理角色验证
     */
    @Around("requiresRolesPointcut()")
    public Object checkRoles(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查用户是否登录
        if (!LoginUser.isAuthenticated()) {
            throw new UnauthorizedException("用户未登录");
        }
        
        // 超级管理员直接放行
        if (LoginUser.isSuperAdmin()) {
            return joinPoint.proceed();
        }
        
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRoles annotation = method.getAnnotation(RequiresRoles.class);
        
        if (annotation != null) {
            String[] requiredRoles = annotation.value();
            RequiresRoles.Logical logical = annotation.logical();
            
            // 验证角色
            boolean hasRole = false;
            if (logical == RequiresRoles.Logical.AND) {
                // 必须拥有所有角色
                hasRole = Arrays.stream(requiredRoles)
                        .allMatch(role -> LoginUser.hasPlatformRole(role) || LoginUser.hasSystemRole(role));
            } else {
                // 只需拥有其中一个角色
                hasRole = Arrays.stream(requiredRoles)
                        .anyMatch(role -> LoginUser.hasPlatformRole(role) || LoginUser.hasSystemRole(role));
            }
            
            if (!hasRole) {
                throw new UnauthorizedException("角色权限不足: " + Arrays.toString(requiredRoles));
            }
        }
        
        return joinPoint.proceed();
    }
}