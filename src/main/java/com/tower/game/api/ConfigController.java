package com.tower.game.api;

import com.tower.game.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 获取服务器列表
     */
    @GetMapping("/server-list")
    public ApiResponse<Map<String, Object>> getServerList() {
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("serverId", 1);
        serverInfo.put("serverName", "服务器1");
        serverInfo.put("status", "正常");
        serverInfo.put("websocketUrl", "ws://localhost:" + serverPort + "/ws");
        
        return ApiResponse.success(serverInfo);
    }

    /**
     * 获取游戏配置
     */
    @GetMapping("/game-config")
    public ApiResponse<Map<String, Object>> getGameConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("version", "1.0.0");
        config.put("maxPlayers", 1000);
        config.put("maxFloors", 50);
        
        return ApiResponse.success(config);
    }
}
