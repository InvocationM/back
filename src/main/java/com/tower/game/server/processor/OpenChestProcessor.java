package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.map.MapLootCache;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.model.entity.Chest;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.ChestService;
import com.tower.game.service.MapJsonCellService;
import com.tower.game.service.MapLootCacheService;
import com.tower.game.service.MapWalkableService;
import com.tower.game.service.SessionMapRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenChestProcessor implements MessageProcessor {

    private static final int EVENT_TYPE_CHEST = 6;
    private static final String SOURCE_TYPE_CHEST = "CHEST";

    private final MapWalkableService mapWalkableService;
    private final MapJsonCellService mapJsonCellService;
    private final SessionMapRedisService sessionMapRedisService;
    private final MapLootCacheService mapLootCacheService;
    private final ChestService chestService;

    @Override
    public void handle(PlayerSession session, Object message) {
        if (!(message instanceof Map)) {
            sendFail(session, "invalid message", null);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        Integer chestId = getInt(msg, "chestId");
        Integer cellX = getInt(msg, "cellX");
        Integer cellY = getInt(msg, "cellY");
        Integer seqId = getInt(msg, "seqId");
        if (chestId == null || cellX == null || cellY == null) {
            sendFail(session, "missing chestId or cellX/cellY", seqId);
            return;
        }
        if (session.getMapId() == null || !session.hasPosition()) {
            sendFail(session, "invalid map position", seqId);
            return;
        }
        if (Math.abs(session.getCellX() - cellX) + Math.abs(session.getCellY() - cellY) != 1) {
            sendFail(session, "not adjacent to chest", seqId);
            return;
        }

        String mapData = sessionMapRedisService.getMapJson(session.getUserId(), session.getMapId());
        if (mapData == null || mapData.isBlank()) {
            throw new BusinessException(500, "map cache missing, load map first");
        }
        int[] event = mapWalkableService.getCellEvent(session.getMapId(), cellX, cellY, mapData);
        if (event == null || event[0] != EVENT_TYPE_CHEST || event[1] != chestId) {
            sendFail(session, "cell does not contain this chest", seqId);
            return;
        }

        Chest chest = chestService.getById(chestId);
        if (chest == null) {
            sendFail(session, "chest config not found", seqId);
            return;
        }
        if (chest.getRewards() == null || chest.getRewards().isBlank()) {
            sendFail(session, "chest rewards are empty", seqId);
            return;
        }

        MapLootCache lootCache = new MapLootCache();
        lootCache.setCellX(cellX);
        lootCache.setCellY(cellY);
        lootCache.setSourceType(SOURCE_TYPE_CHEST);
        lootCache.setSourceId(chestId);
        lootCache.setPendingItemConfig(chest.getRewards());
        String mapCacheId = mapLootCacheService.createLoot(session.getUserId(), lootCache);

        var cleared = mapJsonCellService.clearEventFromCell(mapData, cellX, cellY, EVENT_TYPE_CHEST, chestId);
        sessionMapRedisService.saveMapJson(session.getUserId(), session.getMapId(), cleared.newMapJson());

        sendSuccess(session, mapCacheId, cellX, cellY, seqId);
    }

    @Override
    public int getMessageType() {
        return MessageType.OPEN_CHEST;
    }

    private void sendSuccess(PlayerSession session, String mapCacheId, int cellX, int cellY, Integer seqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", MessageType.OPEN_CHEST);
        body.put("code", 200);
        body.put("mapCacheId", mapCacheId);
        body.put("cellX", cellX);
        body.put("cellY", cellY);
        body.put("sourceType", SOURCE_TYPE_CHEST);
        if (seqId != null) body.put("seqId", seqId);
        session.sendMessage(body);
    }

    private void sendFail(PlayerSession session, String message, Integer seqId) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.OPEN_CHEST);
        fail.put("code", 400);
        fail.put("message", message);
        if (seqId != null) fail.put("seqId", seqId);
        session.sendMessage(fail);
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
