package com.tower.game.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tower.game.common.dto.map.MapCachedItem;
import com.tower.game.common.dto.map.MapLootCache;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapLootCacheService {

    private static final String KEY_PREFIX = "tower:loot:";
    public static final String SOURCE_TYPE_DROP = "DROP";

    private final StringRedisTemplate stringRedisTemplate;
    private final BigMapRunRedisService bigMapRunRedisService;

    public String createLoot(Long userId, MapLootCache cache) {
        if (userId == null || cache == null) {
            throw new BusinessException("地图掉落缓存参数无效");
        }
        if (cache.getMapCacheId() == null || cache.getMapCacheId().isBlank()) {
            cache.setMapCacheId(newId("loot"));
        }
        if (cache.getItems() == null) {
            cache.setItems(new ArrayList<>());
        }
        Map<String, MapLootCache> caches = load(userId);
        caches.put(cache.getMapCacheId(), cache);
        save(userId, caches);
        return cache.getMapCacheId();
    }

    public MapLootCache getLoot(Long userId, String mapCacheId) {
        if (userId == null || mapCacheId == null || mapCacheId.isBlank()) {
            return null;
        }
        return load(userId).get(mapCacheId);
    }

    public void saveLoot(Long userId, MapLootCache cache) {
        if (userId == null || cache == null || cache.getMapCacheId() == null || cache.getMapCacheId().isBlank()) {
            return;
        }
        Map<String, MapLootCache> caches = load(userId);
        caches.put(cache.getMapCacheId(), cache);
        save(userId, caches);
    }

    public MapCachedItem findCachedItem(Long userId, String cachedItemId) {
        if (userId == null || cachedItemId == null || cachedItemId.isBlank()) {
            return null;
        }
        for (MapLootCache cache : load(userId).values()) {
            if (cache.getItems() == null) continue;
            for (MapCachedItem item : cache.getItems()) {
                if (cachedItemId.equals(item.getCachedItemId())) {
                    return item;
                }
            }
        }
        return null;
    }

    public MapCachedItem removeCachedItem(Long userId, String cachedItemId) {
        RemoveCachedItemResult result = removeCachedItemWithResult(userId, cachedItemId);
        return result != null ? result.item() : null;
    }

    public RemoveCachedItemResult removeCachedItemWithResult(Long userId, String cachedItemId) {
        if (userId == null || cachedItemId == null || cachedItemId.isBlank()) {
            return null;
        }
        Map<String, MapLootCache> caches = load(userId);
        MapCachedItem removed = null;
        var cacheIt = caches.entrySet().iterator();
        while (cacheIt.hasNext()) {
            MapLootCache cache = cacheIt.next().getValue();
            if (cache.getItems() == null) continue;
            var itemIt = cache.getItems().iterator();
            while (itemIt.hasNext()) {
                MapCachedItem item = itemIt.next();
                if (cachedItemId.equals(item.getCachedItemId())) {
                    removed = item;
                    itemIt.remove();
                    String mapCacheId = cache.getMapCacheId();
                    int cellX = cache.getCellX();
                    int cellY = cache.getCellY();
                    boolean cacheCleared = cache.getItems().isEmpty();
                    if (cache.getItems().isEmpty()) {
                        cacheIt.remove();
                    }
                    save(userId, caches);
                    return new RemoveCachedItemResult(removed, cacheCleared, mapCacheId, cellX, cellY);
                }
            }
        }
        return null;
    }

    public String addItemAtCell(Long userId, int cellX, int cellY, int itemId, int count) {
        AddCachedItemResult result = addItemAtCellWithResult(userId, cellX, cellY, itemId, count);
        return result != null ? result.cachedItemId() : null;
    }

    public AddCachedItemResult addItemAtCellWithResult(Long userId, int cellX, int cellY, int itemId, int count) {
        Map<String, MapLootCache> caches = load(userId);
        for (MapLootCache cache : caches.values()) {
            if (cache.getCellX() == cellX && cache.getCellY() == cellY && SOURCE_TYPE_DROP.equals(cache.getSourceType())) {
                if (cache.getItems() == null) {
                    cache.setItems(new ArrayList<>());
                }
                String cachedItemId = newId("item");
                cache.getItems().add(new MapCachedItem(cachedItemId, itemId, count));
                save(userId, caches);
                return new AddCachedItemResult(cache.getMapCacheId(), cachedItemId, cellX, cellY, cache.getSourceType());
            }
        }

        MapLootCache cache = new MapLootCache();
        cache.setMapCacheId(newId("loot"));
        cache.setCellX(cellX);
        cache.setCellY(cellY);
        cache.setSourceType(SOURCE_TYPE_DROP);
        cache.setItems(new ArrayList<>());
        String cachedItemId = newId("item");
        cache.getItems().add(new MapCachedItem(cachedItemId, itemId, count));
        caches.put(cache.getMapCacheId(), cache);
        save(userId, caches);
        return new AddCachedItemResult(cache.getMapCacheId(), cachedItemId, cellX, cellY, cache.getSourceType());
    }

    public String nextCachedItemId() {
        return newId("item");
    }

    public void clearAll(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(buildKey(userId));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis clear loot failed userId={}", userId, e);
            throw new BusinessException(500, "地图掉落缓存清理失败");
        }
    }

    private Map<String, MapLootCache> load(Long userId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(buildKey(userId));
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            Map<String, MapLootCache> parsed = JsonUtil.parseObject(json, new TypeReference<Map<String, MapLootCache>>() {});
            return parsed != null ? parsed : new LinkedHashMap<>();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis read loot failed userId={}", userId, e);
            throw new BusinessException(500, "地图掉落缓存读取失败");
        }
    }

    private void save(Long userId, Map<String, MapLootCache> caches) {
        try {
            String key = buildKey(userId);
            if (caches == null || caches.isEmpty()) {
                stringRedisTemplate.delete(key);
            } else {
                stringRedisTemplate.opsForValue().set(key, JsonUtil.toJsonString(caches), bigMapRunRedisService.ttl());
            }
            bigMapRunRedisService.touchRun(userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis write loot failed userId={}", userId, e);
            throw new BusinessException(500, "地图掉落缓存写入失败");
        }
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId + ":" + bigMapRunRedisService.requireRunId(userId);
    }

    private static String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    public record RemoveCachedItemResult(
            MapCachedItem item,
            boolean cacheCleared,
            String mapCacheId,
            int cellX,
            int cellY) {}

    public record AddCachedItemResult(
            String mapCacheId,
            String cachedItemId,
            int cellX,
            int cellY,
            String sourceType) {}
}
