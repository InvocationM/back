package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 怪物实体（配置表，对应 Excel 怪物表）
 */
@Data
@TableName("monster")
public class Monster {

    /**
     * 怪物唯一标识符（对应Excel的ID列）
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 怪物名称（对应Excel的Name列）
     */
    private String name;

    /**
     * 怪物图标/精灵图资源名称（对应Excel的Icon列，如NPC1、NPC2、NPC3）
     * 用于从Resources加载对应的Sprite
     */
    private String icon;

    /**
     * 攻击力（对应Excel的attack列）
     */
    private Integer attack;

    /**
     * 防御力（对应Excel的defence列）
     */
    private Integer defence;

    /**
     * 最大生命值（对应Excel的maxhp列）
     */
    private Integer maxhp;

    /**
     * 闪避值（对应Excel的doge列）
     */
    private Integer doge;

    /**
     * 命中值（对应Excel的Accurate列）
     */
    private Integer accurate;

    /**
     * 暴击值（对应Excel的crit列）
     */
    private Integer crit;

    /**
     * 连击值（对应Excel的doublehit列）
     */
    private Integer doublehit;

    /**
     * 反伤值（对应Excel的Reflect列）
     */
    private Integer reflect;

    /**
     * 掉落物品配置字符串（对应Excel的Item列）
     * 格式：下划线分割，道具id_数量随机范围_掉落率（万分比），多条用分号分隔
     * 例如："3_1-1_10000" 表示道具3、数量1～1、100%掉落；"4_2-5_5000" 表示道具4、数量2～5、50%掉落
     */
    private String item;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
