package com.tower.game.server.session;

import com.tower.game.common.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionState implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private Long userId;
    private String username;
    private GameStatus gameStatus;
    private Integer mapId;
    private int cellX;
    private int cellY;
    private long loginTime;
    private long lastActiveTime;
    private int hp;
    private int maxHp;
    private int attack;
    private int defence;
    private int dodge;
    private int accurate;
    private int crit;
    private int doublehit;
    private int reflect;
    private String name;
    private String icon;

    public boolean hasPosition() {
        return cellX >= 0 && cellY >= 0;
    }
}
