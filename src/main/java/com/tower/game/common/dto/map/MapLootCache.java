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
    /** "CORPSE" 或 "CHEST" */
    private String sourceType;
    /** monsterId 或 chestId */
    private Integer sourceId;
    private List<MapCachedItem> items = new ArrayList<>();
}
