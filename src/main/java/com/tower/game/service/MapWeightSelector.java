package com.tower.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tower.game.model.dto.map.CellEventEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 地图格子事件权重选择器。
 * 根据客户端逻辑：每个格子从多个事件中按权重随机选一个，权重为0不参与，概率 = 事件权重/总权重。
 */
@Component
@RequiredArgsConstructor
public class MapWeightSelector {

    private final ObjectMapper objectMapper;

    /**
     * 对地图 JSON 字符串进行处理：每个格子的 events 数组只保留一个事件（按权重随机选中）。
     * 若某格 events 为空或只有一个事件，则不改动。
     *
     * @param mapDataJson 原始地图 JSON（含 mapId、width、height、cells）
     * @return 处理后的地图 JSON，每个格子的 events 至多一个元素
     */
    public String applyWeightSelectionPerCell(String mapDataJson) {
        if (mapDataJson == null || mapDataJson.isBlank()) {
            return mapDataJson;
        }
        try {
            JsonNode root = objectMapper.readTree(mapDataJson);
            JsonNode cells = root.get("cells");
            if (cells == null || !cells.isArray()) {
                return mapDataJson;
            }
            for (int i = 0; i < cells.size(); i++) {
                JsonNode cell = cells.get(i);
                if (cell == null || !cell.isObject()) continue;
                JsonNode events = cell.get("events");
                if (events == null || !events.isArray() || events.size() <= 1) continue;

                List<CellEventEntry> entries = new ArrayList<>();
                for (int j = 0; j < events.size(); j++) {
                    JsonNode ev = events.get(j);
                    if (ev == null || !ev.isObject()) continue;
                    CellEventEntry entry = objectMapper.treeToValue(ev, CellEventEntry.class);
                    if (entry != null) entries.add(entry);
                }
                CellEventEntry selected = selectEventByWeight(entries);
                if (selected != null) {
                    ArrayNode newEvents = objectMapper.createArrayNode();
                    newEvents.add(objectMapper.valueToTree(selected));
                    ((ObjectNode) cell).set("events", newEvents);
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalArgumentException("地图 JSON 处理失败", e);
        }
    }

    /**
     * 根据权重从事件列表中随机选择一个事件。
     * 权重为0的事件不参与；所有权重为0时返回 null。
     * 每个事件的选中概率 = 事件权重 / 总权重。
     *
     * @param events 事件列表（可为空）
     * @return 选中的事件，无有效事件时返回 null
     */
    public CellEventEntry selectEventByWeight(List<CellEventEntry> events) {
        if (events == null || events.isEmpty()) return null;

        List<CellEventEntry> valid = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (CellEventEntry e : events) {
            if (isValidEvent(e) && e.getWeight() > 0) {
                valid.add(e);
                weights.add(e.getWeight());
            }
        }
        if (valid.isEmpty()) return null;
        if (valid.size() == 1) return valid.get(0);

        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) return null; // 额外安全检查
        // 使用 ThreadLocalRandom 替代 Random
        int r = ThreadLocalRandom.current().nextInt(totalWeight);
        int acc = 0;
        for (int i = 0; i < valid.size(); i++) {
            acc += weights.get(i);
            if (r < acc) return valid.get(i);
        }
        return valid.get(0);
    }

    private static boolean isValidEvent(CellEventEntry e) {
        return e != null && e.getType() > 0;
    }
}
