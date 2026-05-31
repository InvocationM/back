package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.battle.BattleResultDto;
import com.tower.game.common.dto.battle.BattleResultType;
import com.tower.game.common.dto.map.MapLootCache;
import com.tower.game.common.enums.GameStatus;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.model.entity.PlayerAttribute;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.BattleEngineService;
import com.tower.game.service.BigMapRunRedisService;
import com.tower.game.service.MapJsonCellService;
import com.tower.game.service.MapLootCacheService;
import com.tower.game.service.MapWalkableService;
import com.tower.game.service.MonsterService;
import com.tower.game.service.PlayerAttributeService;
import com.tower.game.service.SessionMapRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleStartProcessor implements MessageProcessor {

    private static final int MAX_ROUNDS = 50;
    private static final int EVENT_TYPE_MONSTER = 5;

    private final BattleEngineService battleEngineService;
    private final MonsterService monsterService;
    private final MapWalkableService mapWalkableService;
    private final PlayerAttributeService playerAttributeService;
    private final SessionMapRedisService sessionMapRedisService;
    private final MapLootCacheService mapLootCacheService;
    private final BigMapRunRedisService bigMapRunRedisService;
    private final MapJsonCellService mapJsonCellService;

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
        if (monsterId == null || cellX == null || cellY == null) {
            sendFail(session, "缺少 monsterId 或 cellX/cellY");
            return;
        }
        if (session.getGameStatus() == GameStatus.BATTLE) {
            sendFail(session, "已在战斗中");
            return;
        }
        if (session.getMapId() == null || !session.hasPosition()) {
            sendFail(session, "未在有效地图位置");
            return;
        }
        int px = session.getCellX();
        int py = session.getCellY();
        if (Math.abs(px - cellX) + Math.abs(py - cellY) != 1) {
            sendFail(session, "不在怪物相邻格");
            return;
        }

        String mapData = sessionMapRedisService.getMapJson(session.getUserId(), session.getMapId());
        if (mapData == null || mapData.isBlank()) {
            throw new BusinessException(500, "地图缓存不存在，请先加载地图");
        }
        int[] cellEvent = mapWalkableService.getCellEvent(session.getMapId(), cellX, cellY, mapData);
        if (cellEvent == null || cellEvent[0] != EVENT_TYPE_MONSTER || cellEvent[1] != monsterId) {
            sendFail(session, "该格无此怪物或非怪物格");
            return;
        }

        var monster = monsterService.getById(monsterId);
        if (monster == null) {
            sendFail(session, "未找到怪物");
            return;
        }

        session.setGameStatus(GameStatus.BATTLE);
        BattleResultDto result = battleEngineService.run(
                battleEngineService.buildPlayerSnapshot(session.getState()),
                battleEngineService.buildMonsterSnapshot(monster),
                MAX_ROUNDS);

        session.setHp(result.getPlayerCurrentHp());
        session.setGameStatus(GameStatus.IN_GAME);
        bigMapRunRedisService.getRun(session.getUserId()).ifPresent(run -> {
            run.setHp(result.getPlayerCurrentHp());
            bigMapRunRedisService.saveRun(session.getUserId(), run);
        });

        PlayerAttribute attr = playerAttributeService.getByPlayerId(session.getUserId());
        if (attr != null) {
            attr.setHp(result.getPlayerCurrentHp());
            playerAttributeService.updateById(attr);
        }

        if (result.getDrops() == null) {
            result.setDrops(java.util.Collections.emptyList());
        }

        String mapCacheId = null;
        if (result.getType() == BattleResultType.Win) {
            var cleared = mapJsonCellService.clearEventFromCell(mapData, cellX, cellY, EVENT_TYPE_MONSTER, monsterId);
            sessionMapRedisService.saveMapJson(session.getUserId(), session.getMapId(), cleared.newMapJson());

            if (monster.getItem() != null && !monster.getItem().isBlank()) {
                MapLootCache lootCache = new MapLootCache();
                lootCache.setCellX(cellX);
                lootCache.setCellY(cellY);
                lootCache.setSourceType("CORPSE");
                lootCache.setSourceId(monsterId);
                lootCache.setPendingItemConfig(monster.getItem());
                mapCacheId = mapLootCacheService.createLoot(session.getUserId(), lootCache);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", MessageType.BATTLE_RESULT);
        response.put("code", 200);
        response.put("data", buildBattleResultData(result, monsterId, cellX, cellY, mapCacheId));
        session.sendMessage(response);
    }

    @Override
    public int getMessageType() {
        return MessageType.BATTLE_START;
    }

    private Map<String, Object> buildBattleResultData(BattleResultDto result, int monsterId, int cellX, int cellY,
                                                      String mapCacheId) {
        Map<String, Object> data = new HashMap<>();
        data.put("outcome", result.getType().name());
        data.put("logs", result.getLogs() != null ? result.getLogs() : List.of());
        data.put("stats", Map.of(
                "playerCurrentHp", result.getPlayerCurrentHp(),
                "monsterCurrentHp", result.getMonsterCurrentHp(),
                "totalRounds", result.getTotalRounds()));
        data.put("target", Map.of("monsterId", monsterId, "cell", Map.of("x", cellX, "y", cellY)));
        data.put("rewardChest", mapCacheId == null ? null : Map.of(
                "mapCacheId", mapCacheId,
                "cell", Map.of("x", cellX, "y", cellY),
                "sourceType", "CORPSE"));
        return data;
    }

    private void sendFail(PlayerSession session, String message) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("type", MessageType.BATTLE_RESULT);
        fail.put("code", 400);
        fail.put("message", message);
        session.sendMessage(fail);
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
