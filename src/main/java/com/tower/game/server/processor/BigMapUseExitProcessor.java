package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.bigmap.BigMapRunState;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.BigMapRunRedisService;
import com.tower.game.service.MapWalkableService;
import com.tower.game.service.SessionMapRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家站在出口格（type=4）时进入大章节下一层：推进 layerIndex、切换 mapId、落新图入口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BigMapUseExitProcessor implements MessageProcessor {

    private static final int EVENT_TYPE_EXIT = 4;
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final MapWalkableService mapWalkableService;
    private final SessionMapRedisService sessionMapRedisService;
    private final BigMapRunRedisService bigMapRunRedisService;

    @Override
    public void handle(PlayerSession session, Object message) {
        Integer seqId = getSeqId(message);
        if (!session.hasPosition()) {
            sendFail(session, "未进图，无法使用出口", seqId);
            return;
        }
        Integer sessionMapId = session.getMapId();
        if (sessionMapId == null) {
            sendFail(session, "当前无地图", seqId);
            return;
        }

        BigMapRunState run = bigMapRunRedisService.getRun(session.getUserId()).orElse(null);
        if (run == null || run.getLayerMapIds() == null || run.getLayerMapIds().isEmpty()) {
            sendFail(session, "未开始大章节，请先 POST /api/big-map/start", seqId);
            return;
        }

        int idx = run.getLayerIndex();
        if (idx < 0 || idx >= run.getLayerMapIds().size()) {
            sendFail(session, "章节进度异常", seqId);
            return;
        }
        if (!sessionMapId.equals(run.getLayerMapIds().get(idx))) {
            sendFail(session, "当前地图与章节进度不一致", seqId);
            return;
        }

        ensureSessionMapLoaded(session, sessionMapId);
        String mapData = session.getCurrentMapData();
        int[] ev = mapWalkableService.getCellEvent(sessionMapId, session.getCellX(), session.getCellY(), mapData);
        if (ev == null || ev[0] != EVENT_TYPE_EXIT) {
            sendFail(session, "当前格子不是出口", seqId);
            return;
        }

        if (idx + 1 >= run.getLayerMapIds().size()) {
            sendFail(session, "已是最后一层", seqId);
            return;
        }

        int nextMapId = run.getLayerMapIds().get(idx + 1);
        String nextJson = sessionMapRedisService.getMapJson(session.getUserId(), nextMapId);
        if (nextJson == null || nextJson.isBlank()) {
            sendFail(session, "请先通过 POST /api/map/" + nextMapId + " 加载下一层地图", seqId);
            return;
        }

        sessionMapRedisService.deleteMapJson(session.getUserId(), sessionMapId);
        run.setLayerIndex(idx + 1);
        bigMapRunRedisService.saveRun(session.getUserId(), run);

        session.clearMapLootCaches();
        session.setMapId(nextMapId);
        session.setCurrentMapData(nextMapId, nextJson);
        int[] entrance = mapWalkableService.findEntrance(nextMapId, nextJson);
        session.setCellX(entrance[0]);
        session.setCellY(entrance[1]);

        sendSuccess(session, nextMapId, entrance[0], entrance[1], seqId);
        log.debug("出口进层: userId={} -> mapId={} layerIndex={}", session.getUserId(), nextMapId, run.getLayerIndex());
    }

    private void ensureSessionMapLoaded(PlayerSession session, int mapId) {
        if (session.hasCurrentMapDataFor(mapId)) {
            return;
        }
        String json = sessionMapRedisService.getMapJson(session.getUserId(), mapId);
        if (json != null && !json.isBlank()) {
            session.setCurrentMapData(mapId, json);
            return;
        }
        throw new com.tower.game.common.exception.BusinessException(500, "地图缓存不存在，请先通过地图接口加载 mapId=" + mapId);
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
        if (seqId != null) {
            body.put("seqId", seqId);
        }
        session.sendMessage(body);
    }

    private void sendFail(PlayerSession session, String msg, Integer seqId) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.BIG_MAP_USE_EXIT);
        fail.put("code", 400);
        fail.put("success", false);
        fail.put("message", msg);
        if (seqId != null) {
            fail.put("seqId", seqId);
        }
        session.sendMessage(fail);
    }

    private static Integer getSeqId(Object message) {
        if (!(message instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        return getInt(msg, "seqId");
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public int getMessageType() {
        return MessageType.BIG_MAP_USE_EXIT;
    }
}
