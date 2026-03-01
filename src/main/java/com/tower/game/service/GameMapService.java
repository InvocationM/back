package com.tower.game.service;

import com.tower.game.mapper.GameMapMapper;
import com.tower.game.model.entity.GameMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 地图服务
 */
@Service
@RequiredArgsConstructor
public class GameMapService {

    private final GameMapMapper gameMapMapper;

    /**
     * 根据 mapId 查询地图
     */
    public GameMap getByMapId(Integer mapId) {
        return gameMapMapper.findByMapId(mapId);
    }
}
