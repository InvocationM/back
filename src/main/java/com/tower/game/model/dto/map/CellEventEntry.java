package com.tower.game.model.dto.map;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 格子事件条目，对应地图 JSON 中单个格子的一个事件（与客户端 CellEventEntry 一致）
 * 用于服务端按权重选择事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CellEventEntry {

    /**
     * 事件类型 1-6（入口、空地、阻挡、出口、怪物、宝箱  9=血瓶等）
     */
    private int type;

    /**
     * 事件ID（如怪物ID、宝箱ID）
     */
    private int id;

    /**
     * 权重 0-100，100=必出；选中的概率 = 该事件权重 / 总权重
     */
    private int weight;
}
