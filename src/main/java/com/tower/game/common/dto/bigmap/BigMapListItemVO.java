package com.tower.game.common.dto.bigmap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大章节列表单项（只读元数据，不含随机路线）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigMapListItemVO {
    private Integer id;
    private String name;
    /** 配置的层数（big_map_layer 行数） */
    private int layerCount;
}
