package com.tower.game.common.dto.bigmap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大章节出参，对应 JSON bigMaps[]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigMapVO {
    private Integer id;
    private String name;
    /**
     * 层列表，每项为 { options: [...] }
     */
    private List<LayerVO> layers;
}
