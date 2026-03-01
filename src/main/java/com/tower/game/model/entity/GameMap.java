package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地图实体（data 存整份前端 JSON）
 */
@Data
@TableName("game_map")
public class GameMap {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 地图业务 ID，即前端 mapId
     */
    @TableField("map_id")
    private Integer mapId;

    /**
     * 整份地图 JSON（含 mapId、width、height、cells）
     */
    private String data;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
