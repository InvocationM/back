package com.tower.game.service;

import com.tower.game.common.dto.bigmap.BigMapRunState;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BigMapRunRedisService {

    private static final String RUN_KEY_PREFIX = "tower:run:";
    private static final String MAP_KEY_PREFIX = "tower:map:";
    private static final String LOOT_KEY_PREFIX = "tower:loot:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    public Optional<BigMapRunState> getRun(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(runKey(userId));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(JsonUtil.parseObject(json, BigMapRunState.class));
        } catch (Exception e) {
            log.warn("Redis read run failed userId={}", userId, e);
            return Optional.empty();
        }
    }

    public void startNewRun(Long userId, BigMapRunState state) {
        if (userId == null || state == null) {
            return;
        }
        clearRunCaches(userId);
        state.setRunId(newRunId());
        saveRun(userId, state);
    }

    public void saveRun(Long userId, BigMapRunState state) {
        if (userId == null || state == null) {
            return;
        }
        if (state.getRunId() == null || state.getRunId().isBlank()) {
            state.setRunId(newRunId());
        }
        try {
            stringRedisTemplate.opsForValue().set(runKey(userId), JsonUtil.toJsonString(state), TTL);
        } catch (Exception e) {
            log.warn("Redis write run failed userId={}", userId, e);
        }
    }

    public void deleteRun(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            clearRunCaches(userId);
            stringRedisTemplate.delete(runKey(userId));
        } catch (Exception e) {
            log.warn("Redis delete run failed userId={}", userId, e);
        }
    }

    public BigMapRunState requireRun(Long userId) {
        return getRun(userId).orElseThrow(() -> new BusinessException(400, "请先开始章节"));
    }

    public String requireRunId(Long userId) {
        BigMapRunState run = requireRun(userId);
        if (run.getRunId() == null || run.getRunId().isBlank()) {
            throw new BusinessException(400, "章节运行态缺少 runId");
        }
        return run.getRunId();
    }

    public void touchRun(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.expire(runKey(userId), TTL);
        } catch (Exception e) {
            log.warn("Redis touch run failed userId={}", userId, e);
        }
    }

    public Duration ttl() {
        return TTL;
    }

    private void clearRunCaches(Long userId) {
        deleteByPattern(MAP_KEY_PREFIX + userId + ":*");
        deleteByPattern(LOOT_KEY_PREFIX + userId + ":*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private static String runKey(Long userId) {
        return RUN_KEY_PREFIX + userId;
    }

    private static String newRunId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
