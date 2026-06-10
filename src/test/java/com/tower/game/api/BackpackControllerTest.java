package com.tower.game.api;

import com.tower.game.common.auth.CurrentUser;
import com.tower.game.common.auth.CurrentUserResolver;
import com.tower.game.common.dto.BackpackMoveRequest;
import com.tower.game.common.dto.BackpackMoveResult;
import com.tower.game.common.dto.bigmap.BigMapRunState;
import com.tower.game.common.enums.BackpackMoveType;
import com.tower.game.server.session.SessionManager;
import com.tower.game.server.session.SessionState;
import com.tower.game.service.BigMapRunRedisService;
import com.tower.game.service.ItemService;
import com.tower.game.service.MapPotionPickupService;
import com.tower.game.service.PlayerBackpackItemService;
import com.tower.game.service.PlayerBackpackSlotService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackpackControllerTest {

    @Test
    void moveUsesRedisRunStateWhenWebSocketSessionIsMissing() {
        PlayerBackpackSlotService slotService = mock(PlayerBackpackSlotService.class);
        PlayerBackpackItemService itemPlacementService = mock(PlayerBackpackItemService.class);
        ItemService itemService = mock(ItemService.class);
        SessionManager sessionManager = mock(SessionManager.class);
        MapPotionPickupService pickupService = mock(MapPotionPickupService.class);
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        BigMapRunRedisService runRedisService = mock(BigMapRunRedisService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(currentUserResolver.requireUser(httpRequest)).thenReturn(new CurrentUser(1L, "u"));
        when(sessionManager.getSessionByUserId(1L)).thenReturn(null);
        when(runRedisService.requireRun(1L)).thenReturn(BigMapRunState.builder()
                .currentMapId(1001)
                .cellX(4)
                .cellY(5)
                .hp(66)
                .build());
        when(itemPlacementService.move(eq(1L), any(SessionState.class), any(BackpackMoveRequest.class)))
                .thenReturn(BackpackMoveResult.none());

        BackpackController controller = new BackpackController(
                slotService,
                itemPlacementService,
                itemService,
                sessionManager,
                pickupService,
                currentUserResolver,
                runRedisService);

        BackpackMoveRequest request = new BackpackMoveRequest();
        request.setMoveType(BackpackMoveType.BACKPACK_TO_MAP);
        request.setPlacementId(9L);

        controller.move(request, httpRequest);

        ArgumentCaptor<SessionState> stateCaptor = ArgumentCaptor.forClass(SessionState.class);
        verify(itemPlacementService).move(eq(1L), stateCaptor.capture(), eq(request));
        SessionState state = stateCaptor.getValue();
        assertEquals(1001, state.getMapId());
        assertEquals(4, state.getCellX());
        assertEquals(5, state.getCellY());
        assertEquals(66, state.getHp());
    }

    @Test
    void backpackToBackpackMoveDoesNotRequireWebSocketSessionOrRunState() {
        PlayerBackpackSlotService slotService = mock(PlayerBackpackSlotService.class);
        PlayerBackpackItemService itemPlacementService = mock(PlayerBackpackItemService.class);
        ItemService itemService = mock(ItemService.class);
        SessionManager sessionManager = mock(SessionManager.class);
        MapPotionPickupService pickupService = mock(MapPotionPickupService.class);
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        BigMapRunRedisService runRedisService = mock(BigMapRunRedisService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(currentUserResolver.requireUser(httpRequest)).thenReturn(new CurrentUser(1L, "u"));
        when(sessionManager.getSessionByUserId(1L)).thenReturn(null);
        when(itemPlacementService.move(eq(1L), any(SessionState.class), any(BackpackMoveRequest.class)))
                .thenReturn(BackpackMoveResult.none());

        BackpackController controller = new BackpackController(
                slotService,
                itemPlacementService,
                itemService,
                sessionManager,
                pickupService,
                currentUserResolver,
                runRedisService);

        BackpackMoveRequest request = new BackpackMoveRequest();
        request.setMoveType(BackpackMoveType.BACKPACK_TO_BACKPACK);
        request.setPlacementId(9L);
        request.setSlotIndex(0);
        request.setGridRow(1);
        request.setGridCol(2);

        controller.move(request, httpRequest);

        verify(itemPlacementService).move(eq(1L), any(SessionState.class), eq(request));
        verifyNoInteractions(runRedisService);
    }
}
