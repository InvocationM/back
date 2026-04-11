package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品实体（配置表，装备/道具等）
 */
@Data
@TableName("item")
public class Item {

    /**
     * 物品唯一标识符
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 物品名称
     */
    private String name;

    /**
     * 图标资源标识
     */
    private String icon;

    /**
     * 物品类型：1装备 2宝石 3钥匙 4血瓶
     */
    private Integer type;

    /**
     * 物品子类型
     */
    @TableField("sub_type")
    private Integer subType;

    /**
     * 背包占格形态 1～9（见 ItemShapeType）
     */
    @TableField("shape_type")
    private Integer shapeType;

    /**
     * 最大叠加数量
     */
    @TableField("max_stack")
    private Integer maxStack;

    /**
     * 攻击力
     */
    private Integer attack;

    /**
     * 防御力
     */
    private Integer defence;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
