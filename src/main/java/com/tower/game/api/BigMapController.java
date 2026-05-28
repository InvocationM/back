package com.tower.game.api;

import com.tower.game.common.auth.CurrentUserResolver;
import com.tower.game.common.dto.bigmap.BigMapListResponse;
import com.tower.game.common.dto.bigmap.BigMapStartRequest;
import com.tower.game.common.dto.bigmap.BigMapVO;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.service.BigMapService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/big-map")
@RequiredArgsConstructor
public class BigMapController {

    private final BigMapService bigMapService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/bigMapList")
    public ApiResponse<BigMapListResponse> bigMapList() {
        return ApiResponse.success(bigMapService.listAllChapters());
    }

    @PostMapping("/start")
    public ApiResponse<BigMapVO> start(@RequestBody BigMapStartRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getBigMapId() == null) {
            throw new BusinessException(400, "缺少 bigMapId");
        }
        long playerId = currentUserResolver.requireUser(httpRequest).getUserId();
        BigMapVO data = bigMapService.startChapter(playerId, request.getBigMapId());
        return ApiResponse.success(data);
    }
}
