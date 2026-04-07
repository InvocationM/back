package com.tower.game.server.processor;

import com.tower.game.common.constant.MessageType;
import com.tower.game.common.dto.battle.BattleResultDto;
import com.tower.game.common.dto.battle.BattleResultType;
import com.tower.game.common.enums.GameStatus;
import com.tower.game.model.entity.PlayerAttribute;
import com.tower.game.server.session.PlayerSession;
import com.tower.game.service.BattleEngineService;
import com.tower.game.service.DropRollService;
import com.tower.game.service.MapWalkableService;
import com.tower.game.service.MonsterService;
import com.tower.game.service.PlayerAttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.tower.game.common.dto.map.MapCachedItem;
import com.tower.game.common.dto.map.MapLootCache;
import com.tower.game.server.session.SessionState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 战斗开始处理器（方案 A）：收到 BATTLE_START(3001)，校验玩家在怪物格相邻、该格确有该怪，执行战斗并回发 BATTLE_RESULT(3003)。
 * 胜利后不更新玩家位置到怪物格，保持在与怪物相邻格。
 * 胜利掉落：3003 中 result.drops 恒为空；客户端用 lootChest + 4001 开箱查看物品并入包。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleStartProcessor implements MessageProcessor {

    private static final int MAX_ROUNDS = 50;
    private static final int EVENT_TYPE_MONSTER = 5;

    private final BattleEngineService battleEngineService;
    private final MonsterService monsterService;
    private final DropRollService dropRollService;
    private final MapWalkableService mapWalkableService;
    private final PlayerAttributeService playerAttributeService;

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
            log.debug("3001 校验失败: 未在有效地图位置");
            sendFail(session, "未在有效地图位置");
            return;
        }
        int px = session.getCellX();
        int py = session.getCellY();
        if (Math.abs(px - cellX) + Math.abs(py - cellY) != 1) {
            log.debug("3001 校验失败: 不在怪物相邻格 player=({},{}) monsterCell=({},{})", px, py, cellX, cellY);
            sendFail(session, "不在怪物相邻格");
            return;
        }
        int[] cellEvent = mapWalkableService.getCellEvent(session.getMapId(), cellX, cellY, session.getCurrentMapData());
        if (cellEvent == null || cellEvent[0] != EVENT_TYPE_MONSTER || cellEvent[1] != monsterId) {
            log.debug("3001 校验失败: 该格无此怪物 mapId={} cell=({},{}) monsterId={}", session.getMapId(), cellX, cellY, monsterId);
            sendFail(session, "该格无此怪物或非怪物格");
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

        // HP 回写数据库
        PlayerAttribute attr = playerAttributeService.getByPlayerId(session.getUserId());
        if (attr != null) {
            attr.setHp(result.getPlayerCurrentHp());
            playerAttributeService.updateById(attr);
        }
        // 方案 A：胜利后不更新玩家到怪物格，保持在与怪物相邻格

        if (result.getType() == BattleResultType.Win && monster.getItem() != null && !monster.getItem().isBlank()) {
            result.setDrops(dropRollService.parseAndRoll(monster.getItem()));
        }
        if (result.getDrops() == null) {
            result.setDrops(java.util.Collections.emptyList());
        }

        // 战斗胜利且有掉落：缓存到 SessionState
        String mapCacheId = null;
        if (result.getType() == BattleResultType.Win && !result.getDrops().isEmpty()) {
            SessionState state = session.getState();
            mapCacheId = state.nextMapCacheId();

            MapLootCache lootCache = new MapLootCache();
            lootCache.setMapCacheId(mapCacheId);
            lootCache.setCellX(cellX);
            lootCache.setCellY(cellY);
            lootCache.setSourceType("CHEST");
            lootCache.setSourceId(monsterId);

            List<MapCachedItem> cachedItems = new ArrayList<>();
            for (int i = 0; i < result.getDrops().size(); i++) {
                var drop = result.getDrops().get(i);
                cachedItems.add(new MapCachedItem(
                        state.nextCachedItemId(mapCacheId, i),
                        drop.getItemId(),
                        drop.getCount()));
            }
            lootCache.setItems(cachedItems);
            state.addLootCache(lootCache);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", MessageType.BATTLE_RESULT);
        response.put("code", 200);
        response.put("result", toResultMap(result));
        response.put("cellX", cellX != null ? cellX : -1);
        response.put("cellY", cellY != null ? cellY : -1);
        response.put("logs", result.getLogs());
        if (mapCacheId != null) {
            response.put("mapCacheId", mapCacheId);
        }
        session.sendMessage(response);
//        log.debug("战斗结束: userId={}, monsterId={}, type={}, result:{}", session.getUserId(), monsterId, result.getType(), JsonUtil.toJsonString(result));
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
        m.put("drops", List.of());
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
