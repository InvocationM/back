package com.tower.game.common.auth;

import com.tower.game.common.exception.BusinessException;
import com.tower.game.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final AuthTokenService authTokenService;

    public CurrentUser requireUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new BusinessException(401, "未登录");
        }
        String prefix = "Bearer ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new BusinessException(401, "Authorization 格式错误");
        }
        return authTokenService.parseToken(header.substring(prefix.length()));
    }
}
