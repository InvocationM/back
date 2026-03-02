package com.tower.game.common.dto.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗结束结果（WS 下发给客户端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleResultDto {
    private BattleResultType type;
    private int playerCurrentHp;
    private int monsterCurrentHp;
    private int totalRounds;
    @Builder.Default
    private List<DropItemDto> drops = new ArrayList<>();
    @Builder.Default
    private List<String> logs = new ArrayList<>();
}
