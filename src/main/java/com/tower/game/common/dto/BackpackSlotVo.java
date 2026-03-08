package com.tower.game.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个背包位视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackpackSlotVo {

    private int slotIndex;
    private int maxUnlockedOrder;
    /** 下一格待解锁序号，无则 null */
    private Integer nextOrderNumber;
    /** 下一格坐标 [row, col]，无则 null */
    private int[] nextCell;
    private boolean maxUnlocked;
    private List<BackpackItemPlacementVo> items;
}
