package com.tower.game.common.dto.bigmap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大章节单层出参，对应 JSON layers[].{ options: [...] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LayerVO {
    /**
     * 该层选项 ID 列表，如 [1001, 1002, 1003]
     */
    private List<Integer> options;
}
