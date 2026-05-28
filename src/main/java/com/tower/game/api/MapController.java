package com.tower.game.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.auth.CurrentUserResolver;
import com.tower.game.common.dto.OpenDoorRequest;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.GameMap;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionManager;
import com.tower.game.service.GameMapService;
import com.tower.game.service.MapOpenDoorService;
import com.tower.game.service.SessionMapRedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final GameMapService gameMapService;
    private final SessionMapRedisService sessionMapRedisService;
    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;
    private final MapOpenDoorService mapOpenDoorService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/openDoor")
    public ApiResponse<Void> openDoor(@Valid @RequestBody OpenDoorRequest request, HttpServletRequest httpRequest) {
        long playerId = currentUserResolver.requireUser(httpRequest).getUserId();
        PlayerSession session = sessionManager.getSessionByUserId(playerId);
        if (session == null) {
            throw new BusinessException("玩家未在线");
        }
        mapOpenDoorService.openDoor(session, request.getCellX(), request.getCellY(), request.getDoorId());
        return ApiResponse.success(null);
    }

    @PostMapping("/{mapId}")
    public ApiResponse<?> getByMapId(@PathVariable Integer mapId, HttpServletRequest httpRequest) {
        long playerId = currentUserResolver.requireUser(httpRequest).getUserId();
        String json = sessionMapRedisService.getMapJson(playerId, mapId);
        if (json == null || json.isBlank()) {
            GameMap map = gameMapService.getByMapId(mapId);
            if (map == null) {
                throw new BusinessException(404, "地图不存在");
            }
            if (map.getData() == null || map.getData().isBlank()) {
                throw new BusinessException(500, "地图数据为空");
            }
            sessionMapRedisService.saveMapJson(playerId, mapId, map.getData());
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
