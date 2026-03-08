package com.tower.game.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 背包放入请求
 */
@Data
public class BackpackPutRequest {

    @NotNull(message = "玩家ID不能为空")
    private Long playerId;

    @Min(0) @Max(4)
    private int slotIndex = 0;

    @Min(0) @Max(9)
    private int gridRow;

    @Min(0) @Max(6)
    private int gridCol;

    @NotNull(message = "物品ID不能为空")
    private Integer itemId;

    @Min(1)
    private int count = 1;
}
