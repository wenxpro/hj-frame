package com.wenx.v3redis.lock.impl;

import com.wenx.v3redis.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Redis读写锁实现
 * 支持多个读锁共存，写锁独占
 * 
 * @author wenx
 */
@Slf4j
@Component("redisReadWriteLock")
@RequiredArgsConstructor
public class RedisReadWriteLock implements RedisLock {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 读锁计数器后缀
    private static final String READ_COUNT_SUFFIX = ":read:count";
    
    // 读锁持有者集合后缀
    private static final String READ_HOLDERS_SUFFIX = ":read:holders";
    
    // 写锁后缀
    private static final String WRITE_SUFFIX = ":write";
    
    // 锁模式后缀
    private static final String MODE_SUFFIX = ":mode";
    
    // 读锁模式
    private static final String READ_MODE = "READ";
    
    // 写锁模式
    private static final String WRITE_MODE = "WRITE";
    
    // 获取读锁脚本
    private static final String READ_LOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local readCountKey = KEYS[2]\n" +
            "local readHoldersKey = KEYS[3]\n" +
            "local writeKey = KEYS[4]\n" +
            "local modeKey = KEYS[5]\n" +
            "local requestId = ARGV[1]\n" +
            "local expire = ARGV[2]\n" +
            "\n" +
            "-- 检查是否有写锁\n" +
            "local writeOwner = redis.call('get', writeKey)\n" +
            "if writeOwner and writeOwner ~= requestId then\n" +
            "    return 0\n" +
            "end\n" +
            "\n" +
            "-- 获取或设置锁模式\n" +
            "local mode = redis.call('get', modeKey)\n" +
            "if not mode or mode == 'READ' then\n" +
            "    redis.call('set', modeKey, 'READ', 'EX', expire)\n" +
            "    \n" +
            "    -- 增加读锁计数\n" +
            "    local count = redis.call('hincrby', readCountKey, requestId, 1)\n" +
            "    redis.call('expire', readCountKey, expire)\n" +
            "    \n" +
            "    -- 添加到持有者集合\n" +
            "    redis.call('sadd', readHoldersKey, requestId)\n" +
            "    redis.call('expire', readHoldersKey, expire)\n" +
            "    \n" +
            "    -- 设置主键\n" +
            "    redis.call('set', key, 'READ_LOCK', 'EX', expire)\n" +
            "    \n" +
            "    return 1\n" +
            "end\n" +
            "\n" +
            "return 0";
    
    // 获取写锁脚本
    private static final String WRITE_LOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local readCountKey = KEYS[2]\n" +
            "local writeKey = KEYS[3]\n" +
            "local modeKey = KEYS[4]\n" +
            "local requestId = ARGV[1]\n" +
            "local expire = ARGV[2]\n" +
            "\n" +
            "-- 检查是否已经拥有写锁\n" +
            "local writeOwner = redis.call('get', writeKey)\n" +
            "if writeOwner == requestId then\n" +
            "    redis.call('expire', writeKey, expire)\n" +
            "    redis.call('expire', key, expire)\n" +
            "    redis.call('expire', modeKey, expire)\n" +
            "    return 1\n" +
            "end\n" +
            "\n" +
            "-- 检查是否有其他锁\n" +
            "local mode = redis.call('get', modeKey)\n" +
            "if mode then\n" +
            "    return 0\n" +
            "end\n" +
            "\n" +
            "-- 获取写锁\n" +
            "redis.call('set', writeKey, requestId, 'EX', expire)\n" +
            "redis.call('set', modeKey, 'WRITE', 'EX', expire)\n" +
            "redis.call('set', key, 'WRITE_LOCK', 'EX', expire)\n" +
            "\n" +
            "return 1";
    
