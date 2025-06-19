package com.wenx.v3redis.lock.impl;

import com.wenx.v3redis.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis多重锁实现
 * 支持同时获取多个锁，所有锁都获取成功才算成功
 * 
 * @author wenx
 */
@Slf4j
@Component("redisMultiLock")
@RequiredArgsConstructor
public class RedisMultiLock implements RedisLock {
    
    @Autowired
    @Qualifier("redisDistributedLock")
    private RedisLock defaultLock;
    
    // 存储当前线程获取的锁
    private final ThreadLocal<Map<String, List<String>>> acquiredLocks = ThreadLocal.withInitial(HashMap::new);
    
    /**
     * 获取多个锁
     * @param keys 锁的key列表
     * @param requestId 请求ID
     * @param expire 过期时间
     * @param unit 时间单位
     */
    public void lockMulti(List<String> keys, String requestId, long expire, TimeUnit unit) {
        // 对keys进行排序，避免死锁
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        
        List<String> lockedKeys = new ArrayList<>();
        Map<String, List<String>> threadLocks = acquiredLocks.get();
        
        try {
            for (String key : sortedKeys) {
                defaultLock.lock(key, requestId, expire, unit);
                lockedKeys.add(key);
                log.debug("多重锁获取成功，key: {}, requestId: {}", key, requestId);
            }
            
            // 记录当前线程获取的锁
            threadLocks.put(requestId, new ArrayList<>(lockedKeys));
            
        } catch (Exception e) {
            // 获取失败，释放已经获取的锁
            for (String key : lockedKeys) {
                try {
                    defaultLock.unlock(key, requestId);
                } catch (Exception ex) {
                    log.error("释放锁失败，key: {}, requestId: {}", key, requestId, ex);
                }
            }
            throw new RuntimeException("获取多重锁失败", e);
        }
    }
    
    /**
     * 尝试获取多个锁
     */
    public boolean tryLockMulti(List<String> keys, String requestId, long expire, TimeUnit unit) {
        // 对keys进行排序，避免死锁
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        
        List<String> lockedKeys = new ArrayList<>();
        Map<String, List<String>> threadLocks = acquiredLocks.get();
        
        try {
            for (String key : sortedKeys) {
                if (!defaultLock.tryLock(key, requestId, expire, unit)) {
                    // 获取失败，释放已经获取的锁
                    for (String lockedKey : lockedKeys) {
                        try {
                            defaultLock.unlock(lockedKey, requestId);
                        } catch (Exception e) {
                            log.error("释放锁失败，key: {}, requestId: {}", lockedKey, requestId, e);
                        }
                    }
                    return false;
                }
                lockedKeys.add(key);
                log.debug("多重锁尝试获取成功，key: {}, requestId: {}", key, requestId);
            }
            
            // 记录当前线程获取的锁
            threadLocks.put(requestId, new ArrayList<>(lockedKeys));
            return true;
            
        } catch (Exception e) {
            // 获取失败，释放已经获取的锁
            for (String key : lockedKeys) {
                try {
                    defaultLock.unlock(key, requestId);
                } catch (Exception ex) {
                    log.error("释放锁失败，key: {}, requestId: {}", key, requestId, ex);
                }
            }
            return false;
        }
    }
    
    /**
     * 释放多个锁
     */
    public boolean unlockMulti(String requestId) {
        Map<String, List<String>> threadLocks = acquiredLocks.get();
        List<String> keys = threadLocks.get(requestId);
        
        if (keys == null || keys.isEmpty()) {
            return true;
        }
        
        boolean allSuccess = true;
        for (String key : keys) {
            try {
                if (!defaultLock.unlock(key, requestId)) {
                    allSuccess = false;
                }
                log.debug("多重锁释放成功，key: {}, requestId: {}", key, requestId);
            } catch (Exception e) {
                log.error("释放锁异常，key: {}, requestId: {}", key, requestId, e);
                allSuccess = false;
            }
        }
        
        threadLocks.remove(requestId);
        if (threadLocks.isEmpty()) {
            acquiredLocks.remove();
        }
        
        return allSuccess;
    }
    
    @Override
    public void lock(String key, String requestId, long expire, TimeUnit unit) {
        // 单个锁的情况
        lockMulti(Collections.singletonList(key), requestId, expire, unit);
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long expire, TimeUnit unit) {
        // 单个锁的情况
        return tryLockMulti(Collections.singletonList(key), requestId, expire, unit);
    }
    
    @Override
    public boolean tryLock(String key, String requestId, long waitTime, long expire, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        
        while (System.currentTimeMillis() < deadline) {
            if (tryLock(key, requestId, expire, unit)) {
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
        // 单个锁的情况，但需要检查是否是多重锁的一部分
        Map<String, List<String>> threadLocks = acquiredLocks.get();
        List<String> keys = threadLocks.get(requestId);
        
        if (keys != null && keys.contains(key)) {
            // 是多重锁的一部分，释放所有锁
            return unlockMulti(requestId);
        } else {
            // 不是多重锁，直接释放
            return defaultLock.unlock(key, requestId);
        }
    }
    
    @Override
    public boolean renewLock(String key, String requestId, long expire, TimeUnit unit) {
        Map<String, List<String>> threadLocks = acquiredLocks.get();
        List<String> keys = threadLocks.get(requestId);
        
        if (keys != null && !keys.isEmpty()) {
            // 续期所有锁
            boolean allSuccess = true;
            for (String k : keys) {
                if (!defaultLock.renewLock(k, requestId, expire, unit)) {
                    allSuccess = false;
                }
            }
            return allSuccess;
        } else {
            // 单个锁续期
            return defaultLock.renewLock(key, requestId, expire, unit);
        }
    }
    
    @Override
    public boolean isLocked(String key) {
        return defaultLock.isLocked(key);
    }
    
    @Override
    public boolean isLocked(String key, String requestId) {
        return defaultLock.isLocked(key, requestId);
    }
    
    @Override
    public long getRemainTime(String key) {
        return defaultLock.getRemainTime(key);
    }
    
    @Override
    public void forceUnlock(String key) {
        // 强制释放可能需要清理多重锁记录
        Map<String, List<String>> threadLocks = acquiredLocks.get();
        
        // 查找包含该key的所有requestId
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : threadLocks.entrySet()) {
            if (entry.getValue().contains(key)) {
                entry.getValue().remove(key);
                if (entry.getValue().isEmpty()) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        
        // 移除空的记录
        for (String requestId : toRemove) {
            threadLocks.remove(requestId);
        }
        
        if (threadLocks.isEmpty()) {
            acquiredLocks.remove();
        }
        
        // 强制释放锁
        defaultLock.forceUnlock(key);
    }
    
    /**
     * 强制释放多个锁
     */
    public void forceUnlockMulti(List<String> keys) {
        for (String key : keys) {
            forceUnlock(key);
        }
    }
} 