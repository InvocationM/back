package com.tower.game.service;

import com.tower.game.mapper.MonsterMapper;
import com.tower.game.model.entity.Monster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 怪物配置服务
 */
@Service
@RequiredArgsConstructor
public class MonsterService {

    private final MonsterMapper monsterMapper;

    /**
     * 根据 id 列表批量查询怪物
     */
    public List<Monster> listByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return monsterMapper.selectBatchIds(ids);
    }
}
