package com.tower.game.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tower.game.mapper.PlayerMapper;
import com.tower.game.model.entity.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 玩家服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerMapper playerMapper;

    /**
     * 根据ID查询玩家
     */
    public Player getPlayerById(Long id) {
        return playerMapper.selectById(id);
    }

    /**
     * 根据用户名查询玩家
     */
    public Player getPlayerByUsername(String username) {
        return playerMapper.findByUsername(username);
    }

    /**
     * 保存玩家（新增或更新）
     */
    public boolean savePlayer(Player player) {
        if (player.getId() == null) {
            // 新增
            player.setCreateTime(LocalDateTime.now());
            player.setUpdateTime(LocalDateTime.now());
            return playerMapper.insert(player) > 0;
        } else {
            // 更新
            player.setUpdateTime(LocalDateTime.now());
            return playerMapper.updateById(player) > 0;
        }
    }

    /**
     * 删除玩家
     */
    public boolean deletePlayer(Long id) {
        return playerMapper.deleteById(id) > 0;
    }

    /**
     * 查询所有玩家
     */
    public List<Player> getAllPlayers() {
        return playerMapper.selectList(null);
    }

    /**
     * 根据等级查询玩家
     */
    public List<Player> getPlayersByLevel(Integer minLevel) {
        QueryWrapper<Player> wrapper = new QueryWrapper<>();
        wrapper.ge("level", minLevel);
        wrapper.orderByDesc("level", "exp");
        return playerMapper.selectList(wrapper);
    }

    /**
     * 查询高等级玩家（使用XML方式）
     */
    public List<Player> getHighLevelPlayers(Integer minLevel) {
        return playerMapper.findHighLevelPlayers(minLevel);
    }

    /**
     * 分页查询玩家
     */
    public Page<Player> getPlayersByPage(int current, int size) {
        Page<Player> page = new Page<>(current, size);
        return playerMapper.selectPage(page, null);
    }

    /**
     * 根据条件查询玩家数量
     */
    public long countPlayers(Integer minLevel) {
        QueryWrapper<Player> wrapper = new QueryWrapper<>();
        if (minLevel != null) {
            wrapper.ge("level", minLevel);
        }
        return playerMapper.selectCount(wrapper);
    }
}
