package com.tower.game.service;

import com.tower.game.common.dto.bigmap.BigMapRunState;
import com.tower.game.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 大章节闯关进度 Redis：与 {@link SessionMapRedisService} 键风格一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BigMapRunRedisService {

    private static final String KEY_PREFIX = "tower:session:bigmap:run:";

    private final StringRedisTemplate stringRedisTemplate;

    public Optional<BigMapRunState> getRun(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + userId;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            BigMapRunState state = JsonUtil.parseObject(json, BigMapRunState.class);
            return Optional.ofNullable(state);
        } catch (Exception e) {
            log.warn("Redis 读取 bigmap run 失败 userId={}", userId, e);
            return Optional.empty();
        }
    }

    public void saveRun(Long userId, BigMapRunState state) {
        if (userId == null || state == null) {
            return;
        }
        String key = KEY_PREFIX + userId;
        try {
            String json = JsonUtil.toJsonString(state);
            stringRedisTemplate.opsForValue().set(key, json);
            log.debug("Redis 已写入 bigmap run key={}", key);
        } catch (Exception e) {
            log.warn("Redis 写入 bigmap run 失败 userId={}", userId, e);
        }
    }

    public void deleteRun(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis 删除 bigmap run 失败 userId={}", userId, e);
        }
    }
}
