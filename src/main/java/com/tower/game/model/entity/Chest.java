package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝箱实体（配置表）
 */
@Data
@TableName("chest")
public class Chest {

    /**
     * 宝箱唯一标识符
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 宝箱名称
     */
    private String name;

    /**
     * 图标资源名，可选
     */
    private String icon;

    /**
     * 奖励字符串，格式：道具id_数量随机范围_掉落率（万分比），如 "1_1-1_10000;2_1-2_5000"
     */
    private String rewards;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
