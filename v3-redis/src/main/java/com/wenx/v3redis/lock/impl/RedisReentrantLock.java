package com.wenx.v3redis.lock.impl;

import com.wenx.v3redis.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis可重入锁实现
 * 支持同一线程多次获取锁
 * 
 * @author wenx
 */
@Slf4j
@Component("redisReentrantLock")
@RequiredArgsConstructor
public class RedisReentrantLock implements RedisLock {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 获取可重入锁的Lua脚本
    private static final String REENTRANT_LOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local requestId = ARGV[1]\n" +
            "local expire = ARGV[2]\n" +
            "\n" +
            "local lockValue = redis.call('hget', key, 'owner')\n" +
            "if lockValue == requestId then\n" +
            "    -- 已经拥有锁，增加重入次数\n" +
            "    local count = redis.call('hincrby', key, 'count', 1)\n" +
            "    redis.call('expire', key, expire)\n" +
            "    return count\n" +
            "elseif lockValue == false then\n" +
            "    -- 锁不存在，获取锁\n" +
            "    redis.call('hset', key, 'owner', requestId)\n" +
            "    redis.call('hset', key, 'count', 1)\n" +
            "    redis.call('expire', key, expire)\n" +
            "    return 1\n" +
            "else\n" +
            "    -- 锁被其他线程持有\n" +
            "    return 0\n" +
            "end";
    
    // 释放可重入锁的Lua脚本
    private static final String REENTRANT_UNLOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local requestId = ARGV[1]\n" +
            "\n" +
            "local lockValue = redis.call('hget', key, 'owner')\n" +
            "if lockValue == requestId then\n" +
            "    local count = redis.call('hincrby', key, 'count', -1)\n" +
            "    if count <= 0 then\n" +
            "        -- 完全释放锁\n" +
            "        redis.call('del', key)\n" +
            "        return 1\n" +
            "    else\n" +
            "        -- 减少重入次数\n" +
            "        return count\n" +
            "    end\n" +
            "else\n" +
            "    return 0\n" +
            "end";
    
    // 续期可重入锁的Lua脚本
    private static final String REENTRANT_RENEW_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local requestId = ARGV[1]\n" +
            "local expire = ARGV[2]\n" +
            "\n" +
            "local lockValue = redis.call('hget', key, 'owner')\n" +
            "if lockValue == requestId then\n" +
            "    redis.call('expire', key, expire)\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";
    
    // 检查锁状态的Lua脚本
    private static final String CHECK_LOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local requestId = ARGV[1]\n" +
            "\n" +
            "local lockValue = redis.call('hget', key, 'owner')\n" +
            "if lockValue == requestId then\n" +
            "    local count = redis.call('hget', key, 'count')\n" +
            "    return count\n" +
            "elseif lockValue == false then\n" +
            "    return 0\n" +
            "else\n" +
            "    return -1\n" +
            "end";
    
    @Override
    public void lock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        while (true) {
            Long result = redisTemplate.execute(
                    new DefaultRedisScript<>(REENTRANT_LOCK_SCRIPT, Long.class),
                    Collections.singletonList(key),
                    requestId, String.valueOf(expireSeconds)
            );
            
            if (result != null && result > 0) {
                log.debug("可重入锁获取成功，key: {}, requestId: {}, count: {}", key, requestId, result);
                return;
            }
            
            try {
                // 等待一段时间后重试
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取可重入锁被中断", e);
            }
        }
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(REENTRANT_LOCK_SCRIPT, Long.class),
                Collections.singletonList(key),
                requestId, String.valueOf(expireSeconds)
        );
        
        if (result != null && result > 0) {
            log.debug("可重入锁尝试获取成功，key: {}, requestId: {}, count: {}", key, requestId, result);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long waitTime, long expire, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        
        while (System.currentTimeMillis() < deadline) {
            if (tryLock(key, requestId, expire, unit)) {
                return true;
            }
            
            try {
                // 等待一段时间后重试
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean unlock(String key, String requestId) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(REENTRANT_UNLOCK_SCRIPT, Long.class),
                Collections.singletonList(key),
                requestId
        );
        
        if (result != null) {
            if (result == 1) {
                log.debug("可重入锁完全释放，key: {}, requestId: {}", key, requestId);
            } else if (result > 1) {
                log.debug("可重入锁部分释放，key: {}, requestId: {}, remainCount: {}", key, requestId, result);
            } else if (result == 0) {
                log.warn("可重入锁释放失败，不是锁的拥有者，key: {}, requestId: {}", key, requestId);
                return false;
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean renewLock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(REENTRANT_RENEW_SCRIPT, Long.class),
                Collections.singletonList(key),
                requestId, String.valueOf(expireSeconds)
        );
        
        boolean success = result != null && result == 1;
        if (success) {
            log.debug("可重入锁续期成功，key: {}, requestId: {}", key, requestId);
        }
        return success;
    }
    
    @Override
    public boolean isLocked(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    @Override
    public boolean isLocked(String key, String requestId) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(CHECK_LOCK_SCRIPT, Long.class),
                Collections.singletonList(key),
                requestId
        );
        
        // result > 0 表示当前线程持有锁
        return result != null && result > 0;
    }
    
    @Override
    public long getRemainTime(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        return expire != null && expire > 0 ? expire : 0;
    }
    
    @Override
    public void forceUnlock(String key) {
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.warn("强制释放可重入锁成功，key: {}", key);
        }
    }
    
    /**
     * 获取锁的重入次数
     */
    public int getHoldCount(String key, String requestId) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(CHECK_LOCK_SCRIPT, Long.class),
                Collections.singletonList(key),
                requestId
        );
        
        return result != null && result > 0 ? result.intValue() : 0;
    }
} 