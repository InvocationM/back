package com.tower.game.service;

import com.tower.game.common.auth.CurrentUser;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.model.entity.PlayerAttribute;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final PlayerAttributeService playerAttributeService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, CurrentUser> tokenStore = new ConcurrentHashMap<>();

    @Value("${game.auth.default-player-id:1001}")
    private Long defaultPlayerId;

    public CurrentUser login(String username, String password) {
        Long userId = resolveUserId(username);
        PlayerAttribute attr = playerAttributeService.getByPlayerId(userId);
        if (attr == null) {
            throw new BusinessException(401, "玩家不存在: " + userId);
        }
        String displayName = username == null || username.isBlank() ? "player_" + userId : username.trim();
        return new CurrentUser(userId, displayName);
    }

    public String issueToken(CurrentUser user) {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        tokenStore.put(token, user);
        return token;
    }

    public CurrentUser parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "未登录");
        }
        CurrentUser user = tokenStore.get(token.trim());
        if (user == null) {
            throw new BusinessException(401, "登录已失效");
        }
        return user;
    }

    private Long resolveUserId(String username) {
        if (username != null && !username.isBlank()) {
            String trimmed = username.trim();
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
                // Non-numeric names use the configured development player id in v1.
            }
        }
        return defaultPlayerId;
    }
}
