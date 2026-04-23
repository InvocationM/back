package com.tower.game.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * POST /api/map/openDoor 请求体：目标门所在格与门编号（与地图 events[0].id 一致）。
 */
@Data
public class OpenDoorRequest {

    @NotNull(message = "cellX 不能为空")
    private Integer cellX;

    @NotNull(message = "cellY 不能为空")
    private Integer cellY;

    @NotNull(message = "doorId 不能为空")
    private Integer doorId;
}
