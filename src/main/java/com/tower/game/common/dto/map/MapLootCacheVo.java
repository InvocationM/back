package com.tower.game.common.dto.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 开箱响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapLootCacheVo {
    private String mapCacheId;
    private String sourceType;
    private List<MapCachedItemVo> items;
}
