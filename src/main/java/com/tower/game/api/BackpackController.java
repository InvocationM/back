package com.tower.game.api;

import com.tower.game.common.auth.CurrentUserResolver;
import com.tower.game.common.dto.BackpackItemPlacementVo;
import com.tower.game.common.dto.BackpackMoveRequest;
import com.tower.game.common.dto.BackpackMoveResult;
import com.tower.game.common.dto.BackpackPickupMapCellRequest;
import com.tower.game.common.dto.BackpackSlotVo;
import com.tower.game.common.dto.BackpackUnlockRequest;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.BackpackUnlockOrder;
import com.tower.game.model.entity.Item;
import com.tower.game.model.entity.PlayerBackpackItem;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.server.session.SessionManager;
import com.tower.game.service.ItemService;
import com.tower.game.service.MapPotionPickupService;
import com.tower.game.service.PlayerBackpackItemService;
import com.tower.game.service.PlayerBackpackSlotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/backpack")
@RequiredArgsConstructor
public class BackpackController {

    private final PlayerBackpackSlotService playerBackpackSlotService;
    private final PlayerBackpackItemService playerBackpackItemService;
    private final ItemService itemService;
    private final SessionManager sessionManager;
    private final MapPotionPickupService mapPotionPickupService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/getBackpack")
    public ApiResponse<List<BackpackSlotVo>> getBackpack(HttpServletRequest httpRequest) {
        long playerId = currentUserResolver.requireUser(httpRequest).getUserId();
        List<BackpackSlotVo> slots = new ArrayList<>();

        for (int slotIndex = 0; slotIndex < 5; slotIndex++) {
            int maxOrder = playerBackpackSlotService.getMaxUnlockedOrder(playerId, slotIndex);
            BackpackUnlockOrder next = playerBackpackSlotService.getNextUnlock(playerId, slotIndex);
            Integer nextOrder = next == null ? null : next.getOrderNumber();
            int[] nextCell = next == null ? null : new int[]{next.getGridRow(), next.getGridCol()};
            boolean maxUnlocked = playerBackpackSlotService.isMaxUnlocked(playerId, slotIndex);

            List<PlayerBackpackItem> items = playerBackpackItemService.listByPlayerAndSlot(playerId, slotIndex);
            List<Integer> itemIds = items.stream().map(PlayerBackpackItem::getItemId).distinct().collect(Collectors.toList());
            Map<Integer, Item> itemMap = itemIds.isEmpty() ? Map.of()
                    : itemService.listByIds(itemIds).stream().collect(Collectors.toMap(Item::getId, i -> i));

            List<BackpackItemPlacementVo> placements = new ArrayList<>();
            for (PlayerBackpackItem p : items) {
                Item item = itemMap.get(p.getItemId());
                placements.add(BackpackItemPlacementVo.builder()
                        .placementId(p.getId())
                        .gridRow(p.getGridRow())
                        .gridCol(p.getGridCol())
                        .itemId(p.getItemId())
                        .itemName(item != null ? item.getName() : null)
                        .itemIcon(item != null ? item.getIcon() : null)
                        .shapeType(item != null ? item.getShapeType() : null)
                        .count(p.getCount())
                        .build());
            }

            slots.add(BackpackSlotVo.builder()
                    .slotIndex(slotIndex)
                    .maxUnlockedOrder(maxOrder)
                    .nextOrderNumber(nextOrder)
                    .nextCell(nextCell)
                    .maxUnlocked(maxUnlocked)
                    .items(placements)
                    .build());
        }
        return ApiResponse.success(slots);
    }

    @PostMapping("/move")
    public ApiResponse<BackpackMoveResult> move(@Valid @RequestBody BackpackMoveRequest request, HttpServletRequest httpRequest) {
        long playerId = currentUserResolver.requireUser(httpRequest).getUserId();
        PlayerSession session = sessionManager.getSessionByUserId(playerId);
        if (session == null) throw new BusinessException("玩家未在线");
        BackpackMoveResult result = playerBackpackItemService.move(playerId, session.authoritativeState(), request);
        return ApiResponse.success(result);
    }

    @PostMapping("/pickupMapCell")
    public ApiResponse<Void> pickupMapCell(@Valid @RequestBody BackpackPickupMapCellRequest request, HttpServletRequest httpRequest) {
        long playerId = currentUserResolver.requireUser(httpRequest).getUserId();
        PlayerSession session = sessionManager.getSessionByUserId(playerId);
        if (session == null) throw new BusinessException("玩家未在线");
        mapPotionPickupService.pickupFromMapCell(session, request.getCellX(), request.getCellY());
        return ApiResponse.success(null);
    }

    @PostMapping("/unlock")
    public ApiResponse<Void> unlock(@Valid @RequestBody BackpackUnlockRequest request, HttpServletRequest httpRequest) {
        long playerId = currentUserResolver.requireUser(httpRequest).getUserId();
        playerBackpackSlotService.unlockNext(playerId, request.getSlotIndex());
        return ApiResponse.success(null);
    }
}
