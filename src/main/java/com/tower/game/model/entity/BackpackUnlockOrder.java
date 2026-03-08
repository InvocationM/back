package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 背包解锁次序表（配置表）
 */
@Data
@TableName("backpack_unlock_order")
public class BackpackUnlockOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("slot_index")
    private Integer slotIndex;

    @TableField("order_number")
    private Integer orderNumber;

    @TableField("grid_row")
    private Integer gridRow;

    @TableField("grid_col")
    private Integer gridCol;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
