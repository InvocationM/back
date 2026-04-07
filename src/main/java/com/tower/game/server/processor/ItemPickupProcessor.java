package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.map.MapCachedItem;
import com.tower.game.common.dto.map.MapCachedItemVo;
import com.tower.game.common.dto.map.MapLootCache;
import com.tower.game.common.dto.map.MapLootCacheVo;
import com.tower.game.model.entity.Item;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionState;
import com.tower.game.service.DropRollService;
import com.tower.game.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开箱处理器（4001）：查看地图上尸体/战斗宝箱缓存内的物品列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemPickupProcessor implements MessageProcessor {

    private final ItemService itemService;
    private final DropRollService dropRollService;

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

        SessionState state = session.authoritativeState();
        MapLootCache cache = state.getLootCache(mapCacheId);
        if (cache == null) {
            sendFail(session, "该缓存不存在或已被拾取完");
            return;
        }

        resolvePendingLootIfNeeded(cache, state);

        // 查询物品详情
        List<Integer> itemIds = cache.getItems().stream()
                .map(MapCachedItem::getItemId).distinct().toList();
        Map<Integer, Item> itemMap = itemIds.isEmpty() ? Map.of()
                : itemService.listByIds(itemIds).stream()
                .collect(java.util.stream.Collectors.toMap(Item::getId, i -> i));

        List<MapCachedItemVo> itemVos = new ArrayList<>();
        for (MapCachedItem ci : cache.getItems()) {
            Item item = itemMap.get(ci.getItemId());
            itemVos.add(MapCachedItemVo.builder()
                    .cachedItemId(ci.getCachedItemId())
                    .itemId(ci.getItemId())
                    .itemName(item != null ? item.getName() : null)
                    .itemIcon(item != null ? item.getIcon() : null)
                    .shapeType(item != null ? item.getShapeType() : null)
                    .count(ci.getCount())
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

    /**
     * 战斗宝箱等：首次开箱时对 pendingItemConfig 做一次 roll 并写入 items，再清空待解析字段。
     */
    private void resolvePendingLootIfNeeded(MapLootCache cache, SessionState state) {
        synchronized (cache) {
            String pending = cache.getPendingItemConfig();
            if (pending == null || pending.isBlank()) {
                return;
            }
            var drops = dropRollService.parseAndRoll(pending);
            cache.setPendingItemConfig(null);
            String mapCacheId = cache.getMapCacheId();
            List<MapCachedItem> list = new ArrayList<>();
            for (int i = 0; i < drops.size(); i++) {
                var d = drops.get(i);
                list.add(new MapCachedItem(
                        state.nextCachedItemId(mapCacheId, i),
                        d.getItemId(),
                        d.getCount()));
            }
            cache.setItems(list);
        }
    }

    private void sendFail(PlayerSession session, String message) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.ITEM_PICKUP);
        fail.put("code", 400);
        fail.put("message", message);
        session.sendMessage(fail);
    }
}
