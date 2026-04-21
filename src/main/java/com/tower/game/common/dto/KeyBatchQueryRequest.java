package com.tower.game.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 钥匙物品 id 批量查询请求
 */
@Data
public class KeyBatchQueryRequest {

    /**
     * 物品 id 列表（仅返回 type=7 的钥匙），最多 100 个
     */
    @NotEmpty(message = "ids 不能为空")
    @Size(max = 100, message = "单次最多查询 100 个物品")
    private List<Integer> ids;
}
