package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.PlayerBackpackItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 玩家背包放置 Mapper
 */
@Mapper
public interface PlayerBackpackItemMapper extends BaseMapper<PlayerBackpackItem> {
}
