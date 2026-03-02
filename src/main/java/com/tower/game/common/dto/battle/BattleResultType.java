package com.tower.game.common.dto.battle;

/**
 * 战斗结果类型
 */
public enum BattleResultType {
    /** 玩家胜利 */
    Win,
    /** 玩家失败 */
    Lose,
    /** 超过最大回合数判负 */
    Timeout
}
