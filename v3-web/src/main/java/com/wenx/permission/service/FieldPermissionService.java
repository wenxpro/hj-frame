package com.wenx.permission.service;

import com.wenx.anno.FieldPermission;
import com.wenx.permission.context.FieldPermissionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段级权限服务
 * 提供字段权限的管理和验证服务
 * 与数据库配置的权限条件集成
 * 注意：脱敏处理由DataMaskAspect切面负责，这里只做权限控制
 * 
 * @author wenx
 * @since 1.0.0
 */
@Slf4j
@Service
public class FieldPermissionService {
    
    /**
     * 字段权限缓存
     * key: className.fieldName, value: FieldPermissionInfo
     */
    private final Map<String, FieldPermissionInfo> permissionCache = new ConcurrentHashMap<>();
    
    /**
     * 批量验证对象的字段权限
     * 
     * @param objects 待验证的对象列表
     * @return 处理后的对象列表
     */
    public <T> List<T> processFieldPermissions(List<T> objects) {
        if (objects == null || objects.isEmpty()) {
            return objects;
        }
        
        Class<?> objectClass = objects.get(0).getClass();
        Map<String, FieldPermissionInfo> classPermissions = getClassFieldPermissions(objectClass);
        
        if (classPermissions.isEmpty()) {
            return objects; // 没有字段权限注解，直接返回
        }
        
        // 为每个对象处理字段权限
        objects.forEach(obj -> processObjectFieldPermissions(obj, classPermissions));
        
        return objects;
    }
    
    /**
     * 验证单个对象的字段权限
     * 
     * @param object 待验证的对象
     * @return 处理后的对象
     */
    public <T> T processFieldPermissions(T object) {
        if (object == null) {
            return null;
        }
        
        Class<?> objectClass = object.getClass();
        Map<String, FieldPermissionInfo> classPermissions = getClassFieldPermissions(objectClass);
        
        if (classPermissions.isEmpty()) {
            return object; // 没有字段权限注解，直接返回
        }
        
        processObjectFieldPermissions(object, classPermissions);
        return object;
    }
    
    /**
     * 获取类的字段权限信息
     */
    private Map<String, FieldPermissionInfo> getClassFieldPermissions(Class<?> clazz) {
        String className = clazz.getName();
        Map<String, FieldPermissionInfo> classPermissions = new HashMap<>();
        
        Field[] fields = getAllFields(clazz);
        for (Field field : fields) {
            FieldPermission annotation = field.getAnnotation(FieldPermission.class);
            if (annotation != null && annotation.enabled()) {
                String cacheKey = className + "." + field.getName();
                
                FieldPermissionInfo permissionInfo = permissionCache.computeIfAbsent(cacheKey, 
                    key -> new FieldPermissionInfo(field.getName(), className, annotation));
                
                classPermissions.put(field.getName(), permissionInfo);
            }
        }
        
        return classPermissions;
    }
    
    /**
     * 处理单个对象的字段权限
     */
    private void processObjectFieldPermissions(Object object, Map<String, FieldPermissionInfo> permissions) {
        permissions.forEach((fieldName, permissionInfo) -> {
            try {
                processFieldPermission(object, fieldName, permissionInfo);
            } catch (Exception e) {
                log.warn("处理字段权限失败: {}.{}", object.getClass().getSimpleName(), fieldName, e);
            }
        });
    }
    
    /**
     * 处理单个字段的权限
     */
    private void processFieldPermission(Object object, String fieldName, FieldPermissionInfo permissionInfo) 
            throws IllegalAccessException {
        
        // 验证字段权限
        FieldPermissionContextHolder.FieldPermissionResult result = 
                FieldPermissionContextHolder.verifyFieldAccess(
                    object.getClass(), fieldName, object);
        
        if (!result.isAllowed()) {
            // 移除无权限访问的字段（设置为null）
            // 脱敏处理由DataMaskAspect切面负责
            removeUnauthorizedField(object, fieldName);
            
            log.debug("字段权限控制生效: {}.{} -> 已移除", 
                object.getClass().getSimpleName(), fieldName);
        }
    }
    
    /**
     * 移除无权限访问的字段
     */
    private void removeUnauthorizedField(Object object, String fieldName) 
            throws IllegalAccessException {
        
        Field field = getField(object.getClass(), fieldName);
        if (field == null) {
            return;
        }
        
        field.setAccessible(true);
        field.set(object, null);
    }
    

    
    /**
     * 获取字段（支持继承）
     */
    private Field getField(Class<?> clazz, String fieldName) {
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
     * 获取类的所有字段（包括父类）
     */
    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            fields.addAll(Arrays.asList(declaredFields));
            currentClass = currentClass.getSuperclass();
        }
        
        return fields.toArray(new Field[0]);
    }
    
    /**
     * 清空字段权限缓存
     */
    public void clearPermissionCache() {
        permissionCache.clear();
        log.info("字段权限缓存已清空");
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return permissionCache.size();
    }
    
    /**
     * 字段权限信息
     */
    public static class FieldPermissionInfo {
        private final String fieldName;
        private final String className;
        private final FieldPermission annotation;
        
        public FieldPermissionInfo(String fieldName, String className, FieldPermission annotation) {
            this.fieldName = fieldName;
            this.className = className;
            this.annotation = annotation;
        }
        
        // Getters
        public String getFieldName() { return fieldName; }
        public String getClassName() { return className; }
        public FieldPermission getAnnotation() { return annotation; }
    }
}