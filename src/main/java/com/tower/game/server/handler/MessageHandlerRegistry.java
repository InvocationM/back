package com.tower.game.server.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息处理器注册表
 */
@Slf4j
@Component
public class MessageHandlerRegistry {

    // 消息类型 -> 处理器映射
    private final Map<Integer, MessageHandler> handlers = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<MessageHandler> handlerList;

    @PostConstruct
    public void init() {
        if (handlerList != null) {
            for (MessageHandler handler : handlerList) {
                int messageType = handler.getMessageType();
                handlers.put(messageType, handler);
                log.info("注册消息处理器: messageType={}, handler={}", 
                    messageType, handler.getClass().getSimpleName());
            }
        }
        log.info("消息处理器注册完成，共注册 {} 个处理器", handlers.size());
    }

    /**
     * 根据消息类型获取对应的处理器
     */
    public MessageHandler getHandler(int messageType) {
        return handlers.get(messageType);
    }

    /**
     * 检查是否有对应的处理器
     */
    public boolean hasHandler(int messageType) {
        return handlers.containsKey(messageType);
    }
}
