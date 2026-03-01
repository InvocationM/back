package com.tower.game.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 物品 id 批量查询请求
 */
@Data
public class ItemBatchQueryRequest {

    /**
     * 物品 id 列表，最多 100 个
     */
    @NotEmpty(message = "ids 不能为空")
    @Size(max = 100, message = "单次最多查询 100 个物品")
    private List<Integer> ids;
}
