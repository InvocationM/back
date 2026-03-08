package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.BackpackUnlockOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 背包解锁次序 Mapper
 */
@Mapper
public interface BackpackUnlockOrderMapper extends BaseMapper<BackpackUnlockOrder> {
}
