package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.MapPathService;
import com.tower.game.service.MapWalkableService;
import com.tower.game.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动意图处理器：前端只发「点了哪个格子」，后端计算路径/合法性/交互，返回结果。
 * 前端仅缓存结果并播动画。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoveIntentProcessor implements MessageProcessor {

    private static final int DEFAULT_MAP_ID = 1001;
    private static final int EVENT_TYPE_MONSTER = 5;
    private static final int EVENT_TYPE_CHEST = 6;

    private final MapWalkableService mapWalkableService;
    private final MapPathService mapPathService;

    @Override
    public void handle(PlayerSession session, Object message) {
        if (!(message instanceof Map)) {
            sendFail(session, "消息格式错误");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        Integer targetX = getInt(msg, "targetX");
        Integer targetY = getInt(msg, "targetY");
        if (targetX == null || targetY == null) {
            sendFail(session, "缺少 targetX 或 targetY");
            return;
        }
        Integer mapIdParam = getInt(msg, "mapId");
        int mapId = mapIdParam != null ? mapIdParam : DEFAULT_MAP_ID;

        if (!session.hasPosition()) {
            int[] entrance = mapWalkableService.findEntrance(mapId);
            session.setMapId(mapId);
            session.setCellX(entrance[0]);
            session.setCellY(entrance[1]);
        }

        int fromX = session.getCellX();
        int fromY = session.getCellY();
        Integer sessionMapId = session.getMapId();
        if (sessionMapId == null) sessionMapId = DEFAULT_MAP_ID;

        int[] size = mapWalkableService.getMapSize(sessionMapId);
        int width = size[0], height = size[1];
        if (targetX < 0 || targetX >= width || targetY < 0 || targetY >= height) {
            sendFail(session, "目标格超出范围");
            return;
        }

        boolean adjacent = Math.abs(targetX - fromX) + Math.abs(targetY - fromY) == 1;
        int[] cellEvent = mapWalkableService.getCellEvent(sessionMapId, targetX, targetY);

        if (adjacent && cellEvent != null) {
            int eventType = cellEvent[0];
            int eventId = cellEvent[1];
            if (eventType == EVENT_TYPE_MONSTER) {
                sendAction(session, "battle", Map.of("monsterId", eventId, "cellX", targetX, "cellY", targetY));
                return;
            }
            if (eventType == EVENT_TYPE_CHEST) {
                sendAction(session, "chest", Map.of("chestId", eventId, "cellX", targetX, "cellY", targetY));
                return;
            }
        }

        if (targetX == fromX && targetY == fromY) {
            sendMove(session, fromX, fromY, List.of());
            return;
        }

        List<int[]> path = mapPathService.findPath(sessionMapId, fromX, fromY, targetX, targetY);
        if (path.isEmpty()) {
            sendFail(session, "无法到达目标格");
            return;
        }

        int[] last = path.get(path.size() - 1);
        session.setCellX(last[0]);
        session.setCellY(last[1]);
        sendMove(session, fromX, fromY, path);
    }

    private void sendMove(PlayerSession session, int fromX, int fromY, List<int[]> path) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", MessageType.MOVE_INTENT);
        body.put("code", 200);
        body.put("action", "move");
        body.put("fromX", fromX);
        body.put("fromY", fromY);
        List<Map<String, Object>> pathObjs = new ArrayList<>();
        for (int[] p : path) pathObjs.add(Map.of("x", p[0], "y", p[1]));
        body.put("path", pathObjs);
        session.sendMessage(body);
        log.debug("移动意图: 路径 from=({},{}) steps数量={}, path{}", fromX, fromY, path.size(), JsonUtil.toJsonString(path));
    }

    private void sendAction(PlayerSession session, String action, Map<String, Object> extra) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", MessageType.MOVE_INTENT);
        body.put("code", 200);
        body.put("action", action);
        body.putAll(extra);
        session.sendMessage(body);
    }

    private void sendFail(PlayerSession session, String message) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.MOVE_INTENT);
        fail.put("code", 400);
        fail.put("message", message);
        session.sendMessage(fail);
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
