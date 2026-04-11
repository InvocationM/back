package com.tower.game.api;

import com.tower.game.common.dto.BloodPotionBatchQueryRequest;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.Item;
import com.tower.game.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 血瓶配置查询接口（物品 type=4）
 */
@RestController
@RequestMapping("/api/blood-potion")
@RequiredArgsConstructor
public class BloodPotionController {

    private final ItemService itemService;

    /**
     * 根据 id 列表批量查询血瓶（仅返回 type=4 的物品）
     * POST /api/blood-potion/batch，请求体示例：{ "ids": [4] }
     */
    @PostMapping("/batch")
    public ApiResponse<List<Item>> batch(@Valid @RequestBody BloodPotionBatchQueryRequest request) {
        List<Item> list = itemService.listBloodPotionsByIds(request.getIds());
        return ApiResponse.success(list);
    }
}
