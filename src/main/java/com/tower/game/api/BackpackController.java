package com.tower.game.api;

import com.tower.game.common.dto.BackpackItemPlacementVo;
import com.tower.game.common.dto.BackpackPutRequest;
import com.tower.game.common.dto.BackpackSlotVo;
import com.tower.game.common.dto.BackpackUnlockRequest;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.BackpackUnlockOrder;
import com.tower.game.model.entity.Item;
import com.tower.game.model.entity.PlayerBackpackItem;
import com.tower.game.service.ItemService;
import com.tower.game.service.PlayerBackpackItemService;
import com.tower.game.service.PlayerBackpackSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 背包接口
 */
@RestController
@RequestMapping("/api/backpack")
@RequiredArgsConstructor
public class BackpackController {

    private final PlayerBackpackSlotService playerBackpackSlotService;
    private final PlayerBackpackItemService playerBackpackItemService;
    private final ItemService itemService;

    /** 临时写死用户ID，后续改为从登录态获取 */
    private static final long DEFAULT_PLAYER_ID = 1L;

    /**
     * 查询玩家背包：5 个 slot 状态 + 每 slot 放置列表
     * POST /api/backpack/getBackpack（无入参，当前写死用户）
     */
    @PostMapping("/getBackpack")
    public ApiResponse<List<BackpackSlotVo>> getBackpack() {


        long playerId = DEFAULT_PLAYER_ID;
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

    /**
     * 放入背包
     * POST /api/backpack/put（当前写死用户，请求体无需传 playerId）
     */
    @PostMapping("/put")
    public ApiResponse<Void> put(@Valid @RequestBody BackpackPutRequest request) {
        long playerId = DEFAULT_PLAYER_ID;
         playerBackpackItemService.validateAndPut(
                playerId,
                request.getSlotIndex(),
                request.getGridRow(),
                request.getGridCol(),
                request.getItemId(),
                request.getCount());
        return ApiResponse.success(null);
    }

    /**
     * 按序解锁：将对应 slot 的 max_unlocked_order + 1（暂不实现金币消耗）
     * POST /api/backpack/unlock
     */
    @PostMapping("/unlock")
    public ApiResponse<Void> unlock(@Valid @RequestBody BackpackUnlockRequest request) {
        long playerId = DEFAULT_PLAYER_ID;
        playerBackpackSlotService.unlockNext(playerId, request.getSlotIndex());
        return ApiResponse.success(null);
    }
}
