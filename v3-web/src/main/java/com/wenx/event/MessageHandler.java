package com.wenx.event;

/**
 * 统一消息处理器接口 实际的逻辑处理
 * @auther wenx
 */
public interface MessageHandler<T extends Message, R> {
    
    /**
     * 处理消息
     */
    R handle(T message);
    
    /**
     * 获取支持的消息类型
     */
    Class<T> getMessageType();
    
    /**
     * 获取处理器名称
     */
    default String getHandlerName() {
        return this.getClass().getSimpleName();
    }
} 