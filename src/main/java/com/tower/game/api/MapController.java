package com.tower.game.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.GameMap;
import com.tower.game.service.GameMapService;
import com.tower.game.service.SessionMapRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地图查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    /** 与 BackpackController 一致，临时写死用户 ID，后续改为登录态 */
    private static final long DEFAULT_PLAYER_ID = 1001L;

    private final GameMapService gameMapService;
    private final SessionMapRedisService sessionMapRedisService;
    private final ObjectMapper objectMapper;

    /**
     * 根据 mapId 查询地图，返回整份前端 JSON（mapId、width、height、cells）。
     * 优先读 Redis 缓存（tower:session:map:{userId}:{mapId}），未命中则查库并写入，与移动协议侧缓存键一致。
     */
    @PostMapping("/{mapId}")
    public ApiResponse<?> getByMapId(@PathVariable Integer mapId) {
        String json = sessionMapRedisService.getMapJson(DEFAULT_PLAYER_ID, mapId);
        if (json == null || json.isBlank()) {
            GameMap map = gameMapService.getByMapId(mapId);
            if (map == null) {
                throw new BusinessException(404, "地图不存在");
            }
            if (map.getData() == null || map.getData().isBlank()) {
                throw new BusinessException(500, "地图数据为空");
            }
            sessionMapRedisService.saveMapJson(DEFAULT_PLAYER_ID, mapId, map.getData());
            json = map.getData();
        }
        try {
            Object data = objectMapper.readValue(json, Object.class);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.warn("解析地图 JSON 失败 mapId={}", mapId, e);
            throw new BusinessException(500, "地图数据格式异常");
        }
    }
}
