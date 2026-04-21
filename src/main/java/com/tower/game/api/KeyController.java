package com.tower.game.api;

import com.tower.game.common.dto.KeyBatchQueryRequest;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.Item;
import com.tower.game.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 钥匙配置查询接口（物品 {@link ItemService#ITEM_TYPE_KEY}）
 */
@RestController
@RequestMapping("/api/key")
@RequiredArgsConstructor
public class KeyController {

    private final ItemService itemService;

    /**
     * 根据 id 列表批量查询钥匙（仅返回 type=7 的物品）
     * POST /api/key/batch，请求体示例：{ "ids": [1, 2] }
     */
    @PostMapping("/batch")
    public ApiResponse<List<Item>> batch(@Valid @RequestBody KeyBatchQueryRequest request) {
        List<Item> list = itemService.listKeysByIds(request.getIds());
        return ApiResponse.success(list);
    }
}
