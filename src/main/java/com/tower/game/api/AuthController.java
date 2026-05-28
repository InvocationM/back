package com.tower.game.api;

import com.tower.game.common.annotation.NoLog;
import com.tower.game.common.auth.CurrentUser;
import com.tower.game.common.dto.LoginRequest;
import com.tower.game.common.dto.LoginResponse;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthTokenService authTokenService;

    @NoLog
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求: {}", request.getUsername());

        CurrentUser user = authTokenService.login(request.getUsername(), request.getPassword());
        String token = authTokenService.issueToken(user);
        String websocketUrl = buildWebSocketUrl(httpRequest, token);

        LoginResponse response = new LoginResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                websocketUrl
        );
        return ApiResponse.success("登录成功", response);
    }

    @NoLog
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody LoginRequest request) {
        log.info("用户注册请求: {}", request.getUsername());
        return ApiResponse.success("注册接口未接入", null);
    }

    private String buildWebSocketUrl(HttpServletRequest request, String token) {
        String scheme = request.isSecure() ? "wss" : "ws";
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return scheme + "://" + request.getServerName() + ":" + request.getServerPort()
                + contextPath + "/ws?token=" + encodedToken;
    }
}
