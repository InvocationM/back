package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.model.entity.GameMap;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.GameMapService;
import com.tower.game.service.MapPathService;
import com.tower.game.service.MapWalkableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动意图处理器：客户端发 targetX/targetY/seqId，服务端校验可达性、按步模拟路径，
 * 遇怪物/宝箱即返回 INTERRUPTED；无中断则返回 SUCCESS 并更新权威坐标。
 * 客户端负责乐观预测与回滚（拉回 correctPos）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoveIntentProcessor implements MessageProcessor {

    private static final int DEFAULT_MAP_ID = 1001;
    private static final int EVENT_TYPE_MONSTER = 5;
    private static final int EVENT_TYPE_CHEST = 6;

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_INTERRUPTED = "INTERRUPTED";

    private final MapWalkableService mapWalkableService;
    private final MapPathService mapPathService;
    private final GameMapService gameMapService;

    /** 确保 Session 中已加载该 mapId 的地图数据（用于复用，避免同请求内重复查库） */
    private void ensureSessionMapLoaded(PlayerSession session, int mapId) {
        if (session.hasCurrentMapDataFor(mapId)) return;
        GameMap map = gameMapService.getByMapId(mapId);
        if (map != null && map.getData() != null && !map.getData().isBlank()) {
            session.setCurrentMapData(mapId, map.getData());
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

        int lastValidX = fromX;
        int lastValidY = fromY;
        for (int i = 0; i < path.size(); i++) {
            int[] cell = path.get(i);
            int cx = cell[0], cy = cell[1];
            int[] cellEvent = mapWalkableService.getCellEvent(sessionMapId, cx, cy, mapData);
            if (cellEvent != null) {
                int eventType = cellEvent[0];
                int eventId = cellEvent[1];
                if (eventType == EVENT_TYPE_MONSTER) {
                    List<int[]> remainingPath = i + 1 < path.size()
                            ? path.subList(i + 1, path.size())
                            : List.of();
                    sendInterrupted(session, lastValidX, lastValidY, "COMBAT", eventId, cx, cy, true, remainingPath, seqId);
                    log.debug("移动中断: 遇怪 from=({},{}) eventCell=({},{}) monsterId={}", lastValidX, lastValidY, cx, cy, eventId);
                    return;
                }
                if (eventType == EVENT_TYPE_CHEST) {
                    List<int[]> remainingPath = i + 1 < path.size()
                            ? path.subList(i + 1, path.size())
                            : List.of();
                    sendInterrupted(session, lastValidX, lastValidY, "CHEST", eventId, cx, cy, true, remainingPath, seqId);
                    log.debug("移动中断: 遇宝箱 from=({},{}) eventCell=({},{}) chestId={}", lastValidX, lastValidY, cx, cy, eventId);
                    return;
                }
            }
            lastValidX = cx;
            lastValidY = cy;
        }

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

    private void sendInterrupted(PlayerSession session, int currentX, int currentY, String eventType, int eventId,
                                 int cellX, int cellY, boolean autoContinue, List<int[]> remainingPath, Integer seqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", MessageType.MOVE_INTENT);
        body.put("code", 200);
        body.put("status", STATUS_INTERRUPTED);
        body.put("currentX", currentX);
        body.put("currentY", currentY);
        Map<String, Object> event = new HashMap<>();
        event.put("type", eventType);
        if ("COMBAT".equals(eventType)) {
            event.put("monsterId", eventId);
        } else if ("CHEST".equals(eventType)) {
            event.put("chestId", eventId);
        }
        event.put("cellX", cellX);
        event.put("cellY", cellY);
        body.put("event", event);
        body.put("autoContinue", autoContinue);
        List<Map<String, Object>> remaining = new ArrayList<>();
        for (int[] p : remainingPath) remaining.add(Map.of("x", p[0], "y", p[1]));
        body.put("remainingPath", remaining);
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
