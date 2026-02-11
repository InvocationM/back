package com.tower.game.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.constant.MessageType;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 游戏消息处理器 - WebSocket版本
 */
@Slf4j
@Component
public class GameMessageHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    // 写死的测试用户
    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_USERNAME = "test_user";
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MessageHandlerRegistry handlerRegistry;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 只处理文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            log.warn("不支持的消息类型: {}", frame.getClass().getName());
            return;
        }

        TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
        String text = textFrame.text();
        
        log.debug("收到WebSocket消息: {}", text);

        PlayerSession session = sessionManager.getSession(ctx.channel());
        
        // 如果没有会话，自动创建写死的测试用户会话
        if (session == null) {
            log.info("自动创建测试用户会话: userId={}, username={}", TEST_USER_ID, TEST_USERNAME);
            session = sessionManager.createSession(TEST_USER_ID, TEST_USERNAME, ctx.channel());
        }

        // 更新最后活跃时间
        session.updateActiveTime();

        try {
            // 解析JSON消息
            Map<String, Object> message = objectMapper.readValue(text, Map.class);
            int messageType = (Integer) message.getOrDefault("type", MessageType.HEARTBEAT);
            
            // 从注册表获取对应的处理器
            MessageHandler handler = handlerRegistry.getHandler(messageType);
            
            if (handler != null) {
                handler.handle(session, message);
            } else {
                log.warn("未找到消息处理器: messageType={}, sessionId={}", 
                    messageType, session.getSessionId());
                sendError(session, "未知的消息类型: " + messageType);
            }
        } catch (Exception e) {
            log.error("解析消息失败: {}", text, e);
            sendError(session, "消息格式错误");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("WebSocket客户端连接: {}", ctx.channel().remoteAddress());
        // 连接时自动创建写死的测试用户会话
        PlayerSession session = sessionManager.createSession(TEST_USER_ID, TEST_USERNAME, ctx.channel());
        log.info("自动创建测试用户会话: sessionId={}, userId={}, username={}", 
            session.getSessionId(), TEST_USER_ID, TEST_USERNAME);
        
        // 发送欢迎消息
        sendWelcomeMessage(session);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("WebSocket客户端断开连接: {}", ctx.channel().remoteAddress());
        sessionManager.removeSession(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接异常", cause);
        sessionManager.removeSession(ctx.channel());
        ctx.close();
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
        try {
            String json = objectMapper.writeValueAsString(welcome);
            session.sendMessage(new TextWebSocketFrame(json));
        } catch (Exception e) {
            log.error("发送欢迎消息失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(PlayerSession session, String errorMsg) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", -1);
        errorResponse.put("code", 500);
        errorResponse.put("message", errorMsg);
        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextWebSocketFrame(json));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }
}
