package com.tower.game.common.dto.bigmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 大章节闯关进度（存 Redis），与 {@link BigMapVO} 分离。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BigMapRunState {

    private Integer bigMapId;
    /** 当前所在层下标，0-based，对应 layerMapIds 中正在游玩的层 */
    private int layerIndex;
    @Builder.Default
    private List<Integer> layerMapIds = new ArrayList<>();
}
