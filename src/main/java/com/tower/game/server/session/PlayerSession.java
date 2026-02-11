package com.tower.game.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.enums.GameStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家会话
 */
@Slf4j
@Data
public class PlayerSession {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(1);

    private String sessionId;
    private Long userId;
    private String username;
    private WebSocketSession webSocketSession;
    private long loginTime;
    private long lastActiveTime;
    private GameStatus gameStatus;

    public PlayerSession(Long userId, String username, WebSocketSession webSocketSession) {
        this.sessionId = generateSessionId();
        this.userId = userId;
        this.username = username;
        this.webSocketSession = webSocketSession;
        this.loginTime = System.currentTimeMillis();
        this.lastActiveTime = System.currentTimeMillis();
        this.gameStatus = GameStatus.IDLE;
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "session_" + SESSION_ID_GENERATOR.getAndIncrement() + "_" + System.currentTimeMillis();
    }

    /**
     * 发送消息给客户端
     */
    public void sendMessage(Object message) {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            try {
                String json;
                if (message instanceof String) {
                    json = (String) message;
                } else {
                    // 如果是Map或其他对象，转换为JSON
                    json = objectMapper.writeValueAsString(message);
                }
                webSocketSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", sessionId, e);
            }
        }
    }

    /**
     * 检查连接是否活跃
     */
    public boolean isActive() {
        return webSocketSession != null && webSocketSession.isOpen();
    }

    /**
     * 更新最后活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
}
