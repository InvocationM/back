package com.tower.game.service;

import com.tower.game.mapper.ChestMapper;
import com.tower.game.model.entity.Chest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 宝箱配置服务
 */
@Service
@RequiredArgsConstructor
public class ChestService {

    private final ChestMapper chestMapper;

    /**
     * 根据 id 列表批量查询宝箱
     */
    public List<Chest> listByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return chestMapper.selectBatchIds(ids);
    }

    public Chest getById(Integer id) {
        if (id == null) {
            return null;
        }
        return chestMapper.selectById(id);
    }
}
