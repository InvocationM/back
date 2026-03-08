package com.tower.game.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大章节实体（bigMaps 章节）
 */
@Data
@TableName("big_map")
public class BigMap {

    /**
     * 大章节ID，与前端 bigMap.id 一致
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 章节名称，如第一章
     */
    private String name;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
