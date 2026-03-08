package com.tower.game.common.dto.bigmap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大章节列表查询出参，对应 JSON { "bigMaps": [ ... ] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigMapsResponse {
    private List<BigMapVO> bigMaps;
}
