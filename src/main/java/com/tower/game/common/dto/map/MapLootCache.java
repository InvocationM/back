package com.tower.game.common.dto.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图上一个尸体/宝箱对应的物品缓存
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapLootCache implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 唯一ID，如 "loot_1" */
    private String mapCacheId;
    private int cellX;
    private int cellY;
    /**
     * "CORPSE" 尸体、"CHEST" 宝箱表现。
     * 战斗胜利地图缓存为 CHEST，此时 sourceId 为击败的 monsterId（非配置表 chestId）。
     */
    private String sourceType;
    /** 怪物尸体为 monsterId；地图事件格宝箱为 chest 配置 id */
    private Integer sourceId;
    private List<MapCachedItem> items = new ArrayList<>();
}
