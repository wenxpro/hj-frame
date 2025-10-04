package com.wenx.permission.context;

import com.wenx.anno.FieldPermission;
import com.wenx.v3secure.utils.LoginUser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段级权限上下文管理器
 * 管理字段级权限的验证和脱敏处理
 * 与现有的DataPermissionContextHolder配合工作
 * 
 * @author wenx
 * @since 1.0.0
 */
@Slf4j
public class FieldPermissionContextHolder {
    
    /**
     * 字段权限信息缓存
     * key: 类名.字段名, value: 字段权限信息
     */
    private static final Map<String, FieldPermissionInfo> FIELD_PERMISSION_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 线程本地的字段访问控制上下文
     */
    private static final ThreadLocal<FieldAccessContext> FIELD_ACCESS_CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置字段访问上下文
     */
    public static void setFieldAccessContext(FieldAccessContext context) {
        FIELD_ACCESS_CONTEXT.set(context);
    }
    
    /**
     * 获取字段访问上下文
     */
    public static FieldAccessContext getFieldAccessContext() {
        return FIELD_ACCESS_CONTEXT.get();
    }
    
    /**
     * 清理字段访问上下文
     */
    public static void clearFieldAccessContext() {
        FIELD_ACCESS_CONTEXT.remove();
    }
    
    /**
     * 验证字段访问权限
     * 
     * @param targetClass 目标类
     * @param fieldName 字段名
     * @param targetObject 目标对象（用于条件验证）
     * @return 字段权限验证结果
     */
    public static FieldPermissionResult verifyFieldAccess(Class<?> targetClass, String fieldName, Object targetObject) {
        try {
            Field field = getField(targetClass, fieldName);
            if (field == null) {
                return FieldPermissionResult.allow(); // 字段不存在，允许访问
            }
            
            FieldPermission annotation = field.getAnnotation(FieldPermission.class);
            if (annotation == null || !annotation.enabled()) {
                return FieldPermissionResult.allow(); // 没有权限注解或已禁用，允许访问
            }
            
            return doVerifyFieldAccess(annotation, field, targetObject);
            
        } catch (Exception e) {
            log.error("字段权限验证失败: {}.{}", targetClass.getSimpleName(), fieldName, e);
            return FieldPermissionResult.deny(); // 异常时拒绝访问
        }
    }
    
    /**
     * 执行字段权限验证
     */
    private static FieldPermissionResult doVerifyFieldAccess(FieldPermission annotation, Field field, Object targetObject) {
        // 超级管理员直接放行
        if (LoginUser.isSuperAdmin()) {
            return FieldPermissionResult.allow();
        }
        
        // 检查访问级别
        FieldPermission.AccessLevel level = annotation.level();
        if (level == FieldPermission.AccessLevel.PUBLIC) {
            return FieldPermissionResult.allow();
        }
        
        if (level == FieldPermission.AccessLevel.CONFIDENTIAL) {
            // 机密级别只有超级管理员才能访问（已在上面检查过）
            return FieldPermissionResult.deny();
        }
        
        // 检查权限编码
        String[] permissions = annotation.value();
        if (permissions.length > 0) {
            boolean hasPermission = Arrays.stream(permissions)
                    .anyMatch(perm -> LoginUser.hasPermission(perm) || LoginUser.hasSystemPermission(perm));
            
            if (!hasPermission) {
                return FieldPermissionResult.deny();
            }
        }
        
        // 检查角色要求
        String[] roles = annotation.roles();
        if (roles.length > 0) {
            boolean hasRole = Arrays.stream(roles)
                    .anyMatch(role -> LoginUser.hasPlatformRole(role) || LoginUser.hasSystemRole(role));
            
            if (!hasRole) {
                return FieldPermissionResult.deny();
            }
        }
        
        // 检查自定义条件
        String condition = annotation.condition();
        if (StringUtils.hasText(condition)) {
            boolean conditionResult = evaluateCondition(condition, targetObject);
            if (!conditionResult) {
                return FieldPermissionResult.deny();
            }
        }
        
        // 检查私有级别的数据所有者权限
        if (level == FieldPermission.AccessLevel.PRIVATE) {
            boolean isOwner = checkDataOwnership(targetObject);
            if (!isOwner) {
                return FieldPermissionResult.deny();
            }
        }
        
        return FieldPermissionResult.allow();
    }
    
