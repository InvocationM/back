package com.tower.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tower.game.mapper.BackpackUnlockOrderMapper;
import com.tower.game.model.entity.BackpackUnlockOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 背包解锁次序配置服务
 */
@Service
@RequiredArgsConstructor
public class BackpackUnlockOrderService {

    private final BackpackUnlockOrderMapper backpackUnlockOrderMapper;

    /** 最大序号（10×7=70） */
    public static final int MAX_ORDER = 70;

    /**
     * 查询已解锁的格子（order_number <= maxOrder）
     * 返回 (grid_row, grid_col) 的集合，用 "row,col" 表示
     */
    public Set<String> getUnlockedCellKeys(int slotIndex, int maxOrder) {
        LambdaQueryWrapper<BackpackUnlockOrder> q = new LambdaQueryWrapper<>();
        q.eq(BackpackUnlockOrder::getSlotIndex, slotIndex)
                .le(BackpackUnlockOrder::getOrderNumber, maxOrder);
        List<BackpackUnlockOrder> list = backpackUnlockOrderMapper.selectList(q);
        return list.stream()
                .map(o -> o.getGridRow() + "," + o.getGridCol())
                .collect(Collectors.toSet());
    }

    /**
     * 查询下一格待解锁（order_number = maxOrder + 1）
     */
    public BackpackUnlockOrder getNextUnlock(int slotIndex, int maxOrder) {
        if (maxOrder >= MAX_ORDER) return null;
        LambdaQueryWrapper<BackpackUnlockOrder> q = new LambdaQueryWrapper<>();
        q.eq(BackpackUnlockOrder::getSlotIndex, slotIndex)
                .eq(BackpackUnlockOrder::getOrderNumber, maxOrder + 1);
        return backpackUnlockOrderMapper.selectOne(q);
    }

    /**
     * 按序号查单条
     */
    public BackpackUnlockOrder getBySlotAndOrder(int slotIndex, int orderNumber) {
        LambdaQueryWrapper<BackpackUnlockOrder> q = new LambdaQueryWrapper<>();
        q.eq(BackpackUnlockOrder::getSlotIndex, slotIndex)
                .eq(BackpackUnlockOrder::getOrderNumber, orderNumber);
        return backpackUnlockOrderMapper.selectOne(q);
    }
}
