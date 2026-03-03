package com.tower.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.model.entity.GameMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 地图可通行与入口查询（与客户端 MapEventType 规则一致）
 * 1=入口 2=空地 3=阻挡 4=出口 5=怪物 6=宝箱；type=3 不可通行，其余可通行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapWalkableService {

    private static final int EVENT_TYPE_BLOCK = 3;

    private final GameMapService gameMapService;
    private final ObjectMapper objectMapper;

    /**
     * 判断地图上某格是否可通行（在范围内且非阻挡）
     */
    public boolean isWalkable(Integer mapId, int x, int y) {
        if (mapId == null) return false;
        GameMap map = gameMapService.getByMapId(mapId);
        if (map == null || map.getData() == null || map.getData().isBlank()) return false;
        try {
            JsonNode root = objectMapper.readTree(map.getData());
            int width = root.path("width").asInt(20);
            int height = root.path("height").asInt(20);
            if (x < 0 || x >= width || y < 0 || y >= height) return false;
            JsonNode cells = root.get("cells");
            if (cells == null || !cells.isArray()) return true;
            for (JsonNode cell : cells) {
                if (cell.path("x").asInt() == x && cell.path("y").asInt() == y) {
                    JsonNode events = cell.get("events");
                    if (events != null && events.isArray() && events.size() > 0) {
                        int type = events.get(0).path("type").asInt(0);
                        return type != EVENT_TYPE_BLOCK;
                    }
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("解析地图失败 mapId={} x={} y={}", mapId, x, y, e);
            return false;
        }
    }

    /**
     * 返回地图入口格子坐标，若无入口则 (0,0)
     */
    public int[] findEntrance(Integer mapId) {
        int[] fallback = {0, 0};
        if (mapId == null) return fallback;
        GameMap map = gameMapService.getByMapId(mapId);
        if (map == null || map.getData() == null || map.getData().isBlank()) return fallback;
        try {
            JsonNode root = objectMapper.readTree(map.getData());
            JsonNode cells = root.get("cells");
            if (cells == null || !cells.isArray()) return fallback;
            for (JsonNode cell : cells) {
                JsonNode events = cell.get("events");
                if (events != null && events.isArray() && events.size() > 0
                        && events.get(0).path("type").asInt(0) == 1) {
                    return new int[]{cell.path("x").asInt(), cell.path("y").asInt()};
                }
            }
            return fallback;
        } catch (Exception e) {
            log.warn("查入口失败 mapId={}", mapId, e);
            return fallback;
        }
    }

    /**
     * 获取格子上的事件类型与ID。与客户端 MapEventType 一致：1=入口 2=空地 3=阻挡 4=出口 5=怪物 6=宝箱。
     *
     * @return [type, id]，若无事件或越界返回 null；有事件时 id 为 events[0].id（怪物/宝箱等）
     */
    public int[] getCellEvent(Integer mapId, int x, int y) {
        if (mapId == null) return null;
        GameMap map = gameMapService.getByMapId(mapId);
        if (map == null || map.getData() == null || map.getData().isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(map.getData());
            int width = root.path("width").asInt(20);
            int height = root.path("height").asInt(20);
            if (x < 0 || x >= width || y < 0 || y >= height) return null;
            JsonNode cells = root.get("cells");
            if (cells == null || !cells.isArray()) return null;
            for (JsonNode cell : cells) {
                if (cell.path("x").asInt() == x && cell.path("y").asInt() == y) {
                    JsonNode events = cell.get("events");
                    if (events != null && events.isArray() && events.size() > 0) {
                        JsonNode first = events.get(0);
                        int type = first.path("type").asInt(0);
                        int id = first.path("id").asInt(0);
                        return new int[]{type, id};
                    }
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("解析地图格子事件失败 mapId={} x={} y={}", mapId, x, y, e);
            return null;
        }
    }

    /**
     * 返回地图宽高 [width, height]
     */
    public int[] getMapSize(Integer mapId) {
        if (mapId == null) return new int[]{20, 20};
        GameMap map = gameMapService.getByMapId(mapId);
        if (map == null || map.getData() == null || map.getData().isBlank())
            return new int[]{20, 20};
        try {
            JsonNode root = objectMapper.readTree(map.getData());
            return new int[]{root.path("width").asInt(20), root.path("height").asInt(20)};
        } catch (Exception e) {
            log.warn("解析地图尺寸失败 mapId={}", mapId, e);
            return new int[]{20, 20};
        }
    }
}
