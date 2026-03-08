package com.tower.game.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.tower.game.common.constant.MessageType;
import com.tower.game.server.processor.MessageProcessor;
import com.tower.game.util.JsonUtil;
import com.tower.game.server.processor.MessageProcessorRegistry;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 游戏WebSocket处理器。
 * 同一玩家的消息（除心跳1000/1001/1002外）由单线程Executor顺序执行，避免移动与战斗等消息并发导致的竞态。
 * 队列消息以原始字符串传入，在玩家单线程内解析并执行，保证同一玩家状态读写无跨线程可见性问题；Executor 由 Caffeine 管理，带监控。
 */
@Slf4j
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    // ---------- 可调配置 ----------
    private static final long EXECUTOR_EXPIRE_AFTER_ACCESS_MINUTES = 30;
    private static final int EXECUTOR_CACHE_MAX_SIZE = 10_000;
    private static final long EXECUTOR_GRACEFUL_AWAIT_SECONDS = 1;
    private static final long MONITOR_INTERVAL_SECONDS = 60;
    private static final int BACKLOG_WARN_THRESHOLD = 100;

    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_USERNAME = "test_user";

    /** 心跳快速判断：先 contains 再 Pattern 保底 */
    private static final String TYPE_PREFIX = "\"type\":100";
    private static final Pattern TYPE_PATTERN = Pattern.compile("\"type\"\\s*:\\s*(-?\\d+)");

    private static boolean isDirectMessageByString(String text) {
        if (text == null || text.isBlank()) return false;
        if (!text.contains(TYPE_PREFIX)) return false;
        Matcher m = TYPE_PATTERN.matcher(text);
        if (m.find()) {
            try {
                int t = Integer.parseInt(m.group(1));
                return t == MessageType.HEARTBEAT || t == MessageType.LOGIN || t == MessageType.LOGOUT;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private static void shutdownExecutorGracefully(ExecutorService executor) {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(EXECUTOR_GRACEFUL_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 玩家 Executor 缓存：过期或淘汰时优雅关闭 */
    private final Cache<String, ExecutorService> executorCache = Caffeine.newBuilder()
            .expireAfterAccess(EXECUTOR_EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES)
            .maximumSize(EXECUTOR_CACHE_MAX_SIZE)
            .removalListener((String key, ExecutorService executor, RemovalCause cause) -> {
                if (executor != null) {
                    shutdownExecutorGracefully(executor);
                }
            })
            .build();

    /** 监控：总入队、完成 */
    private final AtomicLong totalEnqueued = new AtomicLong(0);
    private final AtomicLong totalCompleted = new AtomicLong(0);

    private ScheduledExecutorService monitorScheduler;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MessageProcessorRegistry processorRegistry;

    @PostConstruct
    public void startMonitor() {
        monitorScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-monitor");
            t.setDaemon(true);
            return t;
        });
        monitorScheduler.scheduleAtFixedRate(
                this::logQueueStats,
                MONITOR_INTERVAL_SECONDS,
                MONITOR_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopMonitor() {
        if (monitorScheduler != null) {
            monitorScheduler.shutdownNow();
        }
        executorCache.invalidateAll();
    }

    private void logQueueStats() {
        long enqueued = totalEnqueued.get();
        long completed = totalCompleted.get();
        long backlog = enqueued - completed;
        if (backlog > BACKLOG_WARN_THRESHOLD) {
            log.warn("WebSocket 队列积压: 入队={}, 完成={}, 积压={}", enqueued, completed, backlog);
        } else {
            log.debug("WebSocket 队列: 入队={}, 完成={}, 积压={}", enqueued, completed, backlog);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket客户端连接: {}", session.getRemoteAddress());
        PlayerSession playerSession = sessionManager.createSession(TEST_USER_ID, TEST_USERNAME, session);
        log.info("自动创建测试用户会话: sessionId={}, userId={}, username={}",
                playerSession.getSessionId(), TEST_USER_ID, TEST_USERNAME);
        sendWelcomeMessage(playerSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String text = message.getPayload();

        PlayerSession playerSession = sessionManager.getSession(session);
        if (playerSession == null) {
            log.info("自动创建测试用户会话: userId={}, username={}", TEST_USER_ID, TEST_USERNAME);
            playerSession = sessionManager.createSession(TEST_USER_ID, TEST_USERNAME, session);
        }
        playerSession.updateActiveTime();

        if (isDirectMessageByString(text)) {
            try {
                processMessageInPlayerThread(playerSession, text);
            } catch (Exception e) {
                log.error("解析或处理直接消息失败: {}", text, e);
                sendError(playerSession, "消息格式错误");
            }
            return;
        }

        final String rawPayload = text;
        String sessionKey = session.getId();
        ExecutorService executor = executorCache.get(sessionKey, id ->
                Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "ws-player-" + id);
                    t.setDaemon(true);
                    return t;
                }));

        totalEnqueued.incrementAndGet();
        executor.submit(() -> {
            PlayerSession ps = sessionManager.getSession(session);
            if (ps == null) {
                totalCompleted.incrementAndGet();
                return;
            }
            ps.updateActiveTime();
            try {
                processMessageInPlayerThread(ps, rawPayload);
            } catch (Exception e) {
                log.error("消息处理异常: sessionId={}", ps.getSessionId(), e);
                sendError(ps, "消息格式错误");
            } finally {
                totalCompleted.incrementAndGet();
            }
        });
    }

    /**
     * 解析原始消息并分发。直接消息在 tomcat 线程调用；队列消息在玩家单线程（ws-player-xxx）内调用，保证状态读写同线程可见。
     */
    private void processMessageInPlayerThread(PlayerSession playerSession, String rawPayload) {
        Map<String, Object> msg;
        try {
            msg = objectMapper.readValue(rawPayload, Map.class);
        } catch (Exception e) {
            log.error("玩家线程解析消息失败: {}", rawPayload, e);
            sendError(playerSession, "消息格式错误");
            return;
        }
        int messageType = (Integer) msg.getOrDefault("type", MessageType.HEARTBEAT);
        dispatchMessage(playerSession, messageType, msg);
    }

    private void dispatchMessage(PlayerSession playerSession, int messageType, Map<String, Object> msg) {
        String sessionId = playerSession.getSessionId();
        Long userId = playerSession.getUserId();
        String payloadLog = JsonUtil.truncateForLog(JsonUtil.toJsonString(msg));

        if (messageType == MessageType.HEARTBEAT) {
            log.debug("WS 入参 [{}] sessionId={} userId={} {}", messageType, sessionId, userId, payloadLog);
        } else {
            log.info("WS 入参 [{}] sessionId={} userId={} {}", messageType, sessionId, userId, payloadLog);
        }

        long start = System.currentTimeMillis();
        try {
            MessageProcessor processor = processorRegistry.getProcessor(messageType);
            if (processor != null) {
                processor.handle(playerSession, msg);
            } else {
                log.warn("未找到消息处理器: messageType={}, sessionId={}",
                        messageType, sessionId);
                sendError(playerSession, "未知的消息类型: " + messageType);
            }
        } catch (Exception e) {
            log.error("处理消息失败: messageType={}, sessionId={}", messageType, sessionId, e);
            sendError(playerSession, "消息格式错误");
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (messageType == MessageType.HEARTBEAT) {
                log.debug("WS 处理完成 [{}] sessionId={} 耗时 {}ms", messageType, sessionId, cost);
            } else {
                log.info("WS 处理完成 [{}] sessionId={} 耗时 {}ms", messageType, sessionId, cost);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket客户端断开连接: {}, status: {}", session.getRemoteAddress(), status);
        sessionManager.removeSession(session);
        executorCache.invalidate(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        sessionManager.removeSession(session);
        executorCache.invalidate(session.getId());
    }

    private void sendWelcomeMessage(PlayerSession session) {
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", 0);
        welcome.put("code", 200);
        welcome.put("message", "连接成功");
        welcome.put("userId", TEST_USER_ID);
        welcome.put("username", TEST_USERNAME);
        session.sendMessage(welcome);
    }

    private void sendError(PlayerSession session, String errorMsg) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", -1);
        errorResponse.put("code", 500);
        errorResponse.put("message", errorMsg);
        session.sendMessage(errorResponse);
    }
}
