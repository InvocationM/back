package com.tower.game.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.enums.GameStatus;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
    private Channel channel;
    private long loginTime;
    private long lastActiveTime;
    private GameStatus gameStatus;

    public PlayerSession(Long userId, String username, Channel channel) {
        this.sessionId = generateSessionId();
        this.userId = userId;
        this.username = username;
        this.channel = channel;
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
        if (channel != null && channel.isActive()) {
            if (message instanceof WebSocketFrame) {
                channel.writeAndFlush(message);
            } else if (message instanceof String) {
                // 如果是字符串，包装成TextWebSocketFrame
                channel.writeAndFlush(new TextWebSocketFrame((String) message));
            } else {
                // 如果是Map或其他对象，转换为JSON
                try {
                    String json = objectMapper.writeValueAsString(message);
                    channel.writeAndFlush(new TextWebSocketFrame(json));
                } catch (Exception e) {
                    log.error("发送消息失败", e);
                }
            }
        }
    }

    /**
     * 检查连接是否活跃
     */
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    /**
     * 更新最后活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
}
