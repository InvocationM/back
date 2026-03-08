package com.tower.game.service;

import com.tower.game.mapper.ItemMapper;
import com.tower.game.model.entity.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 物品配置服务
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;

    /**
     * 根据 id 查询单条物品
     */
    public Item getById(Integer id) {
        return id == null ? null : itemMapper.selectById(id);
    }

    /**
     * 根据 id 列表批量查询物品
     */
    public List<Item> listByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return itemMapper.selectBatchIds(ids);
    }
}
