package com.tower.game.server.handler;

import com.tower.game.common.constant.MessageType;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 游戏消息处理器
 */
@Slf4j
@Component
public class GameMessageHandler extends SimpleChannelInboundHandler<Object> {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MessageHandlerRegistry handlerRegistry;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        PlayerSession session = sessionManager.getSession(ctx.channel());
        
        if (session == null) {
            log.warn("收到消息但未找到会话: channel={}", ctx.channel());
            return;
        }

        // 更新最后活跃时间
        session.updateActiveTime();

        // 解析消息类型（这里简化处理，实际应该根据协议解析）
        int messageType = parseMessageType(msg);
        
        // 从注册表获取对应的处理器
        MessageHandler handler = handlerRegistry.getHandler(messageType);
        
        if (handler != null) {
            try {
                handler.handle(session, msg);
            } catch (Exception e) {
                log.error("处理消息异常: messageType={}, sessionId={}", 
                    messageType, session.getSessionId(), e);
                sendError(session, "处理消息失败");
            }
        } else {
            log.warn("未找到消息处理器: messageType={}, sessionId={}", 
                messageType, session.getSessionId());
            sendError(session, "未知的消息类型");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开连接: {}", ctx.channel().remoteAddress());
        sessionManager.removeSession(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接异常", cause);
        sessionManager.removeSession(ctx.channel());
        ctx.close();
    }

    /**
     * 解析消息类型（简化实现，实际应该根据协议解析）
     */
    private int parseMessageType(Object msg) {
        // TODO: 根据实际协议解析消息类型
        // 这里假设消息是一个Map，包含type字段
        if (msg instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) msg;
            Object type = map.get("type");
            if (type instanceof Integer) {
                return (Integer) type;
            }
        }
        return MessageType.HEARTBEAT; // 默认返回心跳
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
