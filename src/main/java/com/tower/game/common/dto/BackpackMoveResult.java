package com.tower.game.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackpackMoveResult {
    private boolean mapLootCleared;
    private String mapCacheId;
    private int cellX = -1;
    private int cellY = -1;
    private String cachedItemId;
    private String sourceType;

    public static BackpackMoveResult none() {
        return new BackpackMoveResult(false, null, -1, -1, null, null);
    }

    public static BackpackMoveResult mapLootCleared(String mapCacheId, int cellX, int cellY) {
        return new BackpackMoveResult(true, mapCacheId, cellX, cellY, null, null);
    }

    public static BackpackMoveResult mapItemAdded(String mapCacheId, String cachedItemId, int cellX, int cellY, String sourceType) {
        return new BackpackMoveResult(false, mapCacheId, cellX, cellY, cachedItemId, sourceType);
    }
}
