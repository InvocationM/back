package com.tower.game.common.dto.bigmap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大章节列表出参：{ "bigMaps": [ ... ] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BigMapListResponse {
    private List<BigMapListItemVO> bigMaps;
}
