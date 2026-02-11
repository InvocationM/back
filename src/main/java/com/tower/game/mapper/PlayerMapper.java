package com.tower.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tower.game.model.entity.Player;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 玩家Mapper
 */
@Mapper
public interface PlayerMapper extends BaseMapper<Player> {

    /**
     * 根据用户名查询玩家
     */
    @Select("SELECT * FROM player WHERE username = #{username}")
    Player findByUsername(@Param("username") String username);

    /**
     * 查询高等级玩家（示例：使用XML方式）
     */
    List<Player> findHighLevelPlayers(@Param("minLevel") Integer minLevel);
}
