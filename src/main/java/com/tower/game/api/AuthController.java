package com.tower.game.api;

import com.tower.game.common.dto.LoginRequest;
import com.tower.game.common.dto.LoginResponse;
import com.tower.game.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${netty.server.port:9090}")
    private int nettyPort;

    @Value("${netty.server.host:localhost}")
    private String serverAddress;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());
        
        // TODO: 实现实际的登录验证逻辑
        // 1. 验证用户名密码
        // 2. 生成token
        // 3. 返回token和WebSocket连接地址
        
        // 临时实现
        String token = generateToken(request.getUsername());
        String websocketUrl = String.format("ws://%s:%d", serverAddress, nettyPort);
        
        LoginResponse response = new LoginResponse(
            token,
            1L, // TODO: 从数据库获取实际userId
            request.getUsername(),
            websocketUrl
        );
        
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody LoginRequest request) {
        log.info("用户注册请求: {}", request.getUsername());
        
        // TODO: 实现实际的注册逻辑
        
        return ApiResponse.success("注册成功", null);
    }

    /**
     * 生成token（临时实现）
     */
    private String generateToken(String username) {
        // TODO: 使用JWT或其他方式生成token
        return "token_" + username + "_" + System.currentTimeMillis();
    }
}
