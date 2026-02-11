package com.tower.game.api;

import com.tower.game.common.dto.PlayerDTO;
import com.tower.game.common.mapper.PlayerMapStructMapper;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.Player;
import com.tower.game.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 玩家查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerMapStructMapper playerMapStructMapper;

    /**
     * 查询所有玩家列表
     */
    @GetMapping("/list")
    public ApiResponse<List<PlayerDTO>> list() {
        List<Player> players = playerService.getAllPlayers();
        List<PlayerDTO> list = playerMapStructMapper.toDTOList(players);
        return ApiResponse.success(list);
    }

    /**
     * 根据ID查询玩家
     */
    @GetMapping("/{id}")
    public ApiResponse<PlayerDTO> getById(@PathVariable Long id) {
        Player player = playerService.getPlayerById(id);
        if (player == null) {
            return ApiResponse.error(404, "玩家不存在");
        }
        PlayerDTO dto = playerMapStructMapper.toDTO(player);
        return ApiResponse.success(dto);
    }
}
