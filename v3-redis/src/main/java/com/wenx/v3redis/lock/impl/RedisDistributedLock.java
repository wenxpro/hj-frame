package com.wenx.v3redis.lock.impl;

import com.wenx.v3redis.exception.RedisLockException;
import com.wenx.v3redis.lock.RedisLock;
import com.wenx.v3redis.util.LuaScriptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁实现
 * 基于SET命令的原子性实现分布式锁
 * 
 * @author wenx
 * @description 基于Redis的分布式锁实现，支持可重入、自动续期等特性
 */
@Slf4j
@Component("redisDistributedLock")
@RequiredArgsConstructor
public class RedisDistributedLock implements RedisLock {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final LuaScriptLoader luaScriptLoader;
    
    @Override
    public boolean tryLock(String key, String requestId, long expire, TimeUnit unit) {
        try {
            long expireSeconds = unit.toSeconds(expire);
            
            DefaultRedisScript<Boolean> script = luaScriptLoader.getRedisScript("distributed_lock", Boolean.class);
            Boolean result = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    requestId, String.valueOf(expireSeconds)
            );
            
            boolean success = Boolean.TRUE.equals(result);
            if (success) {
                log.debug("成功获取锁，key: {}, requestId: {}, expire: {} {}", key, requestId, expire, unit);
            }
            return success;
        } catch (Exception e) {
            log.error("获取锁失败，key: {}, requestId: {}", key, requestId, e);
            throw new RedisLockException("获取锁失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long waitTime, long leaseTime, TimeUnit unit) 
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long waitMillis = unit.toMillis(waitTime);
        
        while (true) {
            // 尝试获取锁
            if (tryLock(key, requestId, leaseTime, unit)) {
                return true;
            }
            
            // 检查是否超时
            if (System.currentTimeMillis() - startTime >= waitMillis) {
                return false;
            }
            
            // 等待一段时间后重试
            Thread.sleep(Math.min(waitMillis / 10, 100));
        }
    }
    
    @Override
    public void lock(String key, String requestId, long leaseTime, TimeUnit unit) 
            throws InterruptedException {
        while (!tryLock(key, requestId, leaseTime, unit)) {
            // 等待一段时间后重试
            Thread.sleep(100);
        }
    }
    
    @Override
    public boolean unlock(String key, String requestId) {
        try {
            DefaultRedisScript<Boolean> script = luaScriptLoader.getRedisScript("distributed_unlock", Boolean.class);
            Boolean result = redisTemplate.execute(
                    script, 
                    Collections.singletonList(key), 
                    requestId
            );
            
            boolean success = Boolean.TRUE.equals(result);
            if (success) {
                log.debug("成功释放锁，key: {}, requestId: {}", key, requestId);
            } else {
                log.warn("释放锁失败，可能不是锁的持有者，key: {}, requestId: {}", key, requestId);
            }
            return success;
        } catch (Exception e) {
            log.error("释放锁异常，key: {}, requestId: {}", key, requestId, e);
            throw new RedisLockException("释放锁失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void forceUnlock(String key) {
        try {
            redisTemplate.delete(key);
            log.warn("强制释放锁，key: {}", key);
        } catch (Exception e) {
            log.error("强制释放锁失败，key: {}", key, e);
            throw new RedisLockException("强制释放锁失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isLocked(String key, String requestId) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return requestId.equals(value);
        } catch (Exception e) {
            log.error("检查锁状态失败，key: {}, requestId: {}", key, requestId, e);
            return false;
        }
    }
    
    @Override
    public boolean isLocked(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查锁是否存在失败，key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public long getRemainTime(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            return expire != null && expire > 0 ? expire : -1;
        } catch (Exception e) {
            log.error("获取锁剩余时间失败，key: {}", key, e);
            return -1;
        }
    }
    
    @Override
    public boolean renewLock(String key, String requestId, long leaseTime, TimeUnit unit) {
        try {
            long expireSeconds = unit.toSeconds(leaseTime);
            
            DefaultRedisScript<Boolean> script = luaScriptLoader.getRedisScript("distributed_renew", Boolean.class);
            Boolean result = redisTemplate.execute(
                    script, 
                    Collections.singletonList(key), 
                    requestId, 
                    String.valueOf(expireSeconds)
            );
            
            boolean success = Boolean.TRUE.equals(result);
            if (success) {
                log.debug("成功续期锁，key: {}, requestId: {}, leaseTime: {} {}", 
                        key, requestId, leaseTime, unit);
            } else {
                log.warn("续期锁失败，可能不是锁的持有者，key: {}, requestId: {}", key, requestId);
            }
            return success;
        } catch (Exception e) {
            log.error("续期锁异常，key: {}, requestId: {}", key, requestId, e);
            throw new RedisLockException("续期锁失败: " + e.getMessage(), e);
        }
    }
} 