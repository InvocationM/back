package com.tower.game.server.session;

import com.tower.game.common.enums.GameStatus;
import io.netty.channel.Channel;
import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家会话
 */
@Data
public class PlayerSession {
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
            channel.writeAndFlush(message);
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
