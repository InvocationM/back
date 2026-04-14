package com.tower.game.service;

import com.tower.game.mapper.ItemMapper;
import com.tower.game.model.entity.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    /** 物品 type：血瓶（与 item 表、{@link Item} 注释一致） */
    public static final int ITEM_TYPE_BLOOD_POTION = 9;

    /**
     * 按 id 批量查询，仅返回 type=血瓶 的物品；非血瓶 id 不会出现在结果中。
     */
    public List<Item> listBloodPotionsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return listByIds(ids).stream()
                .filter(item -> item.getType() != null && item.getType() == ITEM_TYPE_BLOOD_POTION)
                .collect(Collectors.toList());
    }
}
