package com.tower.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tower.game.common.dto.BackpackMoveRequest;
import com.tower.game.common.dto.map.MapCachedItem;
import com.tower.game.common.enums.BackpackMoveType;
import com.tower.game.common.enums.ItemShapeType;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.mapper.PlayerBackpackItemMapper;
import com.tower.game.model.entity.BackpackUnlockOrder;
import com.tower.game.model.entity.Item;
import com.tower.game.model.entity.PlayerBackpackItem;
import com.tower.game.server.session.SessionState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 玩家背包放置服务（移动校验 + CRUD）
 */
@Service
@RequiredArgsConstructor
public class PlayerBackpackItemService {

    private final PlayerBackpackItemMapper playerBackpackItemMapper;
    private final PlayerBackpackSlotService playerBackpackSlotService;
    private final BackpackUnlockOrderService backpackUnlockOrderService;
    private final ItemService itemService;

    /**
     * 统一移动入口，按 moveType 分发
     */
    @Transactional(rollbackFor = Exception.class)
    public void move(Long playerId, SessionState state, BackpackMoveRequest request) {
        switch (request.getMoveType()) {
            case MAP_TO_BACKPACK -> moveFromMap(playerId, state, request);
            case BACKPACK_TO_BACKPACK -> moveInBackpack(playerId, request);
            case BACKPACK_TO_MAP -> moveToMap(playerId, state, request);
        }
    }

    /**
     * 地图缓存 → 背包
     */
    private void moveFromMap(Long playerId, SessionState state, BackpackMoveRequest request) {
        if (request.getCachedItemId() == null) throw new BusinessException("cachedItemId 不能为空");
        requireTarget(request);

        MapCachedItem cached = state.findCachedItem(request.getCachedItemId());
        if (cached == null) throw new BusinessException("地图物品不存在或已被拾取");

        Item item = itemService.getById(cached.getItemId());
        if (item == null) throw new BusinessException("物品不存在");

        placeIntoBackpack(playerId, request.getSlotIndex(), request.getGridRow(), request.getGridCol(),
                item, cached.getCount(), null);

        state.removeCachedItem(request.getCachedItemId());
    }

    /**
     * 背包 → 背包（移动位置）
     */
    private void moveInBackpack(Long playerId, BackpackMoveRequest request) {
        if (request.getPlacementId() == null) throw new BusinessException("placementId 不能为空");
        requireTarget(request);

        PlayerBackpackItem placement = playerBackpackItemMapper.selectById(request.getPlacementId());
        if (placement == null || !placement.getPlayerId().equals(playerId))
            throw new BusinessException("背包物品不存在");

        int slotIndex = request.getSlotIndex();
        int gridRow = request.getGridRow();
        int gridCol = request.getGridCol();

        // 位置没变，直接返回
        if (placement.getSlotIndex() == slotIndex
                && placement.getGridRow() == gridRow
                && placement.getGridCol() == gridCol) {
            return;
        }

        Item item = itemService.getById(placement.getItemId());
        if (item == null) throw new BusinessException("物品不存在");

        // 检查目标位置是否有同物品可叠加
        PlayerBackpackItem mergeTarget = findMergeTarget(playerId, slotIndex, gridRow, gridCol,
                item, placement.getId());

        if (mergeTarget != null) {
            // 叠加到目标位置，删除原记录
            int newCount = mergeTarget.getCount() + placement.getCount();
            if (newCount > item.getMaxStack())
                throw new BusinessException("超过该道具最大叠加数 " + item.getMaxStack());
            mergeTarget.setCount(newCount);
            playerBackpackItemMapper.updateById(mergeTarget);
            playerBackpackItemMapper.deleteById(placement.getId());
        } else {
            // 校验目标位置（排除自身占格）
            validatePosition(playerId, slotIndex, gridRow, gridCol, item, placement.getId());
            // 更新位置
            placement.setSlotIndex(slotIndex);
            placement.setGridRow(gridRow);
            placement.setGridCol(gridCol);
            playerBackpackItemMapper.updateById(placement);
        }
    }

    /**
     * 背包 → 地图缓存
     */
    private void moveToMap(Long playerId, SessionState state, BackpackMoveRequest request) {
        if (request.getPlacementId() == null) throw new BusinessException("placementId 不能为空");

        PlayerBackpackItem placement = playerBackpackItemMapper.selectById(request.getPlacementId());
        if (placement == null || !placement.getPlayerId().equals(playerId))
            throw new BusinessException("背包物品不存在");

        state.addItemToMap(placement.getItemId(), placement.getCount());
        playerBackpackItemMapper.deleteById(placement.getId());
    }

