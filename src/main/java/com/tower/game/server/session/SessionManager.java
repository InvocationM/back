package com.tower.game.server.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器
 */
@Slf4j
@Component
public class SessionManager {

    // 存储所有活跃的会话：sessionId -> PlayerSession
    private final Map<String, PlayerSession> sessions = new ConcurrentHashMap<>();

    // Channel到Session的映射：Channel -> PlayerSession
    private final Map<Channel, PlayerSession> channelToSession = new ConcurrentHashMap<>();

    // 用户ID到Session的映射：userId -> PlayerSession（一个用户只能有一个活跃会话）
    private final Map<Long, PlayerSession> userIdToSession = new ConcurrentHashMap<>();

    /**
     * 创建会话
     */
    public PlayerSession createSession(Long userId, String username, Channel channel) {
        // 如果用户已有会话，先关闭旧会话
        PlayerSession oldSession = userIdToSession.get(userId);
        if (oldSession != null) {
            removeSession(oldSession.getChannel());
        }

        PlayerSession session = new PlayerSession(userId, username, channel);
        sessions.put(session.getSessionId(), session);
        channelToSession.put(channel, session);
        userIdToSession.put(userId, session);

        log.info("创建会话: sessionId={}, userId={}, username={}", 
            session.getSessionId(), userId, username);
        
        return session;
    }

    /**
     * 根据Channel获取会话
     */
    public PlayerSession getSession(Channel channel) {
        return channelToSession.get(channel);
    }

    /**
     * 根据sessionId获取会话
     */
    public PlayerSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 根据userId获取会话
     */
    public PlayerSession getSessionByUserId(Long userId) {
        return userIdToSession.get(userId);
    }

    /**
     * 移除会话
     */
    public void removeSession(Channel channel) {
        PlayerSession session = channelToSession.remove(channel);
        if (session != null) {
            sessions.remove(session.getSessionId());
            userIdToSession.remove(session.getUserId());
            log.info("移除会话: sessionId={}, userId={}", 
                session.getSessionId(), session.getUserId());
        }
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        PlayerSession session = userIdToSession.get(userId);
        return session != null && session.isActive();
    }
}
