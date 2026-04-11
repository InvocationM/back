package com.tower.game.api;

import com.tower.game.common.dto.ItemBatchQueryRequest;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.Item;
import com.tower.game.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 物品配置查询接口
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * 根据 id 列表批量查询物品
     * POST /api/item/batch，请求体示例：{ "ids": [1, 2, 3, 4] }
     */
    @PostMapping("/batch")
    public ApiResponse<List<Item>> batch(@Valid @RequestBody ItemBatchQueryRequest request) {
        List<Item> list = itemService.listByIds(request.getIds());
        return ApiResponse.success(list);
    }
}
