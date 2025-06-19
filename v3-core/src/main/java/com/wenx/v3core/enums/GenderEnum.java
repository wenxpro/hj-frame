package com.wenx.v3core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举
 * 
 * @author wenx
 */
@Getter
@AllArgsConstructor
public enum GenderEnum implements BaseEnum<Integer> {
    
    /**
     * 未知
     */
    UNKNOWN(0, "未知"),
    
    /**
     * 男
     */
    MALE(1, "男"),
    
    /**
     * 女
     */
    FEMALE(2, "女");
    
    /**
     * 值
     */
    private final Integer value;
    
    /**
     * 描述
     */
    private final String desc;
    
    /**
     * 根据值获取枚举
     */
    public static GenderEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (GenderEnum item : values()) {
            if (item.getValue().equals(value)) {
                return item;
            }
        }
        return null;
    }
} 