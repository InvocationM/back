package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.map.MapCachedItem;
import com.tower.game.common.dto.map.MapCachedItemVo;
import com.tower.game.common.dto.map.MapLootCache;
import com.tower.game.common.dto.map.MapLootCacheVo;
import com.tower.game.model.entity.Item;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.DropRollService;
import com.tower.game.service.ItemService;
import com.tower.game.service.MapLootCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemPickupProcessor implements MessageProcessor {

    private final ItemService itemService;
    private final DropRollService dropRollService;
    private final MapLootCacheService mapLootCacheService;

    @Override
    public void handle(PlayerSession session, Object message) {
        if (!(message instanceof Map)) {
            sendFail(session, "消息格式错误");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        String mapCacheId = (String) msg.get("mapCacheId");
        if (mapCacheId == null || mapCacheId.isBlank()) {
            sendFail(session, "缺少 mapCacheId");
            return;
        }

        MapLootCache cache = mapLootCacheService.getLoot(session.getUserId(), mapCacheId);
        if (cache == null) {
            sendFail(session, "该缓存不存在或已被拾取完");
            return;
        }

        resolvePendingLootIfNeeded(session.getUserId(), cache);

        List<Integer> itemIds = cache.getItems().stream()
                .map(MapCachedItem::getItemId).distinct().toList();
        Map<Integer, Item> itemMap = itemIds.isEmpty() ? Map.of()
                : itemService.listByIds(itemIds).stream()
                .collect(java.util.stream.Collectors.toMap(Item::getId, i -> i));

        List<MapCachedItemVo> itemVos = new ArrayList<>();
        for (MapCachedItem cachedItem : cache.getItems()) {
            Item item = itemMap.get(cachedItem.getItemId());
            itemVos.add(MapCachedItemVo.builder()
                    .cachedItemId(cachedItem.getCachedItemId())
                    .itemId(cachedItem.getItemId())
                    .itemName(item != null ? item.getName() : null)
                    .itemIcon(item != null ? item.getIcon() : null)
                    .shapeType(item != null ? item.getShapeType() : null)
                    .count(cachedItem.getCount())
                    .build());
        }

        MapLootCacheVo vo = MapLootCacheVo.builder()
                .mapCacheId(cache.getMapCacheId())
                .sourceType(cache.getSourceType())
                .items(itemVos)
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("type", MessageType.ITEM_PICKUP);
        response.put("code", 200);
        response.put("data", vo);
        session.sendMessage(response);
    }

    @Override
    public int getMessageType() {
        return MessageType.ITEM_PICKUP;
    }

    private void resolvePendingLootIfNeeded(Long userId, MapLootCache cache) {
        String pending = cache.getPendingItemConfig();
        if (pending == null || pending.isBlank()) {
            return;
        }
        var drops = dropRollService.parseAndRoll(pending);
        cache.setPendingItemConfig(null);
        List<MapCachedItem> items = new ArrayList<>();
        for (var drop : drops) {
            items.add(new MapCachedItem(
                    mapLootCacheService.nextCachedItemId(),
                    drop.getItemId(),
                    drop.getCount()));
        }
        cache.setItems(items);
        mapLootCacheService.saveLoot(userId, cache);
    }

    private void sendFail(PlayerSession session, String message) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.ITEM_PICKUP);
        fail.put("code", 400);
        fail.put("message", message);
        session.sendMessage(fail);
    }
}
