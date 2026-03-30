package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.model.entity.GameMap;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.GameMapService;
import com.tower.game.service.MapPathService;
import com.tower.game.service.MapWalkableService;
import com.tower.game.service.SessionMapRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动意图处理器（方案 A）：客户端发 targetX/targetY/seqId，服务端只校验「移动到 target 格」的合法性，
 * 更新权威坐标并返回 SUCCESS 或 400 失败；不再因遇怪/遇宝箱返回 INTERRUPTED。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoveIntentProcessor implements MessageProcessor {

    private static final int DEFAULT_MAP_ID = 1001;
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final MapWalkableService mapWalkableService;
    private final MapPathService mapPathService;
    private final GameMapService gameMapService;
    private final SessionMapRedisService sessionMapRedisService;

    /** 确保 Session 中已加载该 mapId 的地图数据（用于复用，避免同请求内重复查库） */
    private void ensureSessionMapLoaded(PlayerSession session, int mapId) {
        if (session.hasCurrentMapDataFor(mapId)) return;
        GameMap map = gameMapService.getByMapId(mapId);
        if (map != null && map.getData() != null && !map.getData().isBlank()) {
            session.setCurrentMapData(mapId, map.getData());
            sessionMapRedisService.saveMapJson(session.getUserId(), mapId, map.getData());
        }
    }

    @Override
    public void handle(PlayerSession session, Object message) {
        if (!(message instanceof Map)) {
            sendFail(session, "消息格式错误", null, null, null);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        Integer targetX = getInt(msg, "targetX");
        Integer targetY = getInt(msg, "targetY");
        Integer seqId = getInt(msg, "seqId");

        if (targetX == null || targetY == null) {
            sendFail(session, "缺少 targetX 或 targetY", null, null, seqId);
            return;
        }

        Integer mapIdParam = getInt(msg, "mapId");
        int mapId = mapIdParam != null ? mapIdParam : DEFAULT_MAP_ID;

        if (!session.hasPosition()) {
            ensureSessionMapLoaded(session, mapId);
            String mapData = session.getCurrentMapData();
            int[] entrance = mapWalkableService.findEntrance(mapId, mapData);
            Integer prevMapId = session.getMapId();
            if (prevMapId != null && !prevMapId.equals(mapId)) {
                sessionMapRedisService.deleteMapJson(session.getUserId(), prevMapId);
            }
            session.setMapId(mapId);
            session.setCellX(entrance[0]);
            session.setCellY(entrance[1]);
            sendSuccess(session, entrance[0], entrance[1], seqId, List.of());
            return;
        }

        int fromX = session.getCellX();
        int fromY = session.getCellY();
        Integer sessionMapId = session.getMapId();
        if (sessionMapId == null) sessionMapId = DEFAULT_MAP_ID;

        ensureSessionMapLoaded(session, sessionMapId);
        String mapData = session.getCurrentMapData();

        int[] size = mapWalkableService.getMapSize(sessionMapId, mapData);
        int width = size[0], height = size[1];
        if (targetX < 0 || targetX >= width || targetY < 0 || targetY >= height) {
            sendFail(session, "目标格超出范围", fromX, fromY, seqId);
            return;
        }

        if (targetX == fromX && targetY == fromY) {
            sendSuccess(session, fromX, fromY, seqId, List.of());
            return;
        }

        List<int[]> path = mapPathService.findPath(sessionMapId, fromX, fromY, targetX, targetY, mapData);
        if (path.isEmpty()) {
            sendFail(session, "无法到达目标格", fromX, fromY, seqId);
            return;
        }

        int[] lastCell = path.get(path.size() - 1);
        int lastValidX = lastCell[0];
        int lastValidY = lastCell[1];
        session.setCellX(lastValidX);
        session.setCellY(lastValidY);
        sendSuccess(session, lastValidX, lastValidY, seqId, List.of());
        log.debug("移动完成: from=({},{}) to=({},{})", fromX, fromY, lastValidX, lastValidY);
    }

    private void sendSuccess(PlayerSession session, int finalX, int finalY, Integer seqId, List<Map<String, Object>> events) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", MessageType.MOVE_INTENT);
        body.put("code", 200);
        body.put("status", STATUS_SUCCESS);
        body.put("finalX", finalX);
        body.put("finalY", finalY);
        body.put("events", events != null ? events : List.of());
        if (seqId != null) body.put("seqId", seqId);
        session.sendMessage(body);
    }

    private void sendFail(PlayerSession session, String message, Integer correctX, Integer correctY, Integer seqId) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.MOVE_INTENT);
        fail.put("code", 400);
        fail.put("success", false);
        fail.put("message", message);
        if (correctX != null && correctY != null) {
            fail.put("correctPos", Map.of("x", correctX, "y", correctY));
        }
        if (seqId != null) fail.put("seqId", seqId);
        session.sendMessage(fail);
    }

    private void sendFail(PlayerSession session, String message, int correctX, int correctY, Integer seqId) {
        sendFail(session, message, Integer.valueOf(correctX), Integer.valueOf(correctY), seqId);
    }

    @Override
    public int getMessageType() {
        return MessageType.MOVE_INTENT;
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
