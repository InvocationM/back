package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.MapWalkableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家移动消息处理器（第一版最小闭环：一步校验，200/400）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerMoveProcessor implements MessageProcessor {

    private static final int DEFAULT_MAP_ID = 1001;

    private final MapWalkableService mapWalkableService;

    @Override
    public void handle(PlayerSession session, Object message) {

        log.info("session :{}, message", message.toString());

        if (!(message instanceof Map)) {
            sendFail(session, "消息格式错误");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        Integer toX = getInt(msg, "toX");
        Integer toY = getInt(msg, "toY");
        if (toX == null || toY == null) {
            sendFail(session, "缺少 toX 或 toY");
            return;
        }
        Integer mapIdParam = getInt(msg, "mapId");

        if (!session.hasPosition()) {
            int mapId = mapIdParam != null ? mapIdParam : DEFAULT_MAP_ID;
            int[] entrance = mapWalkableService.findEntrance(mapId);
            session.setMapId(mapId);
            session.setCellX(entrance[0]);
            session.setCellY(entrance[1]);
        }

        int fromX = session.getCellX();
        int fromY = session.getCellY();
        Integer sessionMapId = session.getMapId();
        if (sessionMapId == null) sessionMapId = DEFAULT_MAP_ID;

        if (Math.abs(toX - fromX) + Math.abs(toY - fromY) != 1) {
            sendFail(session, "目标格与当前位置不相邻");
            return;
        }
        if (!mapWalkableService.isWalkable(sessionMapId, toX, toY)) {
            sendFail(session, "目标格不可通行");
            return;
        }

        session.setCellX(toX);
        session.setCellY(toY);
        Map<String, Object> ok = new HashMap<>();
        ok.put("type", MessageType.PLAYER_MOVE);
        ok.put("code", 200);
        ok.put("cellX", toX);
        ok.put("cellY", toY);
        session.sendMessage(ok);
        log.debug("玩家移动: userId={} -> ({},{})", session.getUserId(), toX, toY);
    }

    @Override
    public int getMessageType() {
        return MessageType.PLAYER_MOVE;
    }

    private void sendFail(PlayerSession session, String message) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.PLAYER_MOVE);
        fail.put("code", 400);
        fail.put("message", message);
        session.sendMessage(fail);
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
