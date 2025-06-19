package com.wenx.v3redis.service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口
 * 
 * @author wenx
 * @description 提供统一的缓存操作接口
 */
public interface CacheService {
    
    /**
     * 存入缓存
     * 
     * @param key 键
     * @param value 值
     */
    void put(String key, Object value);
    
    /**
     * 存入缓存并设置过期时间
     * 
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    void put(String key, Object value, long timeout, TimeUnit unit);
    
    /**
     * 获取缓存
     * 
     * @param key 键
     * @param clazz 返回类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> clazz);
    
    /**
     * 获取缓存（字符串）
     * 
     * @param key 键
     * @return 缓存值
     */
    String get(String key);
    
    /**
     * 删除缓存
     * 
     * @param key 键
     * @return 是否删除成功
     */
    boolean delete(String key);
    
    /**
     * 批量删除缓存
     * 
     * @param pattern 键的模式
     * @return 删除的数量
     */
    long deleteByPattern(String pattern);
    
    /**
     * 判断缓存是否存在
     * 
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 设置过期时间
     * 
     * @param key 键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);
    
    /**
     * 获取过期时间
     * 
     * @param key 键
     * @return 过期时间（秒）
     */
    Long getExpire(String key);
    
    /**
     * 递增
     * 
     * @param key 键
     * @param delta 增量
     * @return 增加后的值
     */
    Long increment(String key, long delta);
    
    /**
     * 递减
     * 
     * @param key 键
     * @param delta 减量
     * @return 减少后的值
     */
    Long decrement(String key, long delta);
} 