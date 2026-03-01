package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.GameMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 地图 Mapper
 */
@Mapper
public interface GameMapMapper extends BaseMapper<GameMap> {

    @Select("SELECT * FROM game_map WHERE map_id = #{mapId} LIMIT 1")
    GameMap findByMapId(@Param("mapId") Integer mapId);
}
