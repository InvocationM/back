package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.battle.BattleResultType;
import com.tower.game.common.enums.GameStatus;
import com.tower.game.common.dto.battle.BattleResultDto;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.BattleEngineService;
import com.tower.game.service.DropRollService;
import com.tower.game.service.MonsterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 战斗开始处理器：收到 BATTLE_START(3001)，从上下文取玩家、查库取怪物，执行战斗，胜利时后端 roll 掉落，回发 BATTLE_RESULT(3003)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleStartProcessor implements MessageProcessor {

    private static final int MAX_ROUNDS = 50;

    private final BattleEngineService battleEngineService;
    private final MonsterService monsterService;
    private final DropRollService dropRollService;

    @Override
    public void handle(PlayerSession session, Object message) {
        if (!(message instanceof Map)) {
            sendFail(session, "消息格式错误");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) message;
        Integer monsterId = getInt(msg, "monsterId");
        Integer cellX = getInt(msg, "cellX");
        Integer cellY = getInt(msg, "cellY");
        if (monsterId == null) {
            sendFail(session, "缺少 monsterId");
            return;
        }
        if (session.getGameStatus() == GameStatus.BATTLE) {
            sendFail(session, "已在战斗中");
            return;
        }

        var monster = monsterService.getById(monsterId);
        if (monster == null) {
            sendFail(session, "未找到怪物");
            return;
        }

        session.setGameStatus(GameStatus.BATTLE);
        var playerSnapshot = battleEngineService.buildPlayerSnapshot(session.getState());
        var monsterSnapshot = battleEngineService.buildMonsterSnapshot(monster);
        BattleResultDto result = battleEngineService.run(playerSnapshot, monsterSnapshot, MAX_ROUNDS);

        session.setHp(result.getPlayerCurrentHp());
        session.setGameStatus(GameStatus.IN_GAME);

        if (result.getType() == BattleResultType.Win) {
            if (cellX != null && cellY != null) {
                session.setCellX(cellX);
                session.setCellY(cellY);
            }
        }
        if (result.getType() == BattleResultType.Win && monster.getItem() != null && !monster.getItem().isBlank()) {
            result.setDrops(dropRollService.parseAndRoll(monster.getItem()));
        }
        if (result.getDrops() == null) {
            result.setDrops(java.util.Collections.emptyList());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", MessageType.BATTLE_RESULT);
        response.put("code", 200);
        response.put("result", toResultMap(result));
        response.put("cellX", cellX != null ? cellX : -1);
        response.put("cellY", cellY != null ? cellY : -1);
        response.put("logs", result.getLogs());
        session.sendMessage(response);
        log.debug("战斗结束: userId={}, monsterId={}, type={}", session.getUserId(), monsterId, result.getType());
    }

    @Override
    public int getMessageType() {
        return MessageType.BATTLE_START;
    }

    private Map<String, Object> toResultMap(BattleResultDto r) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", r.getType().name());
        m.put("playerCurrentHp", r.getPlayerCurrentHp());
        m.put("totalRounds", r.getTotalRounds());
        m.put("drops", r.getDrops().stream()
                .map(d -> {
                    Map<String, Object> e = new HashMap<>();
                    e.put("itemId", d.getItemId());
                    e.put("count", d.getCount());
                    return e;
                })
                .toList());
        return m;
    }

    private void sendFail(PlayerSession session, String message) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.BATTLE_RESULT);
        fail.put("code", 400);
        fail.put("message", message);
        session.sendMessage(fail);
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
