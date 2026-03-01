package com.tower.game.api;

import com.tower.game.common.dto.MonsterBatchQueryRequest;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.model.entity.Monster;
import com.tower.game.service.MonsterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 怪物配置查询接口
 */
@RestController
@RequestMapping("/api/monster")
@RequiredArgsConstructor
public class MonsterController {

    private final MonsterService monsterService;

    /**
     * 根据 id 列表批量查询怪物
     * POST /api/monster/batch，请求体示例：{ "ids": [1, 2, 3] }
     */
    @PostMapping("/batch")
    public ApiResponse<List<Monster>> batch(@Valid @RequestBody MonsterBatchQueryRequest request) {
        List<Monster> list = monsterService.listByIds(request.getIds());
        return ApiResponse.success(list);
    }
}
