package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.Item;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物品 Mapper
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {
}
