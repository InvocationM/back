package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.bigmap.BigMapRunState;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.BigMapRunRedisService;
import com.tower.game.service.MapLootCacheService;
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
public class BigMapUseExitProcessor implements MessageProcessor {

    private static final int EVENT_TYPE_EXIT = 4;
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final MapWalkableService mapWalkableService;
    private final SessionMapRedisService sessionMapRedisService;
    private final BigMapRunRedisService bigMapRunRedisService;
    private final MapLootCacheService mapLootCacheService;

    @Override
    public void handle(PlayerSession session, Object message) {
        Integer seqId = getSeqId(message);
        if (!session.hasPosition() || session.getMapId() == null) {
            sendFail(session, "未在有效地图位置", seqId);
            return;
        }

        BigMapRunState run = bigMapRunRedisService.getRun(session.getUserId()).orElse(null);
        if (run == null || run.getLayerMapIds() == null || run.getLayerMapIds().isEmpty()) {
            sendFail(session, "未开始大章节，请先 POST /api/big-map/start", seqId);
            return;
        }

        int index = run.getLayerIndex();
        if (index < 0 || index >= run.getLayerMapIds().size()) {
            sendFail(session, "章节进度异常", seqId);
            return;
        }

        int currentMapId = session.getMapId();
        if (currentMapId != run.getLayerMapIds().get(index)) {
            sendFail(session, "当前地图与章节进度不一致", seqId);
            return;
        }

        String currentJson = requireMapJson(session.getUserId(), currentMapId);
        int[] event = mapWalkableService.getCellEvent(currentMapId, session.getCellX(), session.getCellY(), currentJson);
        if (event == null || event[0] != EVENT_TYPE_EXIT) {
            sendFail(session, "当前格子不是出口", seqId);
            return;
        }

        if (index + 1 >= run.getLayerMapIds().size()) {
            sendFail(session, "已经是最后一层", seqId);
            return;
        }

        int nextMapId = run.getLayerMapIds().get(index + 1);
        String nextJson = sessionMapRedisService.getMapJson(session.getUserId(), nextMapId);
        if (nextJson == null || nextJson.isBlank()) {
            sendFail(session, "请先通过 POST /api/map/" + nextMapId + " 加载下一层地图", seqId);
            return;
        }

        sessionMapRedisService.deleteMapJson(session.getUserId(), currentMapId);
        mapLootCacheService.clearAll(session.getUserId());
        int[] entrance = mapWalkableService.findEntrance(nextMapId, nextJson);
        session.setMapId(nextMapId);
        session.setCellX(entrance[0]);
        session.setCellY(entrance[1]);

        run.setLayerIndex(index + 1);
        run.setCurrentMapId(nextMapId);
        run.setCellX(entrance[0]);
        run.setCellY(entrance[1]);
        run.setHp(session.getHp());
        bigMapRunRedisService.saveRun(session.getUserId(), run);

        sendSuccess(session, nextMapId, entrance[0], entrance[1], seqId);
        log.debug("出口进层: userId={} -> mapId={} layerIndex={}", session.getUserId(), nextMapId, run.getLayerIndex());
    }

    private String requireMapJson(Long userId, int mapId) {
        String json = sessionMapRedisService.getMapJson(userId, mapId);
        if (json == null || json.isBlank()) {
            throw new BusinessException(500, "地图缓存不存在，请先通过地图接口加载 mapId=" + mapId);
        }
        return json;
    }

    private void sendSuccess(PlayerSession session, int mapId, int finalX, int finalY, Integer seqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", MessageType.BIG_MAP_USE_EXIT);
        body.put("code", 200);
        body.put("status", STATUS_SUCCESS);
        body.put("mapId", mapId);
        body.put("finalX", finalX);
        body.put("finalY", finalY);
        body.put("events", List.of());
        if (seqId != null) body.put("seqId", seqId);
        session.sendMessage(body);
    }

    private void sendFail(PlayerSession session, String msg, Integer seqId) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.BIG_MAP_USE_EXIT);
        fail.put("code", 400);
        fail.put("success", false);
        fail.put("message", msg);
        if (seqId != null) fail.put("seqId", seqId);
        session.sendMessage(fail);
    }

    private static Integer getSeqId(Object message) {
        if (!(message instanceof Map)) return null;
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        return getInt(msg, "seqId");
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

    @Override
    public int getMessageType() {
        return MessageType.BIG_MAP_USE_EXIT;
    }
}
