package com.tower.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.mapper.PlayerBackpackSlotMapper;
import com.tower.game.model.entity.BackpackUnlockOrder;
import com.tower.game.model.entity.PlayerBackpackSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 玩家背包位状态服务
 */
@Service
@RequiredArgsConstructor
public class PlayerBackpackSlotService {

    private final PlayerBackpackSlotMapper playerBackpackSlotMapper;
    private final BackpackUnlockOrderService backpackUnlockOrderService;

    /** 默认背包位（slot 0）初始已解锁序号（4×4=16） */
    public static final int DEFAULT_INITIAL_UNLOCK_ORDER = 16;

    /**
     * 获取或创建玩家某背包位状态；不存在则创建（slot 0 初始 16，其余 0）
     */
    public PlayerBackpackSlot getOrCreate(Long playerId, int slotIndex) {
        LambdaQueryWrapper<PlayerBackpackSlot> q = new LambdaQueryWrapper<>();
        q.eq(PlayerBackpackSlot::getPlayerId, playerId).eq(PlayerBackpackSlot::getSlotIndex, slotIndex);
        PlayerBackpackSlot slot = playerBackpackSlotMapper.selectOne(q);
        if (slot != null) return slot;
        slot = new PlayerBackpackSlot();
        slot.setPlayerId(playerId);
        slot.setSlotIndex(slotIndex);
        slot.setMaxUnlockedOrder(slotIndex == 0 ? DEFAULT_INITIAL_UNLOCK_ORDER : 0);
        playerBackpackSlotMapper.insert(slot);
        return slot;
    }

    /**
     * 获取已解锁格子集合（key 为 "row,col"）
     */
    public Set<String> getUnlockedCellKeys(Long playerId, int slotIndex) {
        PlayerBackpackSlot slot = getOrCreate(playerId, slotIndex);
        return backpackUnlockOrderService.getUnlockedCellKeys(slotIndex, slot.getMaxUnlockedOrder());
    }

    /**
     * 下一格待解锁（若已达最大返回 null）
     */
    public BackpackUnlockOrder getNextUnlock(Long playerId, int slotIndex) {
        PlayerBackpackSlot slot = getOrCreate(playerId, slotIndex);
        return backpackUnlockOrderService.getNextUnlock(slotIndex, slot.getMaxUnlockedOrder());
    }

    /**
     * 是否已达最大解锁（70）
     */
    public boolean isMaxUnlocked(Long playerId, int slotIndex) {
        PlayerBackpackSlot slot = getOrCreate(playerId, slotIndex);
        return slot.getMaxUnlockedOrder() >= BackpackUnlockOrderService.MAX_ORDER;
    }

    /**
     * 当前已解锁最大序号
     */
    public int getMaxUnlockedOrder(Long playerId, int slotIndex) {
        return getOrCreate(playerId, slotIndex).getMaxUnlockedOrder();
    }

    /**
     * 按序解锁：将对应 slot 的 max_unlocked_order + 1（暂不实现金币消耗）
     * 若已达最大解锁序号则抛出 BusinessException
     */
    public void unlockNext(Long playerId, int slotIndex) {
        PlayerBackpackSlot slot = getOrCreate(playerId, slotIndex);
        if (slot.getMaxUnlockedOrder() >= BackpackUnlockOrderService.MAX_ORDER) {
            throw new BusinessException("该背包位已全部解锁");
        }
        slot.setMaxUnlockedOrder(slot.getMaxUnlockedOrder() + 1);
        playerBackpackSlotMapper.updateById(slot);
    }
}
