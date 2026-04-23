package com.tower.game.service;

import com.tower.game.common.exception.BusinessException;
import com.tower.game.server.session.PlayerSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话内开门：邻格校验、钥匙、门上 doorId 与请求一致；扣钥匙、更新地图 JSON 并写 Redis。
 */
@Service
@RequiredArgsConstructor
public class MapOpenDoorService {

    private final SessionMapRedisService sessionMapRedisService;
    private final MapWalkableService mapWalkableService;
    private final MapJsonCellService mapJsonCellService;
    private final PlayerBackpackItemService playerBackpackItemService;

    @Transactional(rollbackFor = Exception.class)
    public void openDoor(PlayerSession session, int cellX, int cellY, int doorId) {
        Long playerId = session.getUserId();
        if (playerId == null) {
            throw new BusinessException("用户无效");
        }
        if (session.getMapId() == null) {
            throw new BusinessException("未在地图中");
        }
        if (!session.hasPosition()) {
            throw new BusinessException("未在有效地图位置");
        }
        if (doorId <= 0) {
            throw new BusinessException("doorId 无效");
        }

        int mapId = session.getMapId();
        ensureSessionMapLoaded(session, mapId);
        String mapData = session.getCurrentMapData();

        int px = session.getCellX();
        int py = session.getCellY();
        if (Math.abs(px - cellX) + Math.abs(py - cellY) > 1) {
            throw new BusinessException("不在门格相邻或同一格");
        }

        int[] size = mapWalkableService.getMapSize(mapId, mapData);
        if (cellX < 0 || cellX >= size[0] || cellY < 0 || cellY >= size[1]) {
            throw new BusinessException("目标格超出地图范围");
        }

        int[] ev = mapWalkableService.getCellEvent(mapId, cellX, cellY, mapData);
        if (ev == null || ev[0] != MapWalkableService.EVENT_TYPE_DOOR) {
            throw new BusinessException("该格没有门");
        }
        if (ev[1] != doorId) {
            throw new BusinessException("门上编号与请求 doorId 不一致");
        }

        playerBackpackItemService.consumeOneKeyOpeningDoor(playerId, doorId);

        MapJsonCellService.MapDoorOpenResult parsed =
                mapJsonCellService.openDoorFromCell(mapData, cellX, cellY, doorId);

        session.setCurrentMapData(mapId, parsed.newMapJson());
        sessionMapRedisService.saveMapJson(playerId, mapId, parsed.newMapJson());
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
        throw new BusinessException(500, "地图缓存不存在，请先通过地图接口加载 mapId=" + mapId);
    }
}
