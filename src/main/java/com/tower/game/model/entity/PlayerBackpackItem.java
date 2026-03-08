package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家背包放置记录（每个占格放置一条）
 */
@Data
@TableName("player_backpack_item")
public class PlayerBackpackItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("player_id")
    private Long playerId;

    @TableField("slot_index")
    private Integer slotIndex;

    @TableField("grid_row")
    private Integer gridRow;

    @TableField("grid_col")
    private Integer gridCol;

    @TableField("item_id")
    private Integer itemId;

    private Integer count;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
