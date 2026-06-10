package com.tower.game.service;

import com.tower.game.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionMapRedisService {

    private static final String KEY_PREFIX = "tower:map:";

    private final StringRedisTemplate stringRedisTemplate;
    private final BigMapRunRedisService bigMapRunRedisService;

    public String getMapJson(Long userId, Integer mapId) {
        if (userId == null || mapId == null) {
            return null;
        }
        String key = buildKey(userId, mapId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                stringRedisTemplate.expire(key, bigMapRunRedisService.ttl());
                bigMapRunRedisService.touchRun(userId);
            }
            return json;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis read map failed userId={} mapId={}", userId, mapId, e);
            throw new BusinessException(500, "地图缓存读取失败");
        }
    }

    public void saveMapJson(Long userId, Integer mapId, String mapJson) {
        if (userId == null || mapId == null || mapJson == null || mapJson.isBlank()) {
            return;
        }
        String key = buildKey(userId, mapId);
        try {
            stringRedisTemplate.opsForValue().set(key, mapJson, bigMapRunRedisService.ttl());
            bigMapRunRedisService.touchRun(userId);
            log.debug("Redis write map key={}", key);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis write map failed userId={} mapId={}", userId, mapId, e);
            throw new BusinessException(500, "地图缓存写入失败");
        }
    }

    public void deleteMapJson(Long userId, Integer mapId) {
        if (userId == null || mapId == null) {
            return;
        }
        String key = buildKey(userId, mapId);
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis delete map failed userId={} mapId={}", userId, mapId, e);
            throw new BusinessException(500, "地图缓存删除失败");
        }
    }

    private String buildKey(Long userId, Integer mapId) {
        return KEY_PREFIX + userId + ":" + bigMapRunRedisService.requireRunId(userId) + ":" + mapId;
    }
}
