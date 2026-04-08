package com.tower.game.server.session;

import com.tower.game.common.dto.map.MapCachedItem;
import com.tower.game.common.dto.map.MapLootCache;
import com.tower.game.common.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可序列化的玩家会话状态（用于内存/后续 Redis 等存储，与连接解耦）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionState implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private Long userId;
    private String username;
    private GameStatus gameStatus;

    /** 当前地图 ID，未进图为 null */
    private Integer mapId;
    /** 当前格子 X，未进图为 -1 */
    private int cellX;
    /** 当前格子 Y，未进图为 -1 */
    private int cellY;

    /** 当前地图缓存：与 mapId 一致时有效，为当前地图 JSON（GameMap.data） */
    private Integer currentMapId;
    private String currentMapData;

    private long loginTime;
    private long lastActiveTime;

    /** 当前血量 */
    private int hp;
    /** 最大血量 */
    private int maxHp;

    /** 战斗属性（方案 B 默认值，与境界表一致时 10:1 为百分比） */
    private int attack;
    private int defence;
    private int dodge;
    private int accurate;
    private int crit;
    private int doublehit;
    private int reflect;
    private String name;
    private String icon;

    /** 地图上的物品缓存（尸体/宝箱），key = mapCacheId */
    @Builder.Default
    private Map<String, MapLootCache> mapLootCaches = new LinkedHashMap<>();
    /** mapCacheId 自增序列 */
    private long mapCacheIdSeq;

    /**
     * 是否已设置地图位置（已进图）
     */
    public boolean hasPosition() {
        return cellX >= 0 && cellY >= 0;
    }

    /**
     * 当前会话是否已有与 mapId 对应的地图数据缓存
     */
    public boolean hasCurrentMapDataFor(Integer mapId) {
        return mapId != null && mapId.equals(currentMapId) && currentMapData != null && !currentMapData.isBlank();
    }

    /** 清空当前地图缓存（换图时调用） */
    public void clearCurrentMapData() {
        this.currentMapId = null;
        this.currentMapData = null;
    }

    /** 清空地图掉落/宝箱缓存（换层时调用） */
    public void clearMapLootCaches() {
        if (mapLootCaches != null) {
            mapLootCaches.clear();
        }
        this.mapCacheIdSeq = 0;
    }

    // ==================== 地图物品缓存操作 ====================

    public String nextMapCacheId() {
        return "loot_" + (++mapCacheIdSeq);
    }

    public String nextCachedItemId(String mapCacheId, int index) {
        return mapCacheId + "_" + index;
    }

    public void addLootCache(MapLootCache cache) {
        if (mapLootCaches == null) mapLootCaches = new LinkedHashMap<>();
        mapLootCaches.put(cache.getMapCacheId(), cache);
    }

    public MapLootCache getLootCache(String mapCacheId) {
        return mapLootCaches == null ? null : mapLootCaches.get(mapCacheId);
    }

    /** 在所有缓存中查找单个物品 */
    public MapCachedItem findCachedItem(String cachedItemId) {
        if (mapLootCaches == null) return null;
        for (MapLootCache cache : mapLootCaches.values()) {
            for (MapCachedItem item : cache.getItems()) {
                if (item.getCachedItemId().equals(cachedItemId)) return item;
            }
        }
        return null;
    }

    /** 移除单个缓存物品，items 为空时移除整个 lootCache */
    public MapCachedItem removeCachedItem(String cachedItemId) {
        if (mapLootCaches == null) return null;
        Iterator<Map.Entry<String, MapLootCache>> it = mapLootCaches.entrySet().iterator();
        while (it.hasNext()) {
            MapLootCache cache = it.next().getValue();
            Iterator<MapCachedItem> itemIt = cache.getItems().iterator();
            while (itemIt.hasNext()) {
                MapCachedItem item = itemIt.next();
                if (item.getCachedItemId().equals(cachedItemId)) {
                    itemIt.remove();
                    if (cache.getItems().isEmpty()) it.remove();
                    return item;
                }
            }
        }
        return null;
    }

    /** 背包丢回地图：在玩家当前位置创建或追加到已有缓存 */
    public String addItemToMap(int itemId, int count) {
        if (mapLootCaches == null) mapLootCaches = new LinkedHashMap<>();
        // 查找当前位置已有的 DROP 类型缓存
        for (MapLootCache cache : mapLootCaches.values()) {
            if (cache.getCellX() == cellX && cache.getCellY() == cellY && "DROP".equals(cache.getSourceType())) {
                String cachedItemId = cache.getMapCacheId() + "_" + cache.getItems().size();
                cache.getItems().add(new MapCachedItem(cachedItemId, itemId, count));
                return cachedItemId;
            }
        }
        // 新建
        String mapCacheId = nextMapCacheId();
        MapLootCache cache = new MapLootCache();
        cache.setMapCacheId(mapCacheId);
        cache.setCellX(cellX);
        cache.setCellY(cellY);
        cache.setSourceType("DROP");
        cache.setItems(new ArrayList<>());
        String cachedItemId = mapCacheId + "_0";
        cache.getItems().add(new MapCachedItem(cachedItemId, itemId, count));
        mapLootCaches.put(mapCacheId, cache);
        return cachedItemId;
    }
}
