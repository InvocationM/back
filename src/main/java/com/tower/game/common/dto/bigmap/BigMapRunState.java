package com.tower.game.common.dto.bigmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BigMapRunState {

    private String runId;
    private Integer bigMapId;
    private int layerIndex;
    private Integer currentMapId;
    private Integer cellX;
    private Integer cellY;
    private Integer hp;

    @Builder.Default
    private List<Integer> layerMapIds = new ArrayList<>();
}
