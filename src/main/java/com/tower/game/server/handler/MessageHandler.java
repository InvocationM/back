package com.tower.game.server.handler;

import com.tower.game.server.session.PlayerSession;

/**
 * 消息处理器接口
 */
public interface MessageHandler {
    /**
     * 处理消息
     */
    void handle(PlayerSession session, Object message);

    /**
     * 获取该处理器处理的消息类型
     */
    int getMessageType();
}
