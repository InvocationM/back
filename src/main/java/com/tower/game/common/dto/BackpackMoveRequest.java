package com.tower.game.common.dto;

import com.tower.game.common.enums.BackpackMoveType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 背包统一移动请求
 * <p>
 * 三种方向：
 * 1. MAP_TO_BACKPACK：cachedItemId + slotIndex/gridRow/gridCol
 * 2. BACKPACK_TO_BACKPACK：placementId + slotIndex/gridRow/gridCol
 * 3. BACKPACK_TO_MAP：placementId（不传背包坐标）
 */
@Data
public class BackpackMoveRequest {

    @NotNull(message = "移动类型不能为空")
    private BackpackMoveType moveType;

    /** 地图缓存物品ID（MAP_TO_BACKPACK 时必传） */
    private String cachedItemId;

    /** 背包放置记录ID（BACKPACK_TO_BACKPACK / BACKPACK_TO_MAP 时必传） */
    private Long placementId;

    /** 目标背包位（MAP_TO_BACKPACK / BACKPACK_TO_BACKPACK 时必传） */
    private Integer slotIndex;

    /** 目标行（MAP_TO_BACKPACK / BACKPACK_TO_BACKPACK 时必传） */
    private Integer gridRow;

    /** 目标列（MAP_TO_BACKPACK / BACKPACK_TO_BACKPACK 时必传） */
    private Integer gridCol;
}
