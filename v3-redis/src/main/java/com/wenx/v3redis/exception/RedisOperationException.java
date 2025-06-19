package com.wenx.v3redis.exception;

/**
 * Redis操作异常
 * 
 * @author wenx
 * @description Redis操作过程中的异常
 */
public class RedisOperationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public RedisOperationException() {
        super();
    }
    
    public RedisOperationException(String message) {
        super(message);
    }
    
    public RedisOperationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RedisOperationException(Throwable cause) {
        super(cause);
    }
} 