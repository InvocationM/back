package com.tower.game.api;

import com.tower.game.common.dto.bigmap.BigMapListResponse;
import com.tower.game.common.dto.bigmap.BigMapStartRequest;
import com.tower.game.common.dto.bigmap.BigMapVO;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.service.BigMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大章节：开局锁定各层小地图；进下一层走 WS {@code BIG_MAP_USE_EXIT(5010)}。
 */
@RestController
@RequestMapping("/api/big-map")
@RequiredArgsConstructor
public class BigMapController {

    /** 与 MapController 一致，临时写死用户 ID，后续改为登录态 */
    private static final long DEFAULT_PLAYER_ID = 1001L;

    private final BigMapService bigMapService;

    /**
     * 大章节列表：id、name、层数（只读，不含每层随机 mapId）。body 可为空。
     */
    @PostMapping("/bigMapList")
    public ApiResponse<BigMapListResponse> bigMapList() {
        return ApiResponse.success(bigMapService.listAllChapters());
    }

    /**
     * 开始指定章节：随机锁定每层 mapId 并写入 Redis，返回本章 id、name、layers（每层 options 仅一个 mapId）。
     */
    @PostMapping("/start")
    public ApiResponse<BigMapVO> start(@RequestBody BigMapStartRequest request) {
        if (request == null || request.getBigMapId() == null) {
            throw new BusinessException(400, "缺少 bigMapId");
        }
        BigMapVO data = bigMapService.startChapter(DEFAULT_PLAYER_ID, request.getBigMapId());
        return ApiResponse.success(data);
    }
}
