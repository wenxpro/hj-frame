package com.wenx.v3secure.exception;

/**
 * 未授权异常
 * 
 * @author wenx
 * @description 用户未登录或权限不足时抛出
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException() {
        super("未授权访问");
    }
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
} 