package com.tower.game.common.dto.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存物品VO（开箱时返回给前端，含物品详情）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapCachedItemVo {
    private String cachedItemId;
    private int itemId;
    private String itemName;
    private String itemIcon;
    private Integer shapeType;
    private int count;
}
