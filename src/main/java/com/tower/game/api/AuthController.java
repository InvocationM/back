package com.tower.game.api;

import com.tower.game.common.annotation.NoLog;
import com.tower.game.common.auth.CurrentUser;
import com.tower.game.common.dto.LoginRequest;
import com.tower.game.common.dto.LoginResponse;
import com.tower.game.common.response.ApiResponse;
import com.tower.game.service.AuthTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthTokenService authTokenService;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @NoLog
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());

        CurrentUser user = authTokenService.login(request.getUsername(), request.getPassword());
        String token = authTokenService.issueToken(user);
        String websocketUrl = String.format("ws://%s:%d/tower/ws?token=%s", serverAddress, serverPort, token);

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
}
