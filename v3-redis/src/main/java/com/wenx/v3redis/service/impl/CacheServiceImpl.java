package com.wenx.v3redis.service.impl;

import com.wenx.v3redis.exception.RedisOperationException;
import com.wenx.v3redis.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务实现
 * 
 * @author wenx
 * @description 基于Redis的缓存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("缓存存入成功，key: {}", key);
        } catch (Exception e) {
            log.error("缓存存入失败，key: {}", key, e);
            throw new RedisOperationException("缓存存入失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void put(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("缓存存入成功，key: {}, 过期时间: {} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("缓存存入失败，key: {}", key, e);
            throw new RedisOperationException("缓存存入失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            
            // 如果值已经是目标类型，直接返回
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            
            // 否则尝试转换
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            log.error("缓存获取失败，key: {}", key, e);
            throw new RedisOperationException("缓存获取失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String get(String key) {
        return get(key, String.class);
    }
    
    @Override
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("缓存删除{}，key: {}", result ? "成功" : "失败", key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("缓存删除失败，key: {}", key, e);
            throw new RedisOperationException("缓存删除失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            Long count = redisTemplate.delete(keys);
            log.debug("批量删除缓存成功，pattern: {}, 删除数量: {}", pattern, count);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("批量删除缓存失败，pattern: {}", pattern, e);
            throw new RedisOperationException("批量删除缓存失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查缓存存在性失败，key: {}", key, e);
            throw new RedisOperationException("检查缓存存在性失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            log.debug("设置过期时间{}，key: {}, 过期时间: {} {}", 
                    result ? "成功" : "失败", key, timeout, unit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置过期时间失败，key: {}", key, e);
            throw new RedisOperationException("设置过期时间失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.error("获取过期时间失败，key: {}", key, e);
            throw new RedisOperationException("获取过期时间失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            log.debug("递增操作成功，key: {}, 增量: {}, 结果: {}", key, delta, result);
            return result;
        } catch (Exception e) {
            log.error("递增操作失败，key: {}, 增量: {}", key, delta, e);
            throw new RedisOperationException("递增操作失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Long decrement(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, -delta);
            log.debug("递减操作成功，key: {}, 减量: {}, 结果: {}", key, delta, result);
            return result;
        } catch (Exception e) {
            log.error("递减操作失败，key: {}, 减量: {}", key, delta, e);
            throw new RedisOperationException("递减操作失败: " + e.getMessage(), e);
        }
    }
} 