package com.tower.game.server.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
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

    // WebSocketSession到Session的映射：WebSocketSession -> PlayerSession
    private final Map<WebSocketSession, PlayerSession> sessionToPlayer = new ConcurrentHashMap<>();

    // 用户ID到Session的映射：userId -> PlayerSession（一个用户只能有一个活跃会话）
    private final Map<Long, PlayerSession> userIdToSession = new ConcurrentHashMap<>();

    /**
     * 创建会话
     */
    public PlayerSession createSession(Long userId, String username, WebSocketSession webSocketSession) {
        // 如果用户已有会话，先关闭旧会话
        PlayerSession oldSession = userIdToSession.get(userId);
        if (oldSession != null) {
            removeSession(oldSession.getWebSocketSession());
        }

        PlayerSession session = new PlayerSession(userId, username, webSocketSession);
        sessions.put(session.getSessionId(), session);
        sessionToPlayer.put(webSocketSession, session);
        userIdToSession.put(userId, session);

        log.info("创建会话: sessionId={}, userId={}, username={}", 
            session.getSessionId(), userId, username);
        
        return session;
    }

    /**
     * 根据WebSocketSession获取会话
     */
    public PlayerSession getSession(WebSocketSession webSocketSession) {
        return sessionToPlayer.get(webSocketSession);
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
    public void removeSession(WebSocketSession webSocketSession) {
        PlayerSession session = sessionToPlayer.remove(webSocketSession);
        if (session != null) {
            sessions.remove(session.getSessionId());
            userIdToSession.remove(session.getUserId());
            log.info("移除会话: sessionId={}, userId={}", 
                session.getSessionId(), session.getUserId());
        }
    }

    /**
     * 获取所有会话
     */
    public Collection<PlayerSession> getAllSessions() {
        return sessions.values();
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
