package com.tower.game.service;

import com.tower.game.common.dto.battle.BattleResultDto;
import com.tower.game.common.dto.battle.BattleResultType;
import com.tower.game.common.dto.battle.CombatantSnapshot;
import com.tower.game.model.entity.Monster;
import com.tower.game.server.session.SessionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 战斗核心逻辑：命中、伤害、暴击、反伤、连击，50 回合上限（与 Unity BattleEngine 一致）
 */
@Slf4j
@Service
public class BattleEngineService {

    private static final double CRIT_MULTIPLIER = 1.5;
    private static final int MAX_LOGS = 200;

    /**
     * 从 SessionState 构建玩家参战快照（10:1 换算为百分比）
     */
    public CombatantSnapshot buildPlayerSnapshot(SessionState state) {
        if (state == null) return null;
        return CombatantSnapshot.builder()
                .name(state.getName() != null ? state.getName() : "玩家")
                .icon(state.getIcon() != null ? state.getIcon() : "PLAYER1")
                .attack(state.getAttack())
                .defence(state.getDefence())
                .maxHp(state.getMaxHp())
                .currentHp(state.getHp())
                .dodgePct(state.getDodge() / 10.0)
                .accuratePct(state.getAccurate() / 10.0)
                .critPct(state.getCrit() / 10.0)
                .critDamageMultiplier(CRIT_MULTIPLIER)
                .doubleHitPct(state.getDoublehit() / 10.0)
                .reflectPct(state.getReflect() / 10.0)
                .build();
    }

    /**
     * 从 Monster 构建怪物参战快照（当前血量 = maxhp，10:1 为百分比）
     */
    public CombatantSnapshot buildMonsterSnapshot(Monster monster) {
        if (monster == null) return null;
        int maxhp = monster.getMaxhp() != null ? monster.getMaxhp() : 0;
        return CombatantSnapshot.builder()
                .name(monster.getName() != null ? monster.getName() : "怪物")
                .icon(monster.getIcon() != null ? monster.getIcon() : "")
                .attack(monster.getAttack() != null ? monster.getAttack() : 0)
                .defence(monster.getDefence() != null ? monster.getDefence() : 0)
                .maxHp(maxhp)
                .currentHp(maxhp)
                .dodgePct((monster.getDoge() != null ? monster.getDoge() : 0) / 10.0)
                .accuratePct((monster.getAccurate() != null ? monster.getAccurate() : 0) / 10.0)
                .critPct((monster.getCrit() != null ? monster.getCrit() : 0) / 10.0)
                .critDamageMultiplier(CRIT_MULTIPLIER)
                .doubleHitPct((monster.getDoublehit() != null ? monster.getDoublehit() : 0) / 10.0)
                .reflectPct((monster.getReflect() != null ? monster.getReflect() : 0) / 10.0)
                .build();
    }

    /**
     * 执行整场战斗，直到一方死亡或超过最大回合数
     */
    public BattleResultDto run(CombatantSnapshot player, CombatantSnapshot monster, int maxRounds) {
        List<String> logs = new ArrayList<>();
        if (player == null || monster == null) {
            logs.add("战斗数据异常。");
            return BattleResultDto.builder()
                    .type(BattleResultType.Lose)
                    .playerCurrentHp(0)
                    .monsterCurrentHp(monster != null ? monster.getCurrentHp() : 0)
                    .totalRounds(0)
                    .logs(logs)
                    .build();
        }

        int playerHp = player.getCurrentHp();
        int monsterHp = monster.getCurrentHp();
        int round = 0;
        addLog(logs, "战斗开始：" + player.getName() + " vs " + monster.getName() + "。");

        while (round < maxRounds) {
            round++;

            // 玩家攻怪物
            int[] afterPlayerAttack = executeOneAttack(player, monster, playerHp, monsterHp, logs, true);
            playerHp = afterPlayerAttack[0];
            monsterHp = afterPlayerAttack[1];
            if (monsterHp <= 0) {
                addLog(logs, "战斗结束，" + player.getName() + "胜利。");
                return BattleResultDto.builder()
                        .type(BattleResultType.Win)
                        .playerCurrentHp(playerHp)
                        .monsterCurrentHp(0)
                        .totalRounds(round)
                        .logs(logs)
                        .build();
            }

            // 怪物攻玩家
            int[] afterMonsterAttack = executeOneAttack(monster, player, monsterHp, playerHp, logs, false);
            monsterHp = afterMonsterAttack[0];
            playerHp = afterMonsterAttack[1];
            if (playerHp <= 0) {
                addLog(logs, "战斗结束，" + player.getName() + "失败。");
                return BattleResultDto.builder()
                        .type(BattleResultType.Lose)
                        .playerCurrentHp(0)
                        .monsterCurrentHp(monsterHp)
                        .totalRounds(round)
                        .logs(logs)
                        .build();
            }
        }

        addLog(logs, "战斗超过" + maxRounds + "回合未结束，判负。");
        return BattleResultDto.builder()
                .type(BattleResultType.Timeout)
                .playerCurrentHp(playerHp)
                .monsterCurrentHp(monsterHp)
                .totalRounds(round)
                .logs(logs)
                .build();
    }

