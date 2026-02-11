package com.tower.game.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 玩家信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDTO {
    /**
     * 玩家ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 等级
     */
    private Integer level;

    /**
     * 经验值
     */
    private Long exp;

    /**
     * 当前楼层
     */
    private Integer currentFloor;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
