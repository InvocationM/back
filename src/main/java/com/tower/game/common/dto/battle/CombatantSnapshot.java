package com.tower.game.common.dto.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单方参战者快照（玩家或怪物），用于战斗计算
 * 百分比字段为 0~100，与 Unity 境界/怪物 10:1 换算一致（配置值 10 → 1%）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatantSnapshot {
    private String name;
    private String icon;
    private int attack;
    private int defence;
    private int maxHp;
    private int currentHp;
    /** 闪避百分比 0~100 */
    private double dodgePct;
    /** 命中百分比 0~100 */
    private double accuratePct;
    /** 暴击百分比 0~100 */
    private double critPct;
    /** 暴击伤害倍率（如 1.5） */
    private double critDamageMultiplier;
    /** 连击百分比 0~100 */
    private double doubleHitPct;
    /** 反伤百分比 0~100 */
    private double reflectPct;
}
