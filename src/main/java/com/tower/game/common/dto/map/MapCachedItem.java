package com.tower.game.common.dto.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 地图缓存中的单个物品
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapCachedItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 缓存内唯一ID，如 "loot_1_0" */
    private String cachedItemId;
    private int itemId;
    private int count;
}
