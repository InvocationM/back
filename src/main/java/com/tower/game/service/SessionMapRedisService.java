package com.tower.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 将会话中当前地图 JSON 同步到 Redis（与 SessionState.currentMapData 一致）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionMapRedisService {

    private static final String KEY_PREFIX = "tower:session:map:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveMapJson(Long userId, Integer mapId, String mapJson) {
        if (userId == null || mapId == null || mapJson == null || mapJson.isBlank()) {
            return;
        }
        String key = buildKey(userId, mapId);
        try {
            stringRedisTemplate.opsForValue().set(key, mapJson);
            log.debug("Redis 已同步地图 JSON key={}", key);
        } catch (Exception e) {
            log.warn("Redis 写入地图 JSON 失败 userId={} mapId={}", userId, mapId, e);
        }
    }

    public void deleteMapJson(Long userId, Integer mapId) {
        if (userId == null || mapId == null) {
            return;
        }
        String key = buildKey(userId, mapId);
        try {
            stringRedisTemplate.delete(key);
            log.debug("Redis 已删除地图 JSON key={}", key);
        } catch (Exception e) {
            log.warn("Redis 删除地图 JSON 失败 userId={} mapId={}", userId, mapId, e);
        }
    }

    private static String buildKey(Long userId, Integer mapId) {
        return KEY_PREFIX + userId + ":" + mapId;
    }
}
