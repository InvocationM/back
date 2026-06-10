package com.tower.game.service;

import com.tower.game.common.exception.BusinessException;
import com.tower.game.model.entity.Item;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (session == null) throw new BusinessException("玩家未在线");
        pickupFromMapCell(session.getUserId(), session.authoritativeState(), cellX, cellY);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pickupFromMapCell(Long playerId, SessionState state, int cellX, int cellY) {
        if (playerId == null) throw new BusinessException("用户无效");
        if (state == null || state.getMapId() == null) throw new BusinessException("未在地图中");
        if (!state.hasPosition()) throw new BusinessException("未在有效地图位置");

        int mapId = state.getMapId();
        synchronized (pickupLock(playerId, mapId, cellX, cellY)) {
            pickupFromMapCellLocked(playerId, state, mapId, cellX, cellY);
        }
    }

    private void pickupFromMapCellLocked(Long playerId, SessionState state, int mapId, int cellX, int cellY) {
        String mapData = requireMapJson(playerId, mapId);

        int px = state.getCellX();
        int py = state.getCellY();
        if (Math.abs(px - cellX) + Math.abs(py - cellY) > 1) {
            throw new BusinessException("不在目标格相邻或同一格");
        }

        int[] size = mapWalkableService.getMapSize(mapId, mapData);
        if (cellX < 0 || cellX >= size[0] || cellY < 0 || cellY >= size[1]) {
            throw new BusinessException("目标格超出地图范围");
        }

        int[] event = mapWalkableService.getCellEvent(mapId, cellX, cellY, mapData);
        if (event == null) {
            throw new BusinessException("该格没有可拾取物品");
        }
        int mapEventType = event[0];
        if (mapEventType != MapWalkableService.EVENT_TYPE_KEY
                && mapEventType != MapWalkableService.EVENT_TYPE_BLOOD_POTION) {
            throw new BusinessException("该格没有可拾取物品");
        }

        MapJsonCellService.MapCellPickupResult parsed =
                mapJsonCellService.pickupMapItemFromCell(mapData, cellX, cellY, mapEventType);

        Item item = itemService.getById(parsed.itemId());
        if (item == null) throw new BusinessException("物品不存在");

        int expectedItemType = mapEventType == MapWalkableService.EVENT_TYPE_KEY
                ? ItemService.ITEM_TYPE_KEY
                : ItemService.ITEM_TYPE_BLOOD_POTION;
        if (item.getType() == null || item.getType() != expectedItemType) {
            throw new BusinessException(mapEventType == MapWalkableService.EVENT_TYPE_KEY
                    ? "地图配置的物品不是钥匙"
                    : "地图配置的物品不是血瓶");
        }

        playerBackpackItemService.autoPlaceInDefaultBackpack(playerId, item, parsed.count());
        sessionMapRedisService.saveMapJson(playerId, mapId, parsed.newMapJson());
    }

    private Object pickupLock(Long playerId, int mapId, int cellX, int cellY) {
        return ("map-pickup:" + playerId + ":" + mapId + ":" + cellX + ":" + cellY).intern();
    }

    private String requireMapJson(Long playerId, int mapId) {
        String json = sessionMapRedisService.getMapJson(playerId, mapId);
        if (json == null || json.isBlank()) {
            throw new BusinessException(500, "地图缓存不存在，请先通过地图接口加载 mapId=" + mapId);
        }
        return json;
    }
}
