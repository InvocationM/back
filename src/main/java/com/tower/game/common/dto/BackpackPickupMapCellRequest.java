package com.tower.game.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按地图格子拾取（钥匙 type=7、血瓶 type=9），坐标与移动协议 targetX/targetY 一致。
 */
@Data
public class BackpackPickupMapCellRequest {

    @NotNull(message = "cellX 不能为空")
    private Integer cellX;

    @NotNull(message = "cellY 不能为空")
    private Integer cellY;
}
