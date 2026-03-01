package com.tower.game.server.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息处理器注册表（按消息类型分发到对应 MessageProcessor）
 */
@Slf4j
@Component
public class MessageProcessorRegistry {

    // 消息类型 -> 处理器映射
    private final Map<Integer, MessageProcessor> processors = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<MessageProcessor> processorList;

    @PostConstruct
    public void init() {
        if (processorList != null) {
            for (MessageProcessor processor : processorList) {
                int messageType = processor.getMessageType();
                processors.put(messageType, processor);
                log.info("注册消息处理器processor: messageType={}, processor={}",
                    messageType, processor.getClass().getSimpleName());
            }
        }
        log.info("消息处理器processor注册完成，共注册 {} 个处理器", processors.size());
    }

    /**
     * 根据消息类型获取对应的处理器
     */
    public MessageProcessor getProcessor(int messageType) {
        return processors.get(messageType);
    }

    /**
     * 检查是否有对应的处理器
     */
    public boolean hasProcessor(int messageType) {
        return processors.containsKey(messageType);
    }
}
