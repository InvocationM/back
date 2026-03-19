package com.tower.game.common.enums;

/**
 * 背包移动方向
 */
public enum BackpackMoveType {
    /** 地图缓存 → 背包 */
    MAP_TO_BACKPACK,
    /** 背包 → 背包 */
    BACKPACK_TO_BACKPACK,
    /** 背包 → 地图缓存 */
    BACKPACK_TO_MAP
}
