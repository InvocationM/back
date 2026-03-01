package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.Chest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝箱 Mapper
 */
@Mapper
public interface ChestMapper extends BaseMapper<Chest> {
}