    // 释放读锁脚本
    private static final String READ_UNLOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local readCountKey = KEYS[2]\n" +
            "local readHoldersKey = KEYS[3]\n" +
            "local modeKey = KEYS[4]\n" +
            "local requestId = ARGV[1]\n" +
            "\n" +
            "-- 减少读锁计数\n" +
            "local count = redis.call('hincrby', readCountKey, requestId, -1)\n" +
            "if count <= 0 then\n" +
            "    redis.call('hdel', readCountKey, requestId)\n" +
            "    redis.call('srem', readHoldersKey, requestId)\n" +
            "end\n" +
            "\n" +
            "-- 检查是否还有其他读锁\n" +
            "local remainingHolders = redis.call('scard', readHoldersKey)\n" +
            "if remainingHolders == 0 then\n" +
            "    redis.call('del', key)\n" +
            "    redis.call('del', readCountKey)\n" +
            "    redis.call('del', readHoldersKey)\n" +
            "    redis.call('del', modeKey)\n" +
            "end\n" +
            "\n" +
            "return 1";
    
    // 释放写锁脚本
    private static final String WRITE_UNLOCK_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local writeKey = KEYS[2]\n" +
            "local modeKey = KEYS[3]\n" +
            "local requestId = ARGV[1]\n" +
            "\n" +
            "-- 检查是否是锁的拥有者\n" +
            "if redis.call('get', writeKey) == requestId then\n" +
            "    redis.call('del', key)\n" +
            "    redis.call('del', writeKey)\n" +
            "    redis.call('del', modeKey)\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";
    
    // 当前线程的锁类型
    private final ThreadLocal<String> lockType = new ThreadLocal<>();
    
    @Override
    public void lock(String key, String requestId, long expire, TimeUnit unit) {
        // 默认获取写锁
        lockWrite(key, requestId, expire, unit);
    }
    
    public void lockRead(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        while (true) {
            Boolean result = redisTemplate.execute(
                    new DefaultRedisScript<>(READ_LOCK_SCRIPT, Boolean.class),
                    Arrays.asList(key, key + READ_COUNT_SUFFIX, key + READ_HOLDERS_SUFFIX, 
                                key + WRITE_SUFFIX, key + MODE_SUFFIX),
                    requestId, String.valueOf(expireSeconds)
            );
            
            if (Boolean.TRUE.equals(result)) {
                lockType.set(READ_MODE);
                log.debug("读锁获取成功，key: {}, requestId: {}", key, requestId);
                return;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取读锁被中断", e);
            }
        }
    }
    
    public void lockWrite(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        while (true) {
            Boolean result = redisTemplate.execute(
                    new DefaultRedisScript<>(WRITE_LOCK_SCRIPT, Boolean.class),
                    Arrays.asList(key, key + READ_COUNT_SUFFIX, key + WRITE_SUFFIX, key + MODE_SUFFIX),
                    requestId, String.valueOf(expireSeconds)
            );
            
            if (Boolean.TRUE.equals(result)) {
                lockType.set(WRITE_MODE);
                log.debug("写锁获取成功，key: {}, requestId: {}", key, requestId);
                return;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取写锁被中断", e);
            }
        }
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long expire, TimeUnit unit) {
        // 默认尝试获取写锁
        return tryLockWrite(key, requestId, expire, unit);
    }
    
    public boolean tryLockRead(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        Boolean result = redisTemplate.execute(
                new DefaultRedisScript<>(READ_LOCK_SCRIPT, Boolean.class),
                Arrays.asList(key, key + READ_COUNT_SUFFIX, key + READ_HOLDERS_SUFFIX, 
                            key + WRITE_SUFFIX, key + MODE_SUFFIX),
                requestId, String.valueOf(expireSeconds)
        );
        
        if (Boolean.TRUE.equals(result)) {
            lockType.set(READ_MODE);
            log.debug("读锁尝试获取成功，key: {}, requestId: {}", key, requestId);
            return true;
        }
        return false;
    }
    
