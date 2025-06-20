package com.wenx.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一消息基类
 * 支持命令和查询
 */
public abstract class Message {
    
    private final String messageId;
    private final LocalDateTime timestamp;
    private final String messageType;
    
    protected Message(String messageType) {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.messageType = messageType;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * 判断是否为命令
     */
    public boolean isCommand() {
        return messageType.endsWith("_COMMAND") || this instanceof Command;
    }
    
    /**
     * 判断是否为查询
     */
    public boolean isQuery() {
        return messageType.endsWith("_QUERY") || this instanceof Query;
    }
    
    /**
     * 命令标记接口
     */
    public interface Command {}
    
    /**
     * 查询标记接口
     */
    public interface Query {}
} 