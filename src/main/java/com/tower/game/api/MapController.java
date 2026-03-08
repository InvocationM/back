package com.tower.game.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.GameMap;
import com.tower.game.service.GameMapService;
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

    private final GameMapService gameMapService;
    private final ObjectMapper objectMapper;

    /**
     * 根据 mapId 查询地图，返回整份前端 JSON（mapId、width、height、cells）
     */
    @PostMapping("/{mapId}")
    public ApiResponse<?> getByMapId(@PathVariable Integer mapId) {
        GameMap map = gameMapService.getByMapId(mapId);
        if (map == null) {
            throw new BusinessException(404, "地图不存在");
        }
        try {
            Object data = objectMapper.readValue(map.getData(), Object.class);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.warn("解析地图 JSON 失败 mapId={}", mapId, e);
            throw new BusinessException(500, "地图数据格式异常");
        }
    }
}
