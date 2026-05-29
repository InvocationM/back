package com.tower.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tower.game.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 修改地图 JSON 中单个格子（拾取可入包地图物后改为空地）
 */
@Service
@RequiredArgsConstructor
public class MapJsonCellService {

    private static final int EVENT_TYPE_EMPTY = 2;

    private final ObjectMapper objectMapper;

    public record MapCellPickupResult(int itemId, int count, String newMapJson) {}

    public record MapDoorOpenResult(String newMapJson) {}

    public record MapEventClearResult(String newMapJson) {}

    /**
     * 校验 (x,y) 格首事件为指定 type（如钥匙 7、血瓶 9），替换为空地 type=2。
     */
    public MapCellPickupResult pickupMapItemFromCell(String mapJson, int x, int y, int expectedEventType) {
        if (mapJson == null || mapJson.isBlank()) {
            throw new BusinessException("地图数据为空");
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(mapJson);
            JsonNode cells = root.get("cells");
            if (cells == null || !cells.isArray()) {
                throw new BusinessException("地图无 cells 数据");
            }
            for (int i = 0; i < cells.size(); i++) {
                JsonNode cell = cells.get(i);
                if (cell == null || !cell.isObject()) continue;
                if (cell.path("x").asInt() != x || cell.path("y").asInt() != y) continue;

                JsonNode events = cell.get("events");
                if (events == null || !events.isArray() || events.size() == 0) {
                    throw new BusinessException("该格无可拾取事件");
                }
                JsonNode first = events.get(0);
                int type = first.path("type").asInt(0);
                if (type != expectedEventType) {
                    throw new BusinessException(pickupWrongTypeMessage(expectedEventType));
                }
                int itemId = first.path("id").asInt(0);
                if (itemId <= 0) {
                    throw new BusinessException("地图事件未配置物品 id");
                }
                int count = first.path("count").asInt(1);
                if (count <= 0) {
                    throw new BusinessException("拾取数量无效");
                }

                ArrayNode newEvents = objectMapper.createArrayNode();
                ObjectNode emptyEv = objectMapper.createObjectNode();
                emptyEv.put("type", EVENT_TYPE_EMPTY);
                emptyEv.put("id", 0);
                emptyEv.put("weight", 100);
                newEvents.add(emptyEv);
                ((ObjectNode) cell).set("events", newEvents);

                return new MapCellPickupResult(itemId, count, objectMapper.writeValueAsString(root));
            }
            throw new BusinessException("该格无地图事件");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析或修改地图 JSON 失败");
        }
    }

    /**
     * 将门上事件（type=8）替换为空地 type=2；校验首事件为门且 id 与 doorId 一致。
     */
    public MapDoorOpenResult openDoorFromCell(String mapJson, int x, int y, int expectedDoorId) {
        if (mapJson == null || mapJson.isBlank()) {
            throw new BusinessException("地图数据为空");
        }
        if (expectedDoorId <= 0) {
            throw new BusinessException("doorId 无效");
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(mapJson);
            JsonNode cells = root.get("cells");
            if (cells == null || !cells.isArray()) {
                throw new BusinessException("地图无 cells 数据");
            }
            for (int i = 0; i < cells.size(); i++) {
                JsonNode cell = cells.get(i);
                if (cell == null || !cell.isObject()) continue;
                if (cell.path("x").asInt() != x || cell.path("y").asInt() != y) continue;

                JsonNode events = cell.get("events");
                if (events == null || !events.isArray() || events.size() == 0) {
                    throw new BusinessException("该格无门事件");
                }
                JsonNode first = events.get(0);
                int type = first.path("type").asInt(0);
                if (type != MapWalkableService.EVENT_TYPE_DOOR) {
                    throw new BusinessException("该格不是门");
                }
                int doorCellId = first.path("id").asInt(0);
                if (doorCellId != expectedDoorId) {
                    throw new BusinessException("门上编号与请求 doorId 不一致");
                }

                ArrayNode newEvents = objectMapper.createArrayNode();
                ObjectNode emptyEv = objectMapper.createObjectNode();
                emptyEv.put("type", EVENT_TYPE_EMPTY);
                emptyEv.put("id", 0);
                emptyEv.put("weight", 100);
                newEvents.add(emptyEv);
                ((ObjectNode) cell).set("events", newEvents);

                return new MapDoorOpenResult(objectMapper.writeValueAsString(root));
            }
            throw new BusinessException("该格无地图事件");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析或修改地图 JSON 失败");
        }
    }

    public MapEventClearResult clearEventFromCell(String mapJson, int x, int y, int expectedEventType, int expectedEventId) {
        if (mapJson == null || mapJson.isBlank()) {
            throw new BusinessException("map data is empty");
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(mapJson);
            JsonNode cells = root.get("cells");
            if (cells == null || !cells.isArray()) {
                throw new BusinessException("map cells missing");
            }
            for (int i = 0; i < cells.size(); i++) {
                JsonNode cell = cells.get(i);
                if (cell == null || !cell.isObject()) continue;
                if (cell.path("x").asInt() != x || cell.path("y").asInt() != y) continue;

                JsonNode events = cell.get("events");
                if (events == null || !events.isArray() || events.size() == 0) {
                    throw new BusinessException("cell has no map event");
                }
                JsonNode first = events.get(0);
                int type = first.path("type").asInt(0);
                int id = first.path("id").asInt(0);
                if (type != expectedEventType || id != expectedEventId) {
                    throw new BusinessException("cell event does not match expected event");
                }

                ArrayNode newEvents = objectMapper.createArrayNode();
                ObjectNode emptyEv = objectMapper.createObjectNode();
                emptyEv.put("type", EVENT_TYPE_EMPTY);
                emptyEv.put("id", 0);
                emptyEv.put("weight", 100);
                newEvents.add(emptyEv);
                ((ObjectNode) cell).set("events", newEvents);

                return new MapEventClearResult(objectMapper.writeValueAsString(root));
            }
            throw new BusinessException("cell has no map event");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("failed to parse or update map JSON");
        }
    }

    private static String pickupWrongTypeMessage(int expectedEventType) {
        if (expectedEventType == MapWalkableService.EVENT_TYPE_KEY) {
            return "该格不是可拾取钥匙";
        }
        if (expectedEventType == MapWalkableService.EVENT_TYPE_BLOOD_POTION) {
            return "该格不是可拾取血瓶";
        }
        return "该格事件类型与拾取不匹配";
    }
}
