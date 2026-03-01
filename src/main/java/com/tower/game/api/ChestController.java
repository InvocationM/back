package com.tower.game.api;

import com.tower.game.common.dto.ChestBatchQueryRequest;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.Chest;
import com.tower.game.service.ChestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 宝箱配置查询接口
 */
@RestController
@RequestMapping("/api/chest")
@RequiredArgsConstructor
public class ChestController {

    private final ChestService chestService;

    /**
     * 根据 id 列表批量查询宝箱
     * POST /api/chest/batch，请求体示例：{ "ids": [1, 2, 3] }
     */
    @PostMapping("/batch")
    public ApiResponse<List<Chest>> batch(@Valid @RequestBody ChestBatchQueryRequest request) {
        List<Chest> list = chestService.listByIds(request.getIds());
        return ApiResponse.success(list);
    }
}
