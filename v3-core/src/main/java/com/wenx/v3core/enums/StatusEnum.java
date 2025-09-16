package com.wenx.v3core.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 状态枚举
 * 支持字符串和数字两种格式的JSON序列化/反序列化
 * 
 * @author wenx
 */
@Getter
@AllArgsConstructor
public enum StatusEnum implements BaseEnum<Integer> {
    
    /**
     * 禁用
     */
    DISABLED(0, "disabled", "禁用"),
    
    /**
     * 启用/激活
     */
    ACTIVE(1, "active", "启用"),
    
    /**
     * 锁定
     */
    LOCKED(2, "locked", "锁定"),
    
    /**
     * 暂停
     */
    SUSPENDED(3, "suspended", "暂停"),
    
    /**
     * 过期
     */
    EXPIRED(4, "expired", "过期");
    
    /**
     * 数值
     */
    private final Integer value;
    
    /**
     * 字符串代码
     */
    private final String code;
    
    /**
     * 描述
     */
    private final String desc;
    
    /**
     * JSON序列化时使用数值
     */
    @JsonValue
    public Integer getValue() {
        return value;
    }
    
    /**
     * JSON反序列化时的创建方法
     * 支持数字和字符串两种格式
     */
    @JsonCreator
    public static StatusEnum fromValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // 如果是数字类型
        if (value instanceof Number) {
            Integer intValue = ((Number) value).intValue();
            return getByValue(intValue);
        }
        
        // 如果是字符串类型
        if (value instanceof String) {
            String strValue = (String) value;
            
            // 尝试按数字解析
            try {
                Integer intValue = Integer.parseInt(strValue);
                return getByValue(intValue);
            } catch (NumberFormatException e) {
                // 按字符串代码查找
                return getByCode(strValue);
            }
        }
        
        throw new IllegalArgumentException("无法解析状态值: " + value);
    }
    
    /**
     * 根据数值获取枚举
     */
    public static StatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (StatusEnum item : values()) {
            if (item.getValue().equals(value)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * 根据字符串代码获取枚举
     */
    public static StatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (StatusEnum item : values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }
}