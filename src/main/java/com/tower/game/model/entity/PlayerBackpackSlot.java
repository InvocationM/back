package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家背包位状态
 */
@Data
@TableName("player_backpack_slot")
public class PlayerBackpackSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("player_id")
    private Long playerId;

    @TableField("slot_index")
    private Integer slotIndex;

    @TableField("max_unlocked_order")
    private Integer maxUnlockedOrder;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
