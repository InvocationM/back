package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家实体
 */
@Data
@TableName("player")
public class Player {
    /**
     * 玩家ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密后）
     */
    private String password;

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
