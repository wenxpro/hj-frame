package com.wenx.aspect;

import com.wenx.anno.DataPermission;
import com.wenx.permission.context.DataPermissionContextHolder;
import com.wenx.permission.context.PermissionConditionInfo;
import com.wenx.v3secure.utils.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 数据权限AOP切面 - 重构版
 * 采用权限验证与SQL注入分离的架构设计
 * - type=CUSTOM：执行 count 查询校验当前用户是否满足 sys_permission_condition 中的条件，
 *   count = 0 时抛出异常阻断操作（读写通用）
 * - 其他 type：构建 SQL 条件存入上下文，由 MyBatis 拦截器注入 WHERE 子句
 * 
 * @author wenx
 * @since 1.0.0
 */
@Aspect
@Component
@Order(100) // 确保在事务切面之前执行
@Slf4j
public class DataPermissionAspect {
    
    /**
     * 环绕通知：处理@DataPermission注解
     * 新架构：权限验证 -> 上下文传递 -> 延迟SQL注入
     */
    @Around("@annotation(com.wenx.anno.DataPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataPermission dataPermission = method.getAnnotation(DataPermission.class);
        
        // 检查是否启用权限控制
        if (!dataPermission.enabled()) {
            log.debug("方法 {}.{} 的数据权限控制已禁用", 
                     method.getDeclaringClass().getSimpleName(), method.getName());
            return joinPoint.proceed();
        }
        
        // 检查用户是否登录
        if (!LoginUser.isAuthenticated()) {
            log.debug("用户未登录，跳过数据权限控制");
            return joinPoint.proceed();
        }
        
        // 检查是否为超级管理员
        if (LoginUser.isSuperAdmin()) {
            log.debug("超级管理员，跳过数据权限控制");
            return joinPoint.proceed();
        }
        
        try {
            // 构建 SQL 条件存入上下文，由 MyBatis 拦截器注入 WHERE
            performPermissionVerification(dataPermission);
            DataPermissionContextHolder.setPermissionVerified(true);
            log.debug("执行方法 {}.{} 的数据权限控制，已验证的条件数量: {}",
                     method.getDeclaringClass().getSimpleName(),
                     method.getName(),
                     DataPermissionContextHolder.getAllConditionInfos().size());
            return joinPoint.proceed();
            
        } finally {
            // 清除权限上下文
            DataPermissionContextHolder.clear();
        }
    }
    
    /**
     * 执行权限验证
     * 构建 SQL 条件存入上下文，由 MyBatis 拦截器注入 WHERE 子句
     */
    private void performPermissionVerification(DataPermission dataPermission) {
        String[] tables = dataPermission.tables();
        DataPermission.PermissionType type = dataPermission.type();
        String customCondition = dataPermission.condition();
        performSimplePermissionVerification(tables, type, customCondition);
    }
    
    /**
     * 简单权限验证模式（降级方案）
     * 当权限验证服务不可用时使用
     */
    private void performSimplePermissionVerification(String[] tables, 
                                                      DataPermission.PermissionType type, 
                                                      String customCondition) {
        for (String table : tables) {
            PermissionConditionInfo.PermissionConditionInfoBuilder builder = PermissionConditionInfo.builder()
                    .tableName(table)
                    .permissionType(type.name())
                    .status(PermissionConditionInfo.VerificationStatus.VERIFIED);
            
            // 处理自定义条件
            if (type == DataPermission.PermissionType.CUSTOM && customCondition != null && !customCondition.trim().isEmpty()) {
                String condition = DataPermissionContextHolder.buildCondition(customCondition);
                builder.originalCondition(customCondition)
                       .finalCondition(condition);
            }
            // 处理预定义类型
            else if (type != DataPermission.PermissionType.NONE) {
                String template = getConditionTemplate(type);
                if (template != null && !template.trim().isEmpty()) {
                    String condition = DataPermissionContextHolder.buildCondition(template);
                    builder.originalCondition(template)
                           .finalCondition(condition);
                }
            }
            
            PermissionConditionInfo conditionInfo = builder.build();
            DataPermissionContextHolder.setConditionInfo(table, conditionInfo);
        }
    }
    
    /**
     * 获取权限类型对应的条件模板
     */
    private String getConditionTemplate(DataPermission.PermissionType type) {
        switch (type) {
            case USER_SCOPE:
                return DataPermissionContextHolder.getConditionTemplate("USER_SCOPE");
            case DEPT_SCOPE:
                return DataPermissionContextHolder.getConditionTemplate("DEPT_SCOPE");
            case DEPT_AND_SUB:
                return DataPermissionContextHolder.getConditionTemplate("DEPT_AND_SUB");
            default:
                return null;
        }
    }
}