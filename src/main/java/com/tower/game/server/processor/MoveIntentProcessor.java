package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.bigmap.BigMapRunState;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.BigMapRunRedisService;
import com.tower.game.service.MapPathService;
import com.tower.game.service.MapWalkableService;
import com.tower.game.service.SessionMapRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoveIntentProcessor implements MessageProcessor {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final MapWalkableService mapWalkableService;
    private final MapPathService mapPathService;
    private final SessionMapRedisService sessionMapRedisService;
    private final BigMapRunRedisService bigMapRunRedisService;

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
        Integer mapIdParam = getInt(msg, "mapId");

        if (targetX == null || targetY == null) {
            sendFail(session, "缺少 targetX 或 targetY", null, null, seqId);
            return;
        }

        BigMapRunState run = bigMapRunRedisService.getRun(session.getUserId()).orElse(null);
        if (run == null || run.getLayerMapIds() == null || run.getLayerMapIds().isEmpty()) {
            sendFail(session, "请先调用 POST /api/big-map/start 开始章节", null, null, seqId);
            return;
        }

        if (!session.hasPosition()) {
            handleFirstEnter(session, run, mapIdParam, seqId);
            return;
        }

        int fromX = session.getCellX();
        int fromY = session.getCellY();
        Integer sessionMapId = session.getMapId();
        if (sessionMapId == null) {
            sendFail(session, "当前无地图", fromX, fromY, seqId);
            return;
        }

        if (mapIdParam != null && !mapIdParam.equals(sessionMapId)) {
            sendFail(session, "切换小地图请使用出口协议 type=5010", fromX, fromY, seqId);
            return;
        }

        String mapData = requireMapJson(session.getUserId(), sessionMapId);
        int[] size = mapWalkableService.getMapSize(sessionMapId, mapData);
        if (targetX < 0 || targetX >= size[0] || targetY < 0 || targetY >= size[1]) {
            sendFail(session, "目标格超出范围", fromX, fromY, seqId);
            return;
        }

        if (targetX == fromX && targetY == fromY) {
            sendSuccess(session, fromX, fromY, seqId);
            return;
        }

        List<int[]> path = mapPathService.findPath(sessionMapId, fromX, fromY, targetX, targetY, mapData);
        if (path.isEmpty()) {
            sendFail(session, "无法到达目标格", fromX, fromY, seqId);
            return;
        }

        int[] lastCell = path.get(path.size() - 1);
        updatePosition(session, run, sessionMapId, lastCell[0], lastCell[1]);
        sendSuccess(session, lastCell[0], lastCell[1], seqId);
        log.debug("移动完成: userId={} mapId={} from=({},{}) to=({},{})",
                session.getUserId(), sessionMapId, fromX, fromY, lastCell[0], lastCell[1]);
    }

    private void handleFirstEnter(PlayerSession session, BigMapRunState run, Integer mapIdParam, Integer seqId) {
        if (mapIdParam == null) {
            sendFail(session, "首次进图请指定 mapId", null, null, seqId);
            return;
        }
        int layerIndex = run.getLayerIndex();
        if (layerIndex < 0 || layerIndex >= run.getLayerMapIds().size()) {
            sendFail(session, "章节进度异常", null, null, seqId);
            return;
        }
        int expectedMapId = run.getLayerMapIds().get(layerIndex);
        if (mapIdParam != expectedMapId) {
            sendFail(session, "mapId 与当前章节层不一致，期望 " + expectedMapId, null, null, seqId);
            return;
        }

        String mapData = requireMapJson(session.getUserId(), mapIdParam);
        int[] entrance = mapWalkableService.findEntrance(mapIdParam, mapData);
        updatePosition(session, run, mapIdParam, entrance[0], entrance[1]);
        sendSuccess(session, entrance[0], entrance[1], seqId);
    }

    private String requireMapJson(Long userId, int mapId) {
        String json = sessionMapRedisService.getMapJson(userId, mapId);
        if (json == null || json.isBlank()) {
            throw new BusinessException(500, "地图缓存不存在，请先通过地图接口加载 mapId=" + mapId);
        }
        return json;
    }

    private void updatePosition(PlayerSession session, BigMapRunState run, int mapId, int cellX, int cellY) {
        session.setMapId(mapId);
        session.setCellX(cellX);
        session.setCellY(cellY);
        run.setCurrentMapId(mapId);
        run.setCellX(cellX);
        run.setCellY(cellY);
        run.setHp(session.getHp());
        bigMapRunRedisService.saveRun(session.getUserId(), run);
    }

    private void sendSuccess(PlayerSession session, int finalX, int finalY, Integer seqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", MessageType.MOVE_INTENT);
        body.put("code", 200);
        body.put("status", STATUS_SUCCESS);
        body.put("finalX", finalX);
        body.put("finalY", finalY);
        body.put("events", List.of());
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

    @Override
    public int getMessageType() {
        return MessageType.MOVE_INTENT;
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
