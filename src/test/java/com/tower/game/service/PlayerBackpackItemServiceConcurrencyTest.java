package com.tower.game.service;

import com.tower.game.common.dto.BackpackMoveRequest;
import com.tower.game.common.enums.BackpackMoveType;
import com.tower.game.mapper.PlayerBackpackItemMapper;
import com.tower.game.model.entity.Item;
import com.tower.game.model.entity.PlayerBackpackItem;
import com.tower.game.server.session.SessionState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerBackpackItemServiceConcurrencyTest {

    @Test
    void moveSerializesBackpackWritesForSamePlayer() throws Exception {
        PlayerBackpackItemMapper mapper = mock(PlayerBackpackItemMapper.class);
        PlayerBackpackSlotService slotService = mock(PlayerBackpackSlotService.class);
        BackpackUnlockOrderService unlockOrderService = mock(BackpackUnlockOrderService.class);
        ItemService itemService = mock(ItemService.class);
        MapLootCacheService lootCacheService = mock(MapLootCacheService.class);
        PlayerBackpackItemService service = new PlayerBackpackItemService(
                mapper, slotService, unlockOrderService, itemService, lootCacheService);

        Item item = new Item();
        item.setId(1001);
        item.setShapeType(1);
        item.setMaxStack(99);
        PlayerBackpackItem first = placement(1L, 1001, 0, 0);
        PlayerBackpackItem second = placement(2L, 1001, 1, 0);

        when(mapper.selectById(1L)).thenReturn(first);
        when(mapper.selectById(2L)).thenReturn(second);
        when(itemService.getById(1001)).thenReturn(item);
        when(slotService.getUnlockedCellKeys(eq(7L), eq(0))).thenReturn(Set.of("0,1", "1,1"));
        when(mapper.selectList(any())).thenReturn(List.of());

        CountDownLatch updateStarted = new CountDownLatch(2);
        AtomicInteger activeUpdates = new AtomicInteger();
        AtomicInteger maxActiveUpdates = new AtomicInteger();
        org.mockito.Mockito.when(mapper.updateById(any(PlayerBackpackItem.class))).thenAnswer(invocation -> {
            int active = activeUpdates.incrementAndGet();
            maxActiveUpdates.accumulateAndGet(active, Math::max);
            updateStarted.countDown();
            Thread.sleep(100);
            activeUpdates.decrementAndGet();
            return 1;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> service.move(7L, new SessionState(), moveRequest(1L, 0, 1)));
        executor.submit(() -> service.move(7L, new SessionState(), moveRequest(2L, 1, 1)));
        updateStarted.await(1, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertEquals(1, maxActiveUpdates.get());
    }

    private static PlayerBackpackItem placement(long id, int itemId, int row, int col) {
        PlayerBackpackItem placement = new PlayerBackpackItem();
        placement.setId(id);
        placement.setPlayerId(7L);
        placement.setSlotIndex(0);
        placement.setGridRow(row);
        placement.setGridCol(col);
        placement.setItemId(itemId);
        placement.setCount(1);
        return placement;
    }

    private static BackpackMoveRequest moveRequest(long placementId, int row, int col) {
        BackpackMoveRequest request = new BackpackMoveRequest();
        request.setMoveType(BackpackMoveType.BACKPACK_TO_BACKPACK);
        request.setPlacementId(placementId);
        request.setSlotIndex(0);
        request.setGridRow(row);
        request.setGridCol(col);
        return request;
    }
}
