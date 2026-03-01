package com.tower.game.service;

import com.tower.game.mapper.GameMapMapper;
import com.tower.game.model.entity.GameMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 地图服务。返回地图时对每个格子按权重只保留一个事件（服务端权威）。
 */
@Service
@RequiredArgsConstructor
public class GameMapService {

    private final GameMapMapper gameMapMapper;
    private final MapWeightSelector mapWeightSelector;

    /**
     * 根据 mapId 查询地图。返回的 data 中每个格子的 events 已按权重随机选为至多一个事件。
     */
    public GameMap getByMapId(Integer mapId) {
        GameMap map = gameMapMapper.findByMapId(mapId);
        if (map == null || map.getData() == null || map.getData().isBlank()) {
            return map;
        }
        String processed = mapWeightSelector.applyWeightSelectionPerCell(map.getData());
        map.setData(processed);
        return map;
    }
}
