package com.tower.game.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.constant.MessageType;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 游戏WebSocket处理器
 */
@Slf4j
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    // 写死的测试用户
    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_USERNAME = "test_user";
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MessageHandlerRegistry handlerRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket客户端连接: {}", session.getRemoteAddress());
        // 连接时自动创建写死的测试用户会话
        PlayerSession playerSession = sessionManager.createSession(TEST_USER_ID, TEST_USERNAME, session);
        log.info("自动创建测试用户会话: sessionId={}, userId={}, username={}", 
            playerSession.getSessionId(), TEST_USER_ID, TEST_USERNAME);
        
        // 发送欢迎消息
        sendWelcomeMessage(playerSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String text = message.getPayload();
        log.debug("收到WebSocket消息: {}", text);

        PlayerSession playerSession = sessionManager.getSession(session);
        
        // 如果没有会话，自动创建写死的测试用户会话
        if (playerSession == null) {
            log.info("自动创建测试用户会话: userId={}, username={}", TEST_USER_ID, TEST_USERNAME);
            playerSession = sessionManager.createSession(TEST_USER_ID, TEST_USERNAME, session);
        }

        // 更新最后活跃时间
        playerSession.updateActiveTime();

        try {
            // 解析JSON消息
            Map<String, Object> msg = objectMapper.readValue(text, Map.class);
            int messageType = (Integer) msg.getOrDefault("type", MessageType.HEARTBEAT);
            
            // 从注册表获取对应的处理器
            MessageHandler handler = handlerRegistry.getHandler(messageType);
            
            if (handler != null) {
                handler.handle(playerSession, msg);
            } else {
                log.warn("未找到消息处理器: messageType={}, sessionId={}", 
                    messageType, playerSession.getSessionId());
                sendError(playerSession, "未知的消息类型: " + messageType);
            }
        } catch (Exception e) {
            log.error("解析消息失败: {}", text, e);
            sendError(playerSession, "消息格式错误");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket客户端断开连接: {}, status: {}", session.getRemoteAddress(), status);
        sessionManager.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        sessionManager.removeSession(session);
    }

    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(PlayerSession session) {
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", 0);
        welcome.put("code", 200);
        welcome.put("message", "连接成功");
        welcome.put("userId", TEST_USER_ID);
        welcome.put("username", TEST_USERNAME);
        session.sendMessage(welcome);
    }

    /**
     * 发送错误消息
     */
    private void sendError(PlayerSession session, String errorMsg) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", -1);
        errorResponse.put("code", 500);
        errorResponse.put("message", errorMsg);
        session.sendMessage(errorResponse);
    }
}