    /**
     * 仅默认背包位 slot=0：按解锁序号优先找可叠加锚点，否则找首个可放置空位。
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoPlaceInDefaultBackpack(Long playerId, Item item, int count) {
        validateItemConfig(item);
        if (count <= 0) throw new BusinessException("数量必须大于0");
        int slotIndex = 0;
        int maxOrder = playerBackpackSlotService.getMaxUnlockedOrder(playerId, slotIndex);
        for (int order = 1; order <= maxOrder; order++) {
            BackpackUnlockOrder unlock = backpackUnlockOrderService.getBySlotAndOrder(slotIndex, order);
            if (unlock == null || unlock.getGridRow() == null || unlock.getGridCol() == null) continue;
            int gr = unlock.getGridRow();
            int gc = unlock.getGridCol();
            PlayerBackpackItem mergeTarget = findMergeTarget(playerId, slotIndex, gr, gc, item, null);
            if (mergeTarget != null && mergeTarget.getCount() + count <= item.getMaxStack()) {
                placeIntoBackpack(playerId, slotIndex, gr, gc, item, count, null);
                return;
            }
        }
        for (int order = 1; order <= maxOrder; order++) {
            BackpackUnlockOrder unlock = backpackUnlockOrderService.getBySlotAndOrder(slotIndex, order);
            if (unlock == null || unlock.getGridRow() == null || unlock.getGridCol() == null) continue;
            int gr = unlock.getGridRow();
            int gc = unlock.getGridCol();
            if (canPlaceAt(playerId, slotIndex, gr, gc, item, null)) {
                placeIntoBackpack(playerId, slotIndex, gr, gc, item, count, null);
                return;
            }
        }
        throw new BusinessException("默认背包无可用位置");
    }

    private boolean canPlaceAt(Long playerId, int slotIndex, int gridRow, int gridCol,
                               Item item, Long excludePlacementId) {
        try {
            validatePosition(playerId, slotIndex, gridRow, gridCol, item, excludePlacementId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 放入背包（地图→背包专用）：校验 + 插入或叠加
     */
    private void placeIntoBackpack(Long playerId, int slotIndex, int gridRow, int gridCol,
                                   Item item, int count, Long excludePlacementId) {
        if (count <= 0) throw new BusinessException("数量必须大于0");
        validateItemConfig(item);
        ItemShapeType shape = ItemShapeType.fromCode(item.getShapeType());

        // 解锁检查
        checkUnlocked(playerId, slotIndex, gridRow, gridCol, shape);

        // 查找同位置同物品是否可叠加
        PlayerBackpackItem mergeTarget = findMergeTarget(playerId, slotIndex, gridRow, gridCol,
                item, excludePlacementId);

        if (mergeTarget != null) {
            int newCount = mergeTarget.getCount() + count;
            if (newCount > item.getMaxStack())
                throw new BusinessException("超过该道具最大叠加数 " + item.getMaxStack());
            mergeTarget.setCount(newCount);
            playerBackpackItemMapper.updateById(mergeTarget);
            return;
        }

        // 重叠检查
        validatePosition(playerId, slotIndex, gridRow, gridCol, item, excludePlacementId);
        if (count > item.getMaxStack())
            throw new BusinessException("超过该道具最大叠加数 " + item.getMaxStack());

        PlayerBackpackItem insert = new PlayerBackpackItem();
        insert.setPlayerId(playerId);
        insert.setSlotIndex(slotIndex);
        insert.setGridRow(gridRow);
        insert.setGridCol(gridCol);
        insert.setItemId(item.getId());
        insert.setCount(count);
        playerBackpackItemMapper.insert(insert);
    }

    /** 校验物品配置 */
    private void validateItemConfig(Item item) {
        if (item.getShapeType() == null || item.getMaxStack() == null)
            throw new BusinessException("物品未配置形态或叠加数");
        if (ItemShapeType.fromCode(item.getShapeType()) == null)
            throw new BusinessException("物品形态不合法");
    }

    /** 校验放置区域是否已解锁 */
    private void checkUnlocked(Long playerId, int slotIndex, int gridRow, int gridCol, ItemShapeType shape) {
        Set<String> unlocked = playerBackpackSlotService.getUnlockedCellKeys(playerId, slotIndex);
        Set<String> cover = cellsCovered(gridRow, gridCol, shape.getRows(), shape.getCols());
        for (String key : cover) {
            if (!unlocked.contains(key)) throw new BusinessException("放置区域含未解锁格子");
        }
    }

