package com.tower.game.common.dto.battle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条掉落（后端 roll 结果，供客户端表现）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropItemDto {
    private int itemId;
    private int count;
}
