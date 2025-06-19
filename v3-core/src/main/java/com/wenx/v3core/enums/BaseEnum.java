package com.wenx.v3core.enums;

/**
 * 枚举基类接口
 * 
 * @author wenx
 */
public interface BaseEnum<T> {
    
    /**
     * 获取枚举值
     * 
     * @return 枚举值
     */
    T getValue();
    
    /**
     * 获取枚举描述
     * 
     * @return 枚举描述
     */
    String getDesc();
} 