package com.tower.game.service;

import com.tower.game.common.exception.BusinessException;
import com.tower.game.model.entity.Item;
import com.tower.game.server.session.PlayerSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 从地图 JSON 格子拾取血瓶（type=9）并入默认背包，同步会话与 Redis 地图缓存。
 */
@Service
@RequiredArgsConstructor
public class MapPotionPickupService {

    private final SessionMapRedisService sessionMapRedisService;
    private final MapWalkableService mapWalkableService;
    private final MapJsonCellService mapJsonCellService;
    private final PlayerBackpackItemService playerBackpackItemService;
    private final ItemService itemService;

    @Transactional(rollbackFor = Exception.class)
    public void pickupFromMapCell(PlayerSession session, int cellX, int cellY) {
        Long playerId = session.getUserId();
        if (playerId == null) throw new BusinessException("用户无效");
        if (session.getMapId() == null) throw new BusinessException("未在地图中");
        if (!session.hasPosition()) throw new BusinessException("未在有效地图位置");

        int mapId = session.getMapId();
        ensureSessionMapLoaded(session, mapId);
        String mapData = session.getCurrentMapData();

        int px = session.getCellX();
        int py = session.getCellY();
        if (Math.abs(px - cellX) + Math.abs(py - cellY) > 1) {
            throw new BusinessException("不在目标格相邻或同一格");
        }

        int[] size = mapWalkableService.getMapSize(mapId, mapData);
        if (cellX < 0 || cellX >= size[0] || cellY < 0 || cellY >= size[1]) {
            throw new BusinessException("目标格超出地图范围");
        }

        int[] ev = mapWalkableService.getCellEvent(mapId, cellX, cellY, mapData);
        if (ev == null || ev[0] != MapWalkableService.EVENT_TYPE_BLOOD_POTION) {
            throw new BusinessException("该格没有可拾取血瓶");
        }

        MapJsonCellService.BloodPotionPickupResult parsed =
                mapJsonCellService.pickupBloodPotionFromCell(mapData, cellX, cellY);

        Item item = itemService.getById(parsed.itemId());
        if (item == null) throw new BusinessException("物品不存在");
        if (item.getType() == null || item.getType() != ItemService.ITEM_TYPE_BLOOD_POTION) {
            throw new BusinessException("地图配置的物品不是血瓶");
        }

        playerBackpackItemService.autoPlaceInDefaultBackpack(playerId, item, parsed.count());

        session.setCurrentMapData(mapId, parsed.newMapJson());
        sessionMapRedisService.saveMapJson(playerId, mapId, parsed.newMapJson());
    }

    private void ensureSessionMapLoaded(PlayerSession session, int mapId) {
        if (session.hasCurrentMapDataFor(mapId)) return;
        String json = sessionMapRedisService.getMapJson(session.getUserId(), mapId);
        if (json != null && !json.isBlank()) {
            session.setCurrentMapData(mapId, json);
            return;
        }
        throw new BusinessException(500, "地图缓存不存在，请先通过地图接口加载 mapId=" + mapId);
    }
}
