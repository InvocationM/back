package com.tower.game.config;

import com.tower.game.server.handler.GameMessageHandler;
import com.tower.game.server.NettyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Netty服务器配置
 */
@Slf4j
@Configuration
public class NettyServerConfig {

    @Value("${netty.server.port:9090}")
    private int port;

    @Value("${netty.server.boss-thread-count:1}")
    private int bossThreadCount;

    @Value("${netty.server.worker-thread-count:4}")
    private int workerThreadCount;

    @Bean
    public NettyServer nettyServer(GameMessageHandler gameMessageHandler) {
        return new NettyServer(port, bossThreadCount, workerThreadCount, gameMessageHandler);
    }
}
