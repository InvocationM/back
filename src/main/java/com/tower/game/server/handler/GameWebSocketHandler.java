package com.tower.game.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.tower.game.common.auth.CurrentUser;
import com.tower.game.common.constant.MessageType;
import com.tower.game.model.entity.PlayerAttribute;
import com.tower.game.server.processor.MessageProcessor;
import com.tower.game.server.processor.MessageProcessorRegistry;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionManager;
import com.tower.game.service.AuthTokenService;
import com.tower.game.service.PlayerAttributeService;
import com.tower.game.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final long EXECUTOR_EXPIRE_AFTER_ACCESS_MINUTES = 30;
    private static final int EXECUTOR_CACHE_MAX_SIZE = 10_000;
    private static final long EXECUTOR_GRACEFUL_AWAIT_SECONDS = 1;
    private static final long MONITOR_INTERVAL_SECONDS = 60;
    private static final int BACKLOG_WARN_THRESHOLD = 100;

    private static final String TYPE_PREFIX = "\"type\":100";
    private static final Pattern TYPE_PATTERN = Pattern.compile("\"type\"\\s*:\\s*(-?\\d+)");

    private static boolean isDirectMessageByString(String text) {
        if (text == null || text.isBlank()) return false;
        if (!text.contains(TYPE_PREFIX)) return false;
        Matcher matcher = TYPE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int type = Integer.parseInt(matcher.group(1));
                return type == MessageType.HEARTBEAT || type == MessageType.LOGIN || type == MessageType.LOGOUT;
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

    private final Cache<String, ExecutorService> executorCache = Caffeine.newBuilder()
            .expireAfterAccess(EXECUTOR_EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES)
            .maximumSize(EXECUTOR_CACHE_MAX_SIZE)
            .removalListener((String key, ExecutorService executor, RemovalCause cause) -> {
                if (executor != null) {
                    shutdownExecutorGracefully(executor);
                }
            })
            .build();

    private final AtomicLong totalEnqueued = new AtomicLong(0);
    private final AtomicLong totalCompleted = new AtomicLong(0);

    private ScheduledExecutorService monitorScheduler;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private MessageProcessorRegistry processorRegistry;

    @Autowired
    private PlayerAttributeService playerAttributeService;

    @Autowired
    private AuthTokenService authTokenService;

    @PostConstruct
    public void startMonitor() {
        monitorScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ws-monitor");
            thread.setDaemon(true);
            return thread;
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
            log.warn("WebSocket backlog: enqueued={}, completed={}, backlog={}", enqueued, completed, backlog);
        } else {
            log.debug("WebSocket queue: enqueued={}, completed={}, backlog={}", enqueued, completed, backlog);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket client connected: {}", session.getRemoteAddress());
        CurrentUser user;
        try {
            user = authTokenService.parseToken(extractToken(session));
        } catch (Exception e) {
            log.warn("WebSocket token invalid: {}", e.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        PlayerAttribute attr = playerAttributeService.getByPlayerId(user.getUserId());
        if (attr == null) {
            log.error("Player attribute not found: userId={}", user.getUserId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        PlayerSession playerSession = sessionManager.createSession(user.getUserId(), user.getUsername(), attr, session);
        log.info("Created user session: sessionId={}, userId={}, username={}",
                playerSession.getSessionId(), user.getUserId(), user.getUsername());
        sendWelcomeMessage(playerSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String text = message.getPayload();

        PlayerSession playerSession = sessionManager.getSession(session);
        if (playerSession == null) {
            log.warn("WebSocket session missing, closing connection: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        playerSession.updateActiveTime();

        if (isDirectMessageByString(text)) {
            try {
                processMessageInPlayerThread(playerSession, text);
            } catch (Exception e) {
                log.error("Direct message failed: {}", text, e);
                sendError(playerSession, "消息格式错误");
            }
            return;
        }

        final String rawPayload = text;
        String sessionKey = session.getId();
        ExecutorService executor = executorCache.get(sessionKey, id ->
                Executors.newSingleThreadExecutor(r -> {
                    Thread thread = new Thread(r, "ws-player-" + id);
                    thread.setDaemon(true);
                    return thread;
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
                log.error("Message handling failed: sessionId={}", ps.getSessionId(), e);
                sendError(ps, "消息格式错误");
            } finally {
                totalCompleted.incrementAndGet();
            }
        });
    }

    private void processMessageInPlayerThread(PlayerSession playerSession, String rawPayload) {
        Map<String, Object> msg;
        try {
            msg = objectMapper.readValue(rawPayload, Map.class);
        } catch (Exception e) {
            log.error("Parse message failed: {}", rawPayload, e);
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
            log.debug("WS input [{}] sessionId={} userId={} {}", messageType, sessionId, userId, payloadLog);
        } else {
            log.info("WS input [{}] sessionId={} userId={} {}", messageType, sessionId, userId, payloadLog);
        }

        long start = System.currentTimeMillis();
        try {
            MessageProcessor processor = processorRegistry.getProcessor(messageType);
            if (processor != null) {
                processor.handle(playerSession, msg);
            } else {
                log.warn("Processor not found: messageType={}, sessionId={}", messageType, sessionId);
                sendError(playerSession, "未知的消息类型 " + messageType);
            }
        } catch (Exception e) {
            log.error("Handle message failed: messageType={}, sessionId={}", messageType, sessionId, e);
            sendError(playerSession, "消息格式错误");
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (messageType == MessageType.HEARTBEAT) {
                log.debug("WS done [{}] sessionId={} cost={}ms", messageType, sessionId, cost);
            } else {
                log.info("WS done [{}] sessionId={} cost={}ms", messageType, sessionId, cost);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket client disconnected: {}, status: {}", session.getRemoteAddress(), status);
        sessionManager.removeSession(session);
        executorCache.invalidate(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error", exception);
        sessionManager.removeSession(session);
        executorCache.invalidate(session.getId());
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() == null ? null : session.getUri().getRawQuery();
        if (query == null || query.isBlank()) return null;
        for (String part : query.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
            if (!"token".equals(key)) continue;
            return URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private void sendWelcomeMessage(PlayerSession session) {
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", 0);
        welcome.put("code", 200);
        welcome.put("message", "连接成功");
        welcome.put("userId", session.getUserId());
        welcome.put("username", session.getUsername());
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
