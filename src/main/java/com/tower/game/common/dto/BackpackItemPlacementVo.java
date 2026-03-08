package com.tower.game.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 背包内单个放置项（含物品快照）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackpackItemPlacementVo {

    private Long placementId;
    private int gridRow;
    private int gridCol;
    private int itemId;
    private String itemName;
    private String itemIcon;
    private Integer shapeType;
    private int count;
}
