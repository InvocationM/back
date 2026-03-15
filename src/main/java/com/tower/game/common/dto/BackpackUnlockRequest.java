package com.tower.game.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 背包按序解锁请求（暂不实现金币消耗）
 */
@Data
public class BackpackUnlockRequest {

    /** 背包位索引 0~4 */
    @Min(0)
    @Max(4)
    private int slotIndex = 0;
}
