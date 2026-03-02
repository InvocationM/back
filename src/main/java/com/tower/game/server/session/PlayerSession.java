package com.tower.game.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.enums.GameStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
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

    public PlayerSession(Long userId, String username, WebSocketSession webSocketSession) {
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
                .hp(100)
                .maxHp(100)
                .attack(10)
                .defence(5)
                .dodge(0)
                .accurate(0)
                .crit(0)
                .doublehit(0)
                .reflect(0)
                .name("玩家")
                .icon("PLAYER1")
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
    public void setMapId(Integer mapId) { state.setMapId(mapId); }
    public void setCellX(int cellX) { state.setCellX(cellX); }
    public void setCellY(int cellY) { state.setCellY(cellY); }

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
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", state.getSessionId(), e);
            }
        }
    }

    public boolean isActive() {
        return webSocketSession != null && webSocketSession.isOpen();
    }
}
