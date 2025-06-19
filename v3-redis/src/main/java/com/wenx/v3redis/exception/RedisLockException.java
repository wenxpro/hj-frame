package com.wenx.v3redis.exception;

/**
 * Redis锁异常
 * 
 * @author wenx
 * @description Redis锁操作相关异常
 */
public class RedisLockException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public RedisLockException() {
        super();
    }
    
    public RedisLockException(String message) {
        super(message);
    }
    
    public RedisLockException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RedisLockException(Throwable cause) {
        super(cause);
    }
} 