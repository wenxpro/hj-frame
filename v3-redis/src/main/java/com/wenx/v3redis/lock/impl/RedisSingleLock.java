package com.wenx.v3redis.lock.impl;

import com.wenx.v3redis.exception.RedisLockException;
import com.wenx.v3redis.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 简单Redis锁实现
 * 
 * @author wenx
 * @description 基本的Redis锁实现，不支持可重入
 */
@Slf4j
@Component("redisSingleLock")
@RequiredArgsConstructor
public class RedisSingleLock implements RedisLock {

    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 解锁脚本
     */
    private static final String UNLOCK_SCRIPT = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";
    
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_TIMES = 3;

    @Override
    public boolean tryLock(String key, String requestId, long expire, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, requestId, expire, unit);
            boolean success = Boolean.TRUE.equals(result);
            if (success) {
                log.debug("获取锁成功，key: {}, requestId: {}", key, requestId);
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
            if (tryLock(key, requestId, leaseTime, unit)) {
                return true;
            }
            
            if (System.currentTimeMillis() - startTime >= waitMillis) {
                return false;
            }
            
            Thread.sleep(50);
        }
    }

    @Override
    public void lock(String key, String requestId, long leaseTime, TimeUnit unit) 
            throws InterruptedException {
        int retryTimes = 0;
        while (!tryLock(key, requestId, leaseTime, unit)) {
            if (++retryTimes > MAX_RETRY_TIMES * 20) { // 避免无限等待
                throw new RedisLockException("获取锁超时，key: " + key);
            }
            Thread.sleep(50);
        }
    }

    @Override
    public boolean unlock(String key, String requestId) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, 
                    Collections.singletonList(key), 
                    requestId);
            
            boolean success = result != null && result > 0;
            if (success) {
                log.debug("释放锁成功，key: {}, requestId: {}", key, requestId);
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
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("获取锁剩余时间失败，key: {}", key, e);
            return -1;
        }
    }

    @Override
    public boolean renewLock(String key, String requestId, long leaseTime, TimeUnit unit) {
        // 简单锁不支持续期，需要先检查是否持有锁，然后重新设置
        if (isLocked(key, requestId)) {
            try {
                return Boolean.TRUE.equals(
                    redisTemplate.expire(key, leaseTime, unit)
                );
            } catch (Exception e) {
                log.error("续期锁失败，key: {}, requestId: {}", key, requestId, e);
                return false;
            }
        }
        return false;
    }
}
