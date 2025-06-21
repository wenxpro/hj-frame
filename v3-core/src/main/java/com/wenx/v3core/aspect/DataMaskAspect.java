package com.wenx.v3core.aspect;

import com.wenx.v3core.anoo.DataMask;
import com.wenx.v3core.anoo.DataMaskProcessor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * 数据脱敏切面
 * 
 * <p>与其他切面的执行顺序：</p>
 * <ul>
 *   <li>Order(100)：在ControllerAspect之后执行</li>
 *   <li>确保日志记录完成后再进行数据脱敏</li>
 * </ul>
 * 
 * @author wenx
 * @version 1.0
 */
@Aspect
@Component
@Slf4j
@Order(100)
public class DataMaskAspect {

    /**
     * 定义Controller层方法切点
     * 只拦截业务包下的Controller类的公共方法
     */
    @Pointcut("execution(public * com.wenx..*Controller.*(..))")
    public void controllerPublicMethod() {
    }

    /**
     * 环绕通知：对Controller方法返回结果进行数据脱敏
     */
    @Around("controllerPublicMethod()")
    public Object maskSensitiveData(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行原方法
        Object result = joinPoint.proceed();
        
        if (result == null) {
            return null;
        }
        
        try {
            // 记录脱敏开始（仅在debug级别）
            if (log.isDebugEnabled()) {
                String methodSignature = buildMethodSignature(joinPoint);
                log.debug("[MASK] 开始数据脱敏: {}", methodSignature);
            }
            
            // 执行数据脱敏
            Object maskedResult = processMaskData(result);
            
            if (log.isDebugEnabled()) {
                log.debug("[MASK] 数据脱敏完成");
            }
            
            return maskedResult;
            
        } catch (Exception e) {
            log.warn("[MASK] 数据脱敏处理失败: {}, 返回原始数据", e.getMessage());
            return result;
        }
    }

    /**
     * 处理数据脱敏的核心逻辑
     * 
     * @param obj 需要处理的对象
     * @return 脱敏后的对象
     */
    private Object processMaskData(Object obj) {
        if (obj == null) {
            return null;
        }

        // 处理基本类型，直接返回
        if (isBasicType(obj.getClass())) {
            return obj;
        }

        // 处理集合类型
        if (obj instanceof Collection<?> collection) {
            collection.forEach(this::processMaskData);
            return obj;
        }

        // 处理Map类型
        if (obj instanceof Map<?, ?> map) {
            map.values().forEach(this::processMaskData);
            return obj;
        }

        // 处理数组类型
        if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            for (Object item : array) {
                processMaskData(item);
            }
            return obj;
        }

        // 处理普通对象
        return maskObjectFields(obj);
    }

    /**
     * 对对象的字段进行脱敏处理
     * 
     * @param obj 对象
     * @return 处理后的对象
     */
    private Object maskObjectFields(Object obj) {
        if (obj == null || isBasicType(obj.getClass())) {
            return obj;
        }

        try {
            Class<?> clazz = obj.getClass();
            Field[] fields = getAllFields(clazz);
            
            for (Field field : fields) {
                field.setAccessible(true);
                
                DataMask dataMask = field.getAnnotation(DataMask.class);
                if (dataMask != null && dataMask.enabled()) {
                    // 处理标记了@DataMask的字段
                    Object fieldValue = field.get(obj);
                    if (fieldValue instanceof String strValue) {
                        String maskedValue = DataMaskProcessor.maskData(strValue, dataMask);
                        field.set(obj, maskedValue);
                        
                        if (log.isDebugEnabled()) {
                            log.debug("[MASK] 字段脱敏: {}.{} = {} -> {}", 
                                clazz.getSimpleName(), field.getName(), 
                                strValue.length() > 10 ? strValue.substring(0, 10) + "..." : strValue,
                                maskedValue);
                        }
                    }
                } else {
                    // 递归处理嵌套对象
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null && !isBasicType(fieldValue.getClass())) {
                        processMaskData(fieldValue);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[MASK] 字段脱敏处理失败: {}", e.getMessage());
        }

        return obj;
    }

    /**
     * 获取类的所有字段（包括父类字段）
     * 
     * @param clazz 类
     * @return 所有字段数组
     */
    private Field[] getAllFields(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return new Field[0];
        }
        
        Field[] declaredFields = clazz.getDeclaredFields();
        Field[] parentFields = getAllFields(clazz.getSuperclass());
        
        Field[] allFields = new Field[declaredFields.length + parentFields.length];
        System.arraycopy(declaredFields, 0, allFields, 0, declaredFields.length);
        System.arraycopy(parentFields, 0, allFields, declaredFields.length, parentFields.length);
        
        return allFields;
    }

    /**
     * 判断是否为基本类型或常用不可变类型
     * 
     * @param clazz 类型
     * @return 是否为基本类型
     */
    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Character.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               Number.class.isAssignableFrom(clazz) ||
               clazz.isEnum() ||
               (clazz.getPackage() != null && 
                (clazz.getPackage().getName().startsWith("java.lang") ||
                 clazz.getPackage().getName().startsWith("java.time") ||
                 clazz.getPackage().getName().startsWith("java.math")));
    }

    /**
     * 构建方法签名字符串
     * 
     * @param joinPoint 连接点
     * @return 方法签名
     */
    private String buildMethodSignature(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        // 简化类名显示
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        return simpleClassName + "." + methodName;
    }
} 