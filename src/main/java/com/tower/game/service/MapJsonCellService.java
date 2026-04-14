package com.tower.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tower.game.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 修改地图 JSON 中单个格子（如拾取血瓶后改为空地）
 */
@Service
@RequiredArgsConstructor
public class MapJsonCellService {

    private static final int EVENT_TYPE_EMPTY = 2;

    private final ObjectMapper objectMapper;

    public record BloodPotionPickupResult(int itemId, int count, String newMapJson) {}

    /**
     * 校验 (x,y) 格首事件为血瓶 {@link MapWalkableService#EVENT_TYPE_BLOOD_POTION}，替换为空地 type=2。
     */
    public BloodPotionPickupResult pickupBloodPotionFromCell(String mapJson, int x, int y) {
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
                if (type != MapWalkableService.EVENT_TYPE_BLOOD_POTION) {
                    throw new BusinessException("该格不是可拾取血瓶");
                }
                int itemId = first.path("id").asInt(0);
                if (itemId <= 0) {
                    throw new BusinessException("血瓶事件未配置物品 id");
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

                return new BloodPotionPickupResult(itemId, count, objectMapper.writeValueAsString(root));
            }
            throw new BusinessException("该格无地图事件");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析或修改地图 JSON 失败");
        }
    }
}
