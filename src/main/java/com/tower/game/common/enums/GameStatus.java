package com.tower.game.common.enums;

import lombok.Getter;

/**
 * 游戏状态枚举
 */
@Getter
public enum GameStatus {
    IDLE(0, "空闲"),
    IN_GAME(1, "游戏中"),
    BATTLE(2, "战斗中"),
    PAUSED(3, "暂停");

    private final int code;
    private final String desc;

    GameStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
