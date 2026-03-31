package com.tower.game.server.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.constant.MessageType;
import com.tower.game.common.enums.GameStatus;
import com.tower.game.model.entity.PlayerAttribute;
import com.tower.game.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 玩家会话：连接相关留在本机，状态委托给可序列化的 SessionState（便于后续扩展 Redis 等）
 */
@Slf4j
public class PlayerSession {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(1);

    private final SessionState state;
    private final WebSocketSession webSocketSession;

    public PlayerSession(Long userId, String username, PlayerAttribute attr, WebSocketSession webSocketSession) {
        String sessionId = generateSessionId();
        long now = System.currentTimeMillis();
        this.state = SessionState.builder()
                .sessionId(sessionId)
                .userId(userId)
                .username(username)
                .gameStatus(GameStatus.IDLE)
                .mapId(null)
                .cellX(-1)
                .cellY(-1)
                .loginTime(now)
                .lastActiveTime(now)
                .hp(attr.getHp() != null ? attr.getHp() : 100)
                .maxHp(attr.getMaxHp() != null ? attr.getMaxHp() : 100)
                .attack(attr.getAttack() != null ? attr.getAttack() : 10)
                .defence(attr.getDefence() != null ? attr.getDefence() : 5)
                .dodge(attr.getDodge() != null ? attr.getDodge() : 0)
                .accurate(attr.getAccurate() != null ? attr.getAccurate() : 0)
                .crit(attr.getCrit() != null ? attr.getCrit() : 0)
                .doublehit(attr.getDoublehit() != null ? attr.getDoublehit() : 0)
                .reflect(attr.getReflect() != null ? attr.getReflect() : 0)
                .name(attr.getName() != null ? attr.getName() : "玩家")
                .icon(attr.getIcon() != null ? attr.getIcon() : "PLAYER1")
                .build();
        this.webSocketSession = webSocketSession;
    }

    private static String generateSessionId() {
        return "session_" + SESSION_ID_GENERATOR.getAndIncrement() + "_" + System.currentTimeMillis();
    }

    // ---------- 状态读写：委托给 SessionState，对外保持原有 getter/setter 兼容 ----------

    public String getSessionId() { return state.getSessionId(); }
    public Long getUserId() { return state.getUserId(); }
    public String getUsername() { return state.getUsername(); }
    public GameStatus getGameStatus() { return state.getGameStatus(); }
    public Integer getMapId() { return state.getMapId(); }
    public int getCellX() { return state.getCellX(); }
    public int getCellY() { return state.getCellY(); }
    public long getLoginTime() { return state.getLoginTime(); }
    public long getLastActiveTime() { return state.getLastActiveTime(); }
    public int getHp() { return state.getHp(); }
    public int getMaxHp() { return state.getMaxHp(); }
    public int getAttack() { return state.getAttack(); }
    public int getDefence() { return state.getDefence(); }
    public int getDodge() { return state.getDodge(); }
    public int getAccurate() { return state.getAccurate(); }
    public int getCrit() { return state.getCrit(); }
    public int getDoublehit() { return state.getDoublehit(); }
    public int getReflect() { return state.getReflect(); }
    public String getCombatName() { return state.getName(); }
    public String getCombatIcon() { return state.getIcon(); }

    public void setHp(int hp) { state.setHp(hp); }
    public void setGameStatus(GameStatus gameStatus) { state.setGameStatus(gameStatus); }
    public void setMapId(Integer mapId) {
        if (mapId != null && state.getMapId() != null && !mapId.equals(state.getMapId())) {
            state.clearCurrentMapData();
        }
        state.setMapId(mapId);
    }
    public void setCellX(int cellX) { state.setCellX(cellX); }
    public void setCellY(int cellY) { state.setCellY(cellY); }

    /** 当前地图缓存：与 mapId 一致时有效 */
    public Integer getCurrentMapId() { return state.getCurrentMapId(); }
    public String getCurrentMapData() { return state.getCurrentMapData(); }
    public void setCurrentMapData(Integer mapId, String data) {
        state.setCurrentMapId(mapId);
        state.setCurrentMapData(data);
    }
    public boolean hasCurrentMapDataFor(Integer mapId) { return state.hasCurrentMapDataFor(mapId); }
    public void clearCurrentMapData() { state.clearCurrentMapData(); }

    public WebSocketSession getWebSocketSession() { return webSocketSession; }

    /**
     * 获取当前会话状态快照（副本，避免外部直接修改内部状态）
     */
    public SessionState getState() {
        return SessionState.builder()
                .sessionId(state.getSessionId())
                .userId(state.getUserId())
                .username(state.getUsername())
                .gameStatus(state.getGameStatus())
                .mapId(state.getMapId())
                .cellX(state.getCellX())
                .cellY(state.getCellY())
                .currentMapId(state.getCurrentMapId())
                .currentMapData(state.getCurrentMapData())
                .loginTime(state.getLoginTime())
                .lastActiveTime(state.getLastActiveTime())
                .hp(state.getHp())
                .maxHp(state.getMaxHp())
                .attack(state.getAttack())
                .defence(state.getDefence())
                .dodge(state.getDodge())
                .accurate(state.getAccurate())
                .crit(state.getCrit())
                .doublehit(state.getDoublehit())
                .reflect(state.getReflect())
                .name(state.getName())
                .icon(state.getIcon())
                .build();
    }

    /**
     * 更新会话状态（便于批量修改与后续扩展写回 Redis 等）
     */
    public void updateState(Consumer<SessionState> updater) {
        updater.accept(state);
    }

    /**
     * 是否已设置地图位置（已进图）
     */
    public boolean hasPosition() {
        return state.hasPosition();
    }

    /**
     * 更新最后活跃时间
     */
    public void updateActiveTime() {
        state.setLastActiveTime(System.currentTimeMillis());
    }

    // ---------- 连接相关 ----------

    public void sendMessage(Object message) {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            try {
                String json = message instanceof String
                        ? (String) message
                        : objectMapper.writeValueAsString(message);
                webSocketSession.sendMessage(new TextMessage(json));

                int type = resolveMessageType(message, json);
                String payloadLog = JsonUtil.truncateForLog(json);
                if (type == MessageType.HEARTBEAT) {
                    log.debug("WS 出参 [{}] sessionId={} userId={} {}", type, state.getSessionId(), state.getUserId(), payloadLog);
                } else {
                    log.info("WS 出参 [{}] sessionId={} userId={} {}", type, state.getSessionId(), state.getUserId(), payloadLog);
                }
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", state.getSessionId(), e);
            }
        }
    }

    private static int resolveMessageType(Object message, String json) {
        if (message instanceof Map<?, ?> m) {
            Object t = m.get("type");
            if (t instanceof Number n) return n.intValue();
        }
        try {
            JsonNode node = JsonUtil.parseObject(json);
            if (node != null && node.has("type")) return node.get("type").asInt(-1);
        } catch (Exception ignored) {
            // ignore
        }
        return -1;
    }

    public boolean isActive() {
        return webSocketSession != null && webSocketSession.isOpen();
    }
}
