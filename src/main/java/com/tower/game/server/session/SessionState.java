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

    /** 当前地图缓存：与 mapId 一致时有效，为当前地图 JSON（GameMap.data） */
    private Integer currentMapId;
    private String currentMapData;

    private long loginTime;
    private long lastActiveTime;

    /** 当前血量 */
    private int hp;
    /** 最大血量 */
    private int maxHp;

    /** 战斗属性（方案 B 默认值，与境界表一致时 10:1 为百分比） */
    private int attack;
    private int defence;
    private int dodge;
    private int accurate;
    private int crit;
    private int doublehit;
    private int reflect;
    private String name;
    private String icon;

    /**
     * 是否已设置地图位置（已进图）
     */
    public boolean hasPosition() {
        return cellX >= 0 && cellY >= 0;
    }

    /**
     * 当前会话是否已有与 mapId 对应的地图数据缓存
     */
    public boolean hasCurrentMapDataFor(Integer mapId) {
        return mapId != null && mapId.equals(currentMapId) && currentMapData != null && !currentMapData.isBlank();
    }

    /** 清空当前地图缓存（换图时调用） */
    public void clearCurrentMapData() {
        this.currentMapId = null;
        this.currentMapData = null;
    }
}