    /** 查找同位置同物品的叠加目标 */
    private PlayerBackpackItem findMergeTarget(Long playerId, int slotIndex, int gridRow, int gridCol,
                                               Item item, Long excludePlacementId) {
        List<PlayerBackpackItem> existing = listByPlayerAndSlot(playerId, slotIndex);
        for (PlayerBackpackItem e : existing) {
            if (excludePlacementId != null && e.getId().equals(excludePlacementId)) continue;
            if (e.getGridRow().equals(gridRow) && e.getGridCol().equals(gridCol)
                    && e.getItemId().equals(item.getId())) {
                return e;
            }
        }
        return null;
    }

    /** 校验目标位置不与已有放置重叠 */
    private void validatePosition(Long playerId, int slotIndex, int gridRow, int gridCol,
                                  Item item, Long excludePlacementId) {
        validateItemConfig(item);
        ItemShapeType shape = ItemShapeType.fromCode(item.getShapeType());

        checkUnlocked(playerId, slotIndex, gridRow, gridCol, shape);

        Set<String> cover = cellsCovered(gridRow, gridCol, shape.getRows(), shape.getCols());
        List<PlayerBackpackItem> existing = listByPlayerAndSlot(playerId, slotIndex);
        Set<String> occupied = new HashSet<>();
        for (PlayerBackpackItem e : existing) {
            if (excludePlacementId != null && e.getId().equals(excludePlacementId)) continue;
            Item eItem = itemService.getById(e.getItemId());
            if (eItem == null) continue;
            ItemShapeType eShape = ItemShapeType.fromCode(eItem.getShapeType());
            if (eShape == null) continue;
            occupied.addAll(cellsCovered(e.getGridRow(), e.getGridCol(), eShape.getRows(), eShape.getCols()));
        }
        for (String key : cover) {
            if (occupied.contains(key)) throw new BusinessException("与已有放置重叠");
        }
    }

    /** 校验目标坐标必传 */
    private void requireTarget(BackpackMoveRequest request) {
        if (request.getSlotIndex() == null) throw new BusinessException("slotIndex 不能为空");
        if (request.getGridRow() == null) throw new BusinessException("gridRow 不能为空");
        if (request.getGridCol() == null) throw new BusinessException("gridCol 不能为空");
    }

    /** 占格覆盖的 (row,col) 集合，key 为 "row,col" */
    private Set<String> cellsCovered(int startRow, int startCol, int rows, int cols) {
        Set<String> set = new HashSet<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                set.add((startRow + r) + "," + (startCol + c));
            }
        }
        return set;
    }

    public List<PlayerBackpackItem> listByPlayerAndSlot(Long playerId, int slotIndex) {
        LambdaQueryWrapper<PlayerBackpackItem> q = new LambdaQueryWrapper<>();
        q.eq(PlayerBackpackItem::getPlayerId, playerId).eq(PlayerBackpackItem::getSlotIndex, slotIndex);
        return playerBackpackItemMapper.selectList(q);
    }

    public List<PlayerBackpackItem> listByPlayer(Long playerId) {
        LambdaQueryWrapper<PlayerBackpackItem> q = new LambdaQueryWrapper<>();
        q.eq(PlayerBackpackItem::getPlayerId, playerId);
        return playerBackpackItemMapper.selectList(q);
    }

    /**
     * 消耗 1 把可开 {@code doorId} 之门的钥匙（物品 type=7，且 id 或 sub_type 与门一致）；按 slot、格子序取第一个有数量的放置。
     */
    @Transactional(rollbackFor = Exception.class)
    public void consumeOneKeyOpeningDoor(Long playerId, int doorId) {
        List<PlayerBackpackItem> placements = listByPlayer(playerId);
        placements.sort(Comparator
                .comparing(PlayerBackpackItem::getSlotIndex)
                .thenComparing(PlayerBackpackItem::getGridRow)
                .thenComparing(PlayerBackpackItem::getGridCol)
                .thenComparing(PlayerBackpackItem::getId));
        for (PlayerBackpackItem p : placements) {
            Item item = itemService.getById(p.getItemId());
            if (item == null || !ItemService.keyOpensDoor(item, doorId)) {
                continue;
            }
            int count = p.getCount() == null ? 0 : p.getCount();
            if (count < 1) {
                continue;
            }
            int next = count - 1;
            if (next <= 0) {
                playerBackpackItemMapper.deleteById(p.getId());
            } else {
                p.setCount(next);
                playerBackpackItemMapper.updateById(p);
            }
            return;
        }
        throw new BusinessException("背包中没有可打开此门的钥匙");
    }
}
