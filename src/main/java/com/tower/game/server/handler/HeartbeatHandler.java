package com.tower.game.server.handler;

import com.tower.game.common.constant.MessageType;
import com.tower.game.server.session.PlayerSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 心跳消息处理器
 */
@Slf4j
@Component
public class HeartbeatHandler implements MessageHandler {

    @Override
    public void handle(PlayerSession session, Object message) {
        log.debug("收到心跳消息: sessionId={}", session.getSessionId());
        
        // 更新活跃时间
        session.updateActiveTime();
        
        // 回复心跳
        Map<String, Object> response = new HashMap<>();
        response.put("type", MessageType.HEARTBEAT);
        response.put("code", 200);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "pong");
        
        session.sendMessage(response);
    }

    @Override
    public int getMessageType() {
        return MessageType.HEARTBEAT;
    }
}
