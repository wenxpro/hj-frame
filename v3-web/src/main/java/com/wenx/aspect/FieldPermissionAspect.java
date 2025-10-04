package com.wenx.aspect;

import com.wenx.permission.service.FieldPermissionService;
import com.wenx.v3core.response.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 字段级权限切面
 * 在Controller层方法执行后，对返回结果进行字段权限控制
 * 与DataMaskAspect配合工作，确保字段权限控制在数据脱敏之前执行
 * 
 * @author wenx
 * @since 1.0.0
 */
@Aspect
@Component
@Order(90) // 在DataMaskAspect(Order=100)之前执行
@Slf4j
@RequiredArgsConstructor
public class FieldPermissionAspect {
    
    private final FieldPermissionService fieldPermissionService;
    
    /**
     * 拦截Controller层的所有公共方法
     * 对返回结果进行字段权限处理
     */
    @Around("execution(public * com.wenx..*Controller.*(..))")
    public Object processFieldPermissions(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行原方法
        Object result = joinPoint.proceed();
        
        try {
            // 处理返回结果的字段权限
            return processReturnValue(result);
        } catch (Exception e) {
            log.warn("字段权限处理失败: {}.{}", 
                joinPoint.getTarget().getClass().getSimpleName(), 
                joinPoint.getSignature().getName(), e);
            return result; // 出错时返回原始结果
        }
    }
    
    /**
     * 处理返回值的字段权限
     */
    @SuppressWarnings("unchecked")
    private Object processReturnValue(Object result) {
        if (result == null) {
            return null;
        }
        
        // 处理R包装类型
        if (result instanceof R) {
            R response = (R) result;
            Object data = response.getData();
            
            if (data != null) {
                Object processedData = processDataObject(data);
                response.setData(processedData);
            }
            
            return response;
        }
        
        // 直接处理数据对象
        return processDataObject(result);
    }
    
    /**
     * 处理数据对象
     */
    @SuppressWarnings("unchecked")
    private Object processDataObject(Object data) {
        if (data == null) {
            return null;
        }
        
        // 处理集合类型
        if (data instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) data;
            if (!collection.isEmpty()) {
                // 转换为List进行处理
                List<Object> list = collection instanceof List ? 
                    (List<Object>) collection : 
                    List.copyOf(collection);
                    
                return fieldPermissionService.processFieldPermissions(list);
            }
            return data;
        }
        
        // 处理数组类型
        if (data.getClass().isArray()) {
            Object[] array = (Object[]) data;
            List<Object> list = List.of(array);
            return fieldPermissionService.processFieldPermissions(list).toArray();
        }
        
        // 处理单个对象
        if (!isPrimitiveOrWrapper(data.getClass())) {
            return fieldPermissionService.processFieldPermissions(data);
        }
        
        return data;
    }
    
    /**
     * 判断是否为基础类型或包装类型
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               clazz == Character.class ||
               Number.class.isAssignableFrom(clazz);
    }
}