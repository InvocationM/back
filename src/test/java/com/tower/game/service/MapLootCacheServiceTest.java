package com.tower.game.service;

import com.tower.game.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapLootCacheServiceTest {

    @Test
    void addItemAtCellFailsWhenRedisWriteFails() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        BigMapRunRedisService runRedisService = mock(BigMapRunRedisService.class);

        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(null);
        when(runRedisService.requireRunId(1L)).thenReturn("run-1");
        when(runRedisService.ttl()).thenReturn(java.time.Duration.ofHours(24));
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(values).set(anyString(), anyString(), any());

        MapLootCacheService service = new MapLootCacheService(redis, runRedisService);

        assertThrows(BusinessException.class,
                () -> service.addItemAtCellWithResult(1L, 3, 4, 1001, 1));
    }
}
