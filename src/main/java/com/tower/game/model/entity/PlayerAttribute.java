package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家属性实体（战斗属性，与 player 一对一关联）
 */
@Data
@TableName("player_attribute")
public class PlayerAttribute {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 玩家ID */
    private Long playerId;

    /** 当前血量 */
    private Integer hp;

    /** 最大血量 */
    private Integer maxHp;

    /** 攻击力 */
    private Integer attack;

    /** 防御力 */
    private Integer defence;

    /** 闪避值 */
    private Integer dodge;

    /** 命中值 */
    private Integer accurate;

    /** 暴击值 */
    private Integer crit;

    /** 连击值 */
    private Integer doublehit;

    /** 反伤值 */
    private Integer reflect;

    /** 战斗显示名 */
    private String name;

    /** 战斗头像 */
    private String icon;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
