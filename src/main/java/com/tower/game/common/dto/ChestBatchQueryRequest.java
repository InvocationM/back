package com.tower.game.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 宝箱 id 批量查询请求
 */
@Data
public class ChestBatchQueryRequest {

    /**
     * 宝箱 id 列表，最多 100 个
     */
    @NotEmpty(message = "ids 不能为空")
    @Size(max = 100, message = "单次最多查询 100 个宝箱")
    private List<Integer> ids;
}
