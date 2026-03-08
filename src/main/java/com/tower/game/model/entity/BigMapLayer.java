package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大章节层实体（每章的 layers 项，options 存 JSON 数组）
 */
@Data
@TableName("big_map_layer")
public class BigMapLayer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属大章节ID
     */
    @TableField("big_map_id")
    private Integer bigMapId;

    /**
     * 层顺序，对应 layers 数组下标
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 该层选项数组 JSON，如 [1001, 1002, 1003]
     */
    private String options;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
