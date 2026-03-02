package com.tower.game.server.session;

import com.tower.game.common.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 可序列化的玩家会话状态（用于内存/后续 Redis 等存储，与连接解耦）
 */
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

    /** 当前地图 ID，未进图为 null */
    private Integer mapId;
    /** 当前格子 X，未进图为 -1 */
    private int cellX;
    /** 当前格子 Y，未进图为 -1 */
    private int cellY;

    private long loginTime;
    private long lastActiveTime;

    /** 当前血量（可选，玩法需要时使用） */
    private int hp;
    /** 最大血量（可选） */
    private int maxHp;

    /**
     * 是否已设置地图位置（已进图）
     */
    public boolean hasPosition() {
        return cellX >= 0 && cellY >= 0;
    }
}
