package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.PlayerAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 玩家属性 Mapper
 */
@Mapper
public interface PlayerAttributeMapper extends BaseMapper<PlayerAttribute> {

    @Select("SELECT * FROM player_attribute WHERE player_id = #{playerId} LIMIT 1")
    PlayerAttribute findByPlayerId(@Param("playerId") Long playerId);
}
