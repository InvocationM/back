package com.tower.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tower.game.common.enums.ItemShapeType;
import com.tower.game.mapper.PlayerBackpackItemMapper;
import com.tower.game.model.entity.Item;
import com.tower.game.model.entity.PlayerBackpackItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 玩家背包放置服务（放入校验 + CRUD）
 */
@Service
@RequiredArgsConstructor
public class PlayerBackpackItemService {

    private final PlayerBackpackItemMapper playerBackpackItemMapper;
    private final PlayerBackpackSlotService playerBackpackSlotService;
    private final ItemService itemService;

    /**
     * 校验并放入：指定格放置或与同格同道具叠加
     *
     * @return 错误信息，null 表示可放入并已执行
     */
    @Transactional(rollbackFor = Exception.class)
    public String validateAndPut(Long playerId, int slotIndex, int gridRow, int gridCol, int itemId, int count) {
        if (count <= 0) return "数量必须大于0";

        Item item = itemService.getById(itemId);
        if (item == null) return "物品不存在";
        if (item.getShapeType() == null || item.getMaxStack() == null)
            return "物品未配置形态或叠加数";

        ItemShapeType shape = ItemShapeType.fromCode(item.getShapeType());
        if (shape == null) return "物品形态不合法";

        Set<String> unlocked = playerBackpackSlotService.getUnlockedCellKeys(playerId, slotIndex);
        Set<String> cover = cellsCovered(gridRow, gridCol, shape.getRows(), shape.getCols());
        for (String key : cover) {
            if (!unlocked.contains(key)) return "放置区域含未解锁格子";
        }

        List<PlayerBackpackItem> existing = listByPlayerAndSlot(playerId, slotIndex);
        Set<String> occupied = new HashSet<>();
        PlayerBackpackItem samePlace = null;
        for (PlayerBackpackItem e : existing) {
            Item eItem = itemService.getById(e.getItemId());
            if (eItem == null) continue;
            ItemShapeType eShape = ItemShapeType.fromCode(eItem.getShapeType());
            if (eShape == null) continue;
            Set<String> ec = cellsCovered(e.getGridRow(), e.getGridCol(), eShape.getRows(), eShape.getCols());
            occupied.addAll(ec);
            if (e.getGridRow().equals(gridRow) && e.getGridCol().equals(gridCol) && e.getItemId().equals(itemId))
                samePlace = e;
        }

        if (samePlace != null) {
            int newCount = samePlace.getCount() + count;
            if (newCount > item.getMaxStack()) return "超过该道具最大叠加数 " + item.getMaxStack();
            samePlace.setCount(newCount);
            playerBackpackItemMapper.updateById(samePlace);
            return null;
        }

        for (String key : cover) {
            if (occupied.contains(key)) return "与已有放置重叠";
        }
        if (count > item.getMaxStack()) return "超过该道具最大叠加数 " + item.getMaxStack();

        PlayerBackpackItem insert = new PlayerBackpackItem();
        insert.setPlayerId(playerId);
        insert.setSlotIndex(slotIndex);
        insert.setGridRow(gridRow);
        insert.setGridCol(gridCol);
        insert.setItemId(itemId);
        insert.setCount(count);
        playerBackpackItemMapper.insert(insert);
        return null;
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
}
