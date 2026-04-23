package com.tower.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tower.game.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 地图可通行与入口查询（与客户端 MapEventType 规则一致）
 * 1=入口 2=空地 3=阻挡 4=出口 5=怪物 6=宝箱 7=钥匙 8=门 9=血瓶；type=3、8 不可通行，其余可通行。
 * <p>
 * 所有方法均要求调用方传入缓存的 mapData，不再自行查库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapWalkableService {

    private static final int EVENT_TYPE_BLOCK = 3;
    private static final int EVENT_TYPE_ENTRANCE = 1;
    private static final int EVENT_TYPE_EMPTY = 2;
    private static final int EVENT_TYPE_EXIT = 4;
    private static final int EVENT_TYPE_MONSTER = 5;
    private static final int EVENT_TYPE_CHEST = 6;

    /** 地图事件：钥匙（与客户端 MapEventType 一致） */
    public static final int EVENT_TYPE_KEY = 7;

    /** 地图事件：门（与客户端 MapEventType 一致），未开启前不可通行 */
    public static final int EVENT_TYPE_DOOR = 8;

    /** 地图事件：血瓶（与客户端 MapEventType 一致） */
    public static final int EVENT_TYPE_BLOOD_POTION = 9;

    private final ObjectMapper objectMapper;

    /**
     * 获取 mapData，为空直接报错。
     */
    private String requireMapData(Integer mapId, String mapData) {
        if (mapData != null && !mapData.isBlank()) return mapData;
        throw new BusinessException(500, "地图缓存不存在，请先通过地图接口加载 mapId=" + mapId);
    }

    /**
     * 判断地图上某格是否可通行（在范围内且非阻挡）
     */
    public boolean isWalkable(Integer mapId, int x, int y, String mapData) {
        if (mapId == null) return false;
        String data = requireMapData(mapId, mapData);
        if (data == null) return false;
        try {
            JsonNode root = objectMapper.readTree(data);
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
                        return type != EVENT_TYPE_BLOCK && type != EVENT_TYPE_DOOR;
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
     * 寻路用可通行：仅空地(2)、入口(1)、出口(4)为 true；阻挡(3)、怪物(5)、宝箱(6)、门(8)等为 false（门与怪/箱相同，路径止于邻格）。
     */
    public boolean isWalkableForPathfinding(Integer mapId, int x, int y, String mapData) {
        if (mapId == null) return false;
        String data = requireMapData(mapId, mapData);
        if (data == null) return false;
        try {
            JsonNode root = objectMapper.readTree(data);
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
                        return type == EVENT_TYPE_ENTRANCE || type == EVENT_TYPE_EMPTY || type == EVENT_TYPE_EXIT;
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
    public int[] findEntrance(Integer mapId, String mapData) {
        int[] fallback = {0, 0};
        if (mapId == null) return fallback;
        String data = requireMapData(mapId, mapData);
        if (data == null) return fallback;
        try {
            JsonNode root = objectMapper.readTree(data);
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
     * 获取格子上的事件类型与ID。与客户端 MapEventType 一致：1=入口 2=空地 3=阻挡 4=出口 5=怪物 6=宝箱 7=钥匙 8=门 9=血瓶。
     *
     * @return [type, id]，若无事件或越界返回 null；有事件时 id 为 events[0].id（怪物/宝箱等）
     */
    public int[] getCellEvent(Integer mapId, int x, int y, String mapData) {
        if (mapId == null) return null;
        String data = requireMapData(mapId, mapData);
        if (data == null) return null;
        try {
            JsonNode root = objectMapper.readTree(data);
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
    public int[] getMapSize(Integer mapId, String mapData) {
        if (mapId == null) return new int[]{20, 20};
        String data = requireMapData(mapId, mapData);
        if (data == null) return new int[]{20, 20};
        try {
            JsonNode root = objectMapper.readTree(data);
            return new int[]{root.path("width").asInt(20), root.path("height").asInt(20)};
        } catch (Exception e) {
            log.warn("解析地图尺寸失败 mapId={}", mapId, e);
            return new int[]{20, 20};
        }
    }
}
