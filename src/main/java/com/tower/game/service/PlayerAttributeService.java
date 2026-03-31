package com.tower.game.service;

import com.tower.game.mapper.PlayerAttributeMapper;
import com.tower.game.model.entity.PlayerAttribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 玩家属性服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerAttributeService {

    private final PlayerAttributeMapper playerAttributeMapper;

    /**
     * 根据玩家ID查询属性（唯一查询入口）
     */
    public PlayerAttribute getByPlayerId(Long playerId) {
        return playerAttributeMapper.findByPlayerId(playerId);
    }

    /**
     * 更新玩家属性
     */
    public boolean updateById(PlayerAttribute attribute) {
        attribute.setUpdateTime(LocalDateTime.now());
        return playerAttributeMapper.updateById(attribute) > 0;
    }
}
