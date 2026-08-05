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
     * 类级注解缓存（review P2：避免每请求对响应类型全层级反射 + 双重读注解）
     * key: 类名, value: 字段名 → 注解
     */
    private final Map<String, Map<String, FieldPermission>> classAnnotationCache = new ConcurrentHashMap<>();
    
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
        
        // review P2：按元素实际类分派（异构列表不再只处理首元素类型）
        objects.forEach(obj -> processFieldPermissions(obj));
        
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
        Map<String, FieldPermission> classPermissions = getClassFieldPermissions(objectClass);
        
        if (classPermissions.isEmpty()) {
            return object; // 没有字段权限注解，直接返回
        }
        
        classPermissions.forEach((fieldName, annotation) -> {
            try {
                FieldPermissionContextHolder.FieldPermissionResult result =
                        FieldPermissionContextHolder.verifyFieldAccess(objectClass, fieldName, object);
                if (!result.isAllowed()) {
                    removeUnauthorizedField(object, fieldName);
                }
            } catch (Exception e) {
                log.warn("处理字段权限失败: {}.{}", objectClass.getSimpleName(), fieldName, e);
            }
        });
        return object;
    }
    
    /**
     * 获取类的字段权限注解（类级缓存，避免每请求反射）
     */
    private Map<String, FieldPermission> getClassFieldPermissions(Class<?> clazz) {
        return classAnnotationCache.computeIfAbsent(clazz.getName(), key -> {
            Map<String, FieldPermission> permissions = new HashMap<>();
            for (Field field : getAllFields(clazz)) {
                FieldPermission annotation = field.getAnnotation(FieldPermission.class);
                if (annotation != null && annotation.enabled()) {
                    permissions.put(field.getName(), annotation);
                }
            }
            return permissions;
        });
    }
    
    /**
     * 移除无权限访问的字段（设置为null；脱敏由 DataMaskAspect 负责）
     */
    private void removeUnauthorizedField(Object object, String fieldName) {
        Field field = getField(object.getClass(), fieldName);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.set(object, null);
        } catch (IllegalAccessException e) {
            log.warn("字段置空失败: {}.{}", object.getClass().getSimpleName(), fieldName, e);
        }
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
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
    
    /**
     * 清空字段权限缓存
     */
    public void clearPermissionCache() {
        classAnnotationCache.clear();
        log.info("字段权限缓存已清空");
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return classAnnotationCache.size();
    }
}