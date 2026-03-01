package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.Monster;
import org.apache.ibatis.annotations.Mapper;

/**
 * 怪物 Mapper
 */
@Mapper
public interface MonsterMapper extends BaseMapper<Monster> {
}
