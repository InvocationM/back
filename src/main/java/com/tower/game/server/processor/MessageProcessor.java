package com.tower.game.server.processor;

import com.tower.game.server.session.PlayerSession;

/**
 * 消息处理器接口（按消息类型处理的业务处理器）
 */
public interface MessageProcessor {
    /**
     * 处理消息
     */
    void handle(PlayerSession session, Object message);

    /**
     * 获取该处理器处理的消息类型
     */
    int getMessageType();
}
