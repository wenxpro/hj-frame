package com.wenx.event.bus;

import com.wenx.event.Message;
import com.wenx.event.MessageHandler;
import com.wenx.v3core.error.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一消息总线 路由处理与分发
 * @auther wenx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBus {
    
    private final ApplicationContext applicationContext;
    private final Map<Class<?>, Object> handlers = new HashMap<>();
    
    @PostConstruct
    public void initHandlers() {
        // 注册MessageHandler实现类
        applicationContext.getBeansOfType(MessageHandler.class).values()
            .forEach(handler -> {
                try {
                    Class<?> messageType = handler.getMessageType();
                    if (messageType != null) {
                        handlers.put(messageType, handler);
                        log.info("注册事件处理器: {} -> {}", messageType.getSimpleName(), handler.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.warn("注册处理器失败: {}", handler.getClass().getSimpleName(), e);
                }
            });
        
        log.info("消息总线初始化完成，共注册 {} 个处理器", handlers.size());
    }

    /**
     * 发送消息（命令或查询）
     */
    @SneakyThrows
    public <T, R> R send(T message) {
        if (message == null) {
            throw new ServiceException("消息不能为空");
        }
        log.debug("发送消息: {}", message.getClass().getSimpleName());
        
        // 验证消息类型
        validateMessage(message);
        
        Object handler = handlers.get(message.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("未找到处理器: " + message.getClass().getSimpleName());
        }
        
        // 验证处理器类型匹配
        validateHandlerMessageTypeMatch(message, handler);

        R result;

        // 尝试调用统一处理器的handle(Message)方法
        try {
            Method handleMethod = handler.getClass().getMethod("handle", Message.class);
            result = (R) handleMethod.invoke(handler, message);
        } catch (NoSuchMethodException e) {
            // 回退到标准MessageHandler处理
            result = (R) handler.getClass().getMethod("handle", message.getClass()).invoke(handler, message);
        }

        log.debug("消息处理完成: {}", message.getClass().getSimpleName());
        return result;
    }
    
    /**
     * 验证消息类型
     */
    private void validateMessage(Object message) {
        if (message == null) {
            throw new ServiceException("消息不能为空");
        }
        if (!(message instanceof Message)) {
            throw new ServiceException("消息必须继承自Message基类: " + message.getClass().getSimpleName());
        }
        
        Message msg = (Message) message;
        
        if (msg.getMessageType() == null || msg.getMessageType().trim().isEmpty()) {
            throw new ServiceException("消息类型不能为空");
        }

        // 验证Command和Query的互斥性
        if (msg.isCommand() && msg.isQuery()) {
            throw new ServiceException("消息不能同时是Command和Query: " + message.getClass().getSimpleName());
        }
        
        // 验证消息必须是Command或Query之一
        if (!msg.isCommand() && !msg.isQuery()) {
            throw new ServiceException("消息必须是Command或Query之一");
        }
        
        // 验证Command命名规范
        if (msg.isCommand() && !msg.getMessageType().endsWith("_COMMAND")) {
            throw new ServiceException("Command消息类型必须以'_COMMAND'结尾: " + msg.getMessageType());
        }
        
        // 验证Query命名规范
        if (msg.isQuery() && !msg.getMessageType().endsWith("_QUERY")) {
            throw new ServiceException("Query消息类型必须以'_QUERY'结尾: " + msg.getMessageType());
        }
        
        log.debug("消息验证通过: type={}, isCommand={}, isQuery={}", 
                msg.getMessageType(), msg.isCommand(), msg.isQuery());
    }
    
    /**
     * 验证处理器与消息类型匹配
     */
    private void validateHandlerMessageTypeMatch(Object message, Object handler) {
        // 检查是否为统一处理器
        try {
            Method handleMethod = handler.getClass().getMethod("handle", Message.class);
            if (handleMethod != null) {
                return; // 统一处理器跳过类型检查
            }
        } catch (NoSuchMethodException e) {
            // 继续标准检查
        }
        
        if (!(handler instanceof MessageHandler)) {
            throw new IllegalArgumentException("处理器必须实现MessageHandler接口: " + handler.getClass().getSimpleName());
        }
        
        MessageHandler<?, ?> messageHandler = (MessageHandler<?, ?>) handler;
        Class<?> expectedMessageType = messageHandler.getMessageType();
        
        if (!expectedMessageType.isAssignableFrom(message.getClass())) {
            throw new ServiceException(
                String.format("处理器类型不匹配: 期望%s, 实际%s", 
                    expectedMessageType.getSimpleName(), message.getClass().getSimpleName()));
        }
    }
} 
