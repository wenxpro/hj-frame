package com.wenx.v3redis.lock.impl;

import com.wenx.v3redis.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis公平锁实现
 * 基于队列实现，保证先到先得的公平性
 * 
 * @author wenx
 */
@Slf4j
@Component("redisFairLock")
@RequiredArgsConstructor
public class RedisFairLock implements RedisLock {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 等待队列后缀
    private static final String QUEUE_SUFFIX = ":queue";
    
    // 锁拥有者后缀
    private static final String OWNER_SUFFIX = ":owner";
    
    // 公平锁获取脚本
    private static final String FAIR_LOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local queueKey = KEYS[2]\n" +
            "local ownerKey = KEYS[3]\n" +
            "local requestId = ARGV[1]\n" +
            "local expire = ARGV[2]\n" +
            "\n" +
            "-- 检查是否已经拥有锁\n" +
            "local owner = redis.call('get', ownerKey)\n" +
            "if owner == requestId then\n" +
            "    redis.call('expire', key, expire)\n" +
            "    redis.call('expire', ownerKey, expire)\n" +
            "    return 1\n" +
            "end\n" +
            "\n" +
            "-- 将请求ID加入队列\n" +
            "local inQueue = redis.call('lpos', queueKey, requestId)\n" +
            "if not inQueue then\n" +
            "    redis.call('rpush', queueKey, requestId)\n" +
            "end\n" +
            "\n" +
            "-- 检查是否可以获取锁\n" +
            "if redis.call('exists', key) == 0 then\n" +
            "    -- 锁不存在，从队列头部取出请求ID\n" +
            "    local firstInQueue = redis.call('lindex', queueKey, 0)\n" +
            "    if firstInQueue == requestId then\n" +
            "        -- 当前请求在队列头部，可以获取锁\n" +
            "        redis.call('lpop', queueKey)\n" +
            "        redis.call('set', key, requestId, 'EX', expire)\n" +
            "        redis.call('set', ownerKey, requestId, 'EX', expire)\n" +
            "        return 1\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "return 0";
    
    // 公平锁释放脚本
    private static final String FAIR_UNLOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local queueKey = KEYS[2]\n" +
            "local ownerKey = KEYS[3]\n" +
            "local requestId = ARGV[1]\n" +
            "\n" +
            "-- 检查是否是锁的拥有者\n" +
            "if redis.call('get', key) == requestId then\n" +
            "    redis.call('del', key)\n" +
            "    redis.call('del', ownerKey)\n" +
            "    \n" +
            "    -- 通知队列中的下一个请求\n" +
            "    local nextInQueue = redis.call('lindex', queueKey, 0)\n" +
            "    if nextInQueue then\n" +
            "        -- 发布通知事件\n" +
            "        redis.call('publish', key .. ':notify', nextInQueue)\n" +
            "    end\n" +
            "    \n" +
            "    return 1\n" +
            "else\n" +
            "    -- 从队列中移除（可能在等待中）\n" +
            "    redis.call('lrem', queueKey, 0, requestId)\n" +
            "    return 0\n" +
            "end";
    
    // 公平锁续期脚本
    private static final String FAIR_RENEW_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local ownerKey = KEYS[2]\n" +
            "local requestId = ARGV[1]\n" +
            "local expire = ARGV[2]\n" +
            "\n" +
            "if redis.call('get', key) == requestId then\n" +
            "    redis.call('expire', key, expire)\n" +
            "    redis.call('expire', ownerKey, expire)\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";
    
    @Override
    public void lock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        String queueKey = key + QUEUE_SUFFIX;
        String ownerKey = key + OWNER_SUFFIX;
        
        while (true) {
            Boolean result = redisTemplate.execute(
                    new DefaultRedisScript<>(FAIR_LOCK_SCRIPT, Boolean.class),
                    Arrays.asList(key, queueKey, ownerKey),
                    requestId, String.valueOf(expireSeconds)
            );
            
            if (Boolean.TRUE.equals(result)) {
                log.debug("公平锁获取成功，key: {}, requestId: {}", key, requestId);
                return;
            }
            
            try {
                // 等待一段时间后重试
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取公平锁被中断", e);
            }
        }
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        String queueKey = key + QUEUE_SUFFIX;
        String ownerKey = key + OWNER_SUFFIX;
        
        Boolean result = redisTemplate.execute(
                new DefaultRedisScript<>(FAIR_LOCK_SCRIPT, Boolean.class),
                Arrays.asList(key, queueKey, ownerKey),
                requestId, String.valueOf(expireSeconds)
        );
        
        boolean success = Boolean.TRUE.equals(result);
        if (success) {
            log.debug("公平锁尝试获取成功，key: {}, requestId: {}", key, requestId);
        }
        return success;
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
        String queueKey = key + QUEUE_SUFFIX;
        String ownerKey = key + OWNER_SUFFIX;
        
        Boolean result = redisTemplate.execute(
                new DefaultRedisScript<>(FAIR_UNLOCK_SCRIPT, Boolean.class),
                Arrays.asList(key, queueKey, ownerKey),
                requestId
        );
        
        boolean success = Boolean.TRUE.equals(result);
        if (success) {
            log.debug("公平锁释放成功，key: {}, requestId: {}", key, requestId);
        } else {
            log.warn("公平锁释放失败，key: {}, requestId: {}", key, requestId);
        }
        return success;
    }
    
    @Override
    public boolean renewLock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        String ownerKey = key + OWNER_SUFFIX;
        
        Boolean result = redisTemplate.execute(
                new DefaultRedisScript<>(FAIR_RENEW_SCRIPT, Boolean.class),
                Arrays.asList(key, ownerKey),
                requestId, String.valueOf(expireSeconds)
        );
        
        boolean success = Boolean.TRUE.equals(result);
        if (success) {
            log.debug("公平锁续期成功，key: {}, requestId: {}", key, requestId);
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
        String value = (String) redisTemplate.opsForValue().get(key);
        return requestId.equals(value);
    }
    
    @Override
    public long getRemainTime(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        return expire != null && expire > 0 ? expire : 0;
    }
    
    @Override
    public void forceUnlock(String key) {
        String queueKey = key + QUEUE_SUFFIX;
        String ownerKey = key + OWNER_SUFFIX;
        
        // 删除所有相关的key
        redisTemplate.delete(Arrays.asList(key, queueKey, ownerKey));
        
        log.warn("强制释放公平锁，key: {}", key);
    }
} 