    /**
     * 获取字段（支持继承）
     */
    private static Field getField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * 评估自定义条件表达式
     * 简化版实现，实际项目中可以集成SpEL表达式引擎
     */
    private static boolean evaluateCondition(String condition, Object targetObject) {
        try {
            // 简化版条件评估，实际可以使用SpEL
            if (condition.contains("user.departmentId == target.departmentId")) {
                return checkDepartmentMatch(targetObject);
            }
            
            if (condition.contains("user.userId == target.createBy")) {
                return checkCreatorMatch(targetObject);
            }
            
            // 默认返回true，实际项目中需要完整的SpEL支持
            log.warn("不支持的条件表达式: {}", condition);
            return true;
            
        } catch (Exception e) {
            log.error("条件表达式评估失败: {}", condition, e);
            return false;
        }
    }
    
    /**
     * 检查部门匹配
     */
    private static boolean checkDepartmentMatch(Object targetObject) {
        try {
            Long userDeptId = LoginUser.getDepartmentId();
            if (userDeptId == null) {
                return false;
            }
            
            Field deptField = getField(targetObject.getClass(), "departmentId");
            if (deptField == null) {
                deptField = getField(targetObject.getClass(), "deptId");
            }
            
            if (deptField != null) {
                Object targetDeptId = deptField.get(targetObject);
                return userDeptId.equals(targetDeptId);
            }
            
            return false;
        } catch (Exception e) {
            log.error("部门匹配检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查创建者匹配
     */
    private static boolean checkCreatorMatch(Object targetObject) {
        try {
            Long userId = LoginUser.getUserId();
            if (userId == null) {
                return false;
            }
            
            Field createByField = getField(targetObject.getClass(), "createBy");
            if (createByField == null) {
                createByField = getField(targetObject.getClass(), "createUser");
            }
            
            if (createByField != null) {
                Object createBy = createByField.get(targetObject);
                return userId.equals(createBy);
            }
            
            return false;
        } catch (Exception e) {
            log.error("创建者匹配检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查数据所有权
     */
    private static boolean checkDataOwnership(Object targetObject) {
        // 首先检查是否为创建者
        if (checkCreatorMatch(targetObject)) {
            return true;
        }
        
        // 然后检查是否为同部门
        return checkDepartmentMatch(targetObject);
    }
    
    /**
     * 字段权限验证结果
     */
    @Getter
    public static class FieldPermissionResult {
        private final boolean allowed;
        
        private FieldPermissionResult(boolean allowed) {
            this.allowed = allowed;
        }
        
        public static FieldPermissionResult allow() {
            return new FieldPermissionResult(true);
        }
        
        public static FieldPermissionResult deny() {
            return new FieldPermissionResult(false);
        }

    }
    
    /**
     * 字段访问上下文
     */
    public static class FieldAccessContext {
        private final Set<String> allowedFields;
        private final Set<String> deniedFields;

        public FieldAccessContext() {
            this.allowedFields = new HashSet<>();
            this.deniedFields = new HashSet<>();
        }
        
        public void allowField(String fieldName) {
            allowedFields.add(fieldName);
            deniedFields.remove(fieldName);
        }
        
        public void denyField(String fieldName) {
            deniedFields.add(fieldName);
            allowedFields.remove(fieldName);

        }
        
        public boolean isFieldAllowed(String fieldName) {
            return allowedFields.contains(fieldName);
        }
        
        public boolean isFieldDenied(String fieldName) {
            return deniedFields.contains(fieldName);
        }
        
        public Set<String> getAllowedFields() {
            return Collections.unmodifiableSet(allowedFields);
        }
        
        public Set<String> getDeniedFields() {
            return Collections.unmodifiableSet(deniedFields);
        }
    }

    /**
     * 字段权限信息
     *
     * @param fieldName Getters
     */
        public record FieldPermissionInfo(@Getter String fieldName, @Getter String className,
                                          @Getter FieldPermission annotation, boolean hasPermission) {
    }
}