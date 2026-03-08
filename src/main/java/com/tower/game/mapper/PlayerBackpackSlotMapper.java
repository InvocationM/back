package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.PlayerBackpackSlot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 玩家背包位状态 Mapper
 */
@Mapper
public interface PlayerBackpackSlotMapper extends BaseMapper<PlayerBackpackSlot> {
}