    /** 执行一次「攻击方→防御方」进攻，可能连击；返回 [攻击方当前血量, 防御方当前血量]（isPlayerAttacking 时 0=玩家 1=怪物） */
    private int[] executeOneAttack(CombatantSnapshot attacker, CombatantSnapshot defender,
                                   int attackerHp, int defenderHp, List<String> logs, boolean isPlayerAttacking) {
        int[] state = new int[]{attackerHp, defenderHp};
        doSingleHit(attacker, defender, logs, state);
        if (state[1] <= 0 && isPlayerAttacking) return state;
        if (state[0] <= 0 && !isPlayerAttacking) return state;

        if (ThreadLocalRandom.current().nextDouble(0, 100) < attacker.getDoubleHitPct()) {
            addLog(logs, attacker.getName() + "发动连击！");
            doSingleHit(attacker, defender, logs, state);
        }
        return state;
    }

    /** state[0]=攻击方血量, state[1]=防御方血量，方法内会修改 state */
    private void doSingleHit(CombatantSnapshot attacker, CombatantSnapshot defender, List<String> logs, int[] state) {
        int attackerHp = state[0];
        int defenderHp = state[1];

        if (!checkHit(attacker, defender, logs)) {
            return;
        }

        int damage = calculateDamage(attacker, defender, logs);
        defenderHp = Math.max(0, defenderHp - damage);

        if (damage > 0 && defender.getReflectPct() > 0) {
            int reflectDamage = Math.max(1, (int) (damage * defender.getReflectPct() / 100));
            attackerHp = Math.max(0, attackerHp - reflectDamage);
            addLog(logs, attacker.getName() + "受到" + reflectDamage + "点反伤。");
        }

        state[0] = attackerHp;
        state[1] = defenderHp;
    }

    private boolean checkHit(CombatantSnapshot attacker, CombatantSnapshot defender, List<String> logs) {
        double hitChance = 100 + attacker.getAccuratePct() - defender.getDodgePct();
        if (hitChance <= 0) {
            addLog(logs, attacker.getName() + "的攻击被" + defender.getName() + "闪避了。");
            return false;
        }
        if (hitChance < 100) {
            double roll = ThreadLocalRandom.current().nextDouble(0, 100);
            if (roll > hitChance) {
                addLog(logs, attacker.getName() + "的攻击被" + defender.getName() + "闪避了。");
                return false;
            }
        }
        return true;
    }

    private int calculateDamage(CombatantSnapshot attacker, CombatantSnapshot defender, List<String> logs) {
        int base = Math.max(1, attacker.getAttack() - defender.getDefence());
        boolean crit = attacker.getCritPct() >= 100 || ThreadLocalRandom.current().nextDouble(0, 100) < attacker.getCritPct();
        int damage = crit ? (int) Math.round(base * attacker.getCritDamageMultiplier()) : base;
        if (crit) {
            addLog(logs, attacker.getName() + "发动暴击对" + defender.getName() + "造成了" + damage + "点伤害。");
        } else {
            addLog(logs, attacker.getName() + "对" + defender.getName() + "造成了" + damage + "点伤害。");
        }
        return damage;
    }

    private void addLog(List<String> logs, String msg) {
        logs.add(msg);
        if (logs.size() > MAX_LOGS) logs.remove(0);
    }
}
