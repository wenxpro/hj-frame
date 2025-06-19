package com.wenx.v3redis.lock;

import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁接口
 * 
 * @author wenx
 * @description 提供分布式锁的基本操作
 */
public interface RedisLock {

    /**
     * 尝试获取锁
     *
     * @param key 锁的键
     * @param requestId 请求ID，用于标识锁的持有者
     * @param expire 锁的过期时间
     * @param unit 时间单位
     * @return true-获取成功，false-获取失败
     */
    boolean tryLock(String key, String requestId, long expire, TimeUnit unit);
    
    /**
     * 尝试获取锁（带等待时间）
     *
     * @param key 锁的键
     * @param requestId 请求ID
     * @param waitTime 等待时间
     * @param leaseTime 锁的持有时间
     * @param unit 时间单位
     * @return true-获取成功，false-获取失败
     * @throws InterruptedException 等待过程中被中断
     */
    boolean tryLock(String key, String requestId, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;
    
    /**
     * 获取锁（一直等待直到获取成功）
     *
     * @param key 锁的键
     * @param requestId 请求ID
     * @param leaseTime 锁的持有时间
     * @param unit 时间单位
     * @throws InterruptedException 等待过程中被中断
     */
    void lock(String key, String requestId, long leaseTime, TimeUnit unit) throws InterruptedException;
    
    /**
     * 释放锁
     *
     * @param key 锁的键
     * @param requestId 请求ID
     * @return true-释放成功，false-释放失败（可能不是锁的持有者）
     */
    boolean unlock(String key, String requestId);
    
    /**
     * 强制释放锁（不检查持有者）
     *
     * @param key 锁的键
     */
    void forceUnlock(String key);
    
    /**
     * 检查是否持有锁
     *
     * @param key 锁的键
     * @param requestId 请求ID
     * @return true-持有锁，false-未持有锁
     */
    boolean isLocked(String key, String requestId);
    
    /**
     * 检查锁是否存在
     *
     * @param key 锁的键
     * @return true-锁存在，false-锁不存在
     */
    boolean isLocked(String key);
    
    /**
     * 获取锁的剩余时间
     *
     * @param key 锁的键
     * @return 剩余时间（毫秒），-1表示锁不存在，-2表示锁未设置过期时间
     */
    long getRemainTime(String key);
    
    /**
     * 续期锁
     *
     * @param key 锁的键
     * @param requestId 请求ID
     * @param leaseTime 新的持有时间
     * @param unit 时间单位
     * @return true-续期成功，false-续期失败
     */
    boolean renewLock(String key, String requestId, long leaseTime, TimeUnit unit);
}
