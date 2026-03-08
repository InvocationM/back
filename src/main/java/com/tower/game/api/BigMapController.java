package com.tower.game.api;

import com.tower.game.common.dto.bigmap.BigMapsResponse;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.service.BigMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大章节配置接口，出参结构为 { "bigMaps": [ { "id", "name", "layers": [ { "options": [...] } ] } ] }
 */
@RestController
@RequestMapping("/api/big-map")
@RequiredArgsConstructor
public class BigMapController {

    private final BigMapService bigMapService;

    /**
     * 获取大章节列表，出参与约定 JSON 一致
     */
    @PostMapping("/bigMapAll")
    public ApiResponse<BigMapsResponse> bigMapAll() {
        BigMapsResponse data = bigMapService.listBigMaps();
        return ApiResponse.success(data);
    }
}
