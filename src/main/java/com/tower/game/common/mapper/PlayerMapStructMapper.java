package com.tower.game.common.mapper;

import com.tower.game.common.dto.PlayerDTO;
import com.tower.game.model.entity.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Player 实体和 DTO 之间的映射器（MapStruct）
 * MapStruct 会在编译时自动生成实现类
 * 
 * 注意：这个 Mapper 用于实体和 DTO 之间的转换
 * MyBatis Plus 的 PlayerMapper 在 com.tower.game.mapper 包下
 */
@Mapper(componentModel = "spring")
public interface PlayerMapStructMapper {
    
    /**
     * 获取映射器实例（如果使用 componentModel = "spring"，则通过 Spring 注入）
     */
    PlayerMapStructMapper INSTANCE = Mappers.getMapper(PlayerMapStructMapper.class);

    /**
     * Player 实体转 PlayerDTO
     * 字段名相同会自动映射，密码字段会被忽略（因为 DTO 中没有）
     */
    PlayerDTO toDTO(Player player);

    /**
     * PlayerDTO 转 Player 实体
     * 密码字段需要单独设置（DTO 中没有密码）
     */
    @Mapping(target = "password", ignore = true)
    Player toEntity(PlayerDTO playerDTO);

    /**
     * Player 列表转 PlayerDTO 列表
     */
    List<PlayerDTO> toDTOList(List<Player> players);

    /**
     * PlayerDTO 列表转 Player 列表
     */
    List<Player> toEntityList(List<PlayerDTO> playerDTOs);
}