    public boolean tryLockWrite(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        Boolean result = redisTemplate.execute(
                new DefaultRedisScript<>(WRITE_LOCK_SCRIPT, Boolean.class),
                Arrays.asList(key, key + READ_COUNT_SUFFIX, key + WRITE_SUFFIX, key + MODE_SUFFIX),
                requestId, String.valueOf(expireSeconds)
        );
        
        if (Boolean.TRUE.equals(result)) {
            lockType.set(WRITE_MODE);
            log.debug("写锁尝试获取成功，key: {}, requestId: {}", key, requestId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long waitTime, long expire, TimeUnit unit) {
        // 默认尝试获取写锁
        return tryLockWrite(key, requestId, waitTime, expire, unit);
    }
    
    public boolean tryLockRead(String key, String requestId, long waitTime, long expire, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        
        while (System.currentTimeMillis() < deadline) {
            if (tryLockRead(key, requestId, expire, unit)) {
                return true;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    public boolean tryLockWrite(String key, String requestId, long waitTime, long expire, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        
        while (System.currentTimeMillis() < deadline) {
            if (tryLockWrite(key, requestId, expire, unit)) {
                return true;
            }
            
            try {
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
        String type = lockType.get();
        lockType.remove();
        
        if (READ_MODE.equals(type)) {
            return unlockRead(key, requestId);
        } else {
            return unlockWrite(key, requestId);
        }
    }
    
    private boolean unlockRead(String key, String requestId) {
        Boolean result = redisTemplate.execute(
                new DefaultRedisScript<>(READ_UNLOCK_SCRIPT, Boolean.class),
                Arrays.asList(key, key + READ_COUNT_SUFFIX, key + READ_HOLDERS_SUFFIX, key + MODE_SUFFIX),
                requestId
        );
        
        boolean success = Boolean.TRUE.equals(result);
        if (success) {
            log.debug("读锁释放成功，key: {}, requestId: {}", key, requestId);
        }
        return success;
    }
    
    private boolean unlockWrite(String key, String requestId) {
        Boolean result = redisTemplate.execute(
                new DefaultRedisScript<>(WRITE_UNLOCK_SCRIPT, Boolean.class),
                Arrays.asList(key, key + WRITE_SUFFIX, key + MODE_SUFFIX),
                requestId
        );
        
        boolean success = Boolean.TRUE.equals(result);
        if (success) {
            log.debug("写锁释放成功，key: {}, requestId: {}", key, requestId);
        } else {
            log.warn("写锁释放失败，key: {}, requestId: {}", key, requestId);
        }
        return success;
    }
    
    @Override
    public boolean renewLock(String key, String requestId, long expire, TimeUnit unit) {
        // 根据当前锁类型续期
        String type = lockType.get();
        if (READ_MODE.equals(type)) {
            return renewReadLock(key, requestId, expire, unit);
        } else {
            return renewWriteLock(key, requestId, expire, unit);
        }
    }
    
    private boolean renewReadLock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        // 续期所有相关的key
        redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(key + READ_COUNT_SUFFIX, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(key + READ_HOLDERS_SUFFIX, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(key + MODE_SUFFIX, expireSeconds, TimeUnit.SECONDS);
        
        log.debug("读锁续期成功，key: {}, requestId: {}", key, requestId);
        return true;
    }
    
    private boolean renewWriteLock(String key, String requestId, long expire, TimeUnit unit) {
        long expireSeconds = unit.toSeconds(expire);
        
        String writeOwner = (String) redisTemplate.opsForValue().get(key + WRITE_SUFFIX);
        if (requestId.equals(writeOwner)) {
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            redisTemplate.expire(key + WRITE_SUFFIX, expireSeconds, TimeUnit.SECONDS);
            redisTemplate.expire(key + MODE_SUFFIX, expireSeconds, TimeUnit.SECONDS);
            
            log.debug("写锁续期成功，key: {}, requestId: {}", key, requestId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isLocked(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    @Override
    public boolean isLocked(String key, String requestId) {
        // 检查是否持有写锁
        String writeOwner = (String) redisTemplate.opsForValue().get(key + WRITE_SUFFIX);
        if (requestId.equals(writeOwner)) {
            return true;
        }
        
        // 检查是否持有读锁
        Boolean hasReadLock = redisTemplate.opsForSet().isMember(key + READ_HOLDERS_SUFFIX, requestId);
        return Boolean.TRUE.equals(hasReadLock);
    }
    
    @Override
    public long getRemainTime(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        return expire != null && expire > 0 ? expire : 0;
    }
    
    @Override
    public void forceUnlock(String key) {
        // 删除所有相关的key
        redisTemplate.delete(Arrays.asList(
                key,
                key + READ_COUNT_SUFFIX,
                key + READ_HOLDERS_SUFFIX,
                key + WRITE_SUFFIX,
                key + MODE_SUFFIX
        ));
        
        log.warn("强制释放读写锁，key: {}", key);
    }
} 