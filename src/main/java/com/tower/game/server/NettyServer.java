package com.tower.game.server;

import com.tower.game.server.handler.GameMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty服务器
 */
@Slf4j
@Component
public class NettyServer {

    private final int port;
    private final int bossThreadCount;
    private final int workerThreadCount;
    private final GameMessageHandler gameMessageHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public NettyServer(int port, int bossThreadCount, int workerThreadCount, 
                      GameMessageHandler gameMessageHandler) {
        this.port = port;
        this.bossThreadCount = bossThreadCount;
        this.workerThreadCount = workerThreadCount;
        this.gameMessageHandler = gameMessageHandler;
    }

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(bossThreadCount);
        workerGroup = new NioEventLoopGroup(workerThreadCount);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // HTTP编解码器
                        pipeline.addLast(new HttpServerCodec());
                        // HTTP消息聚合器
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // WebSocket协议处理器，路径为 /ws
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                        
                        // 添加业务处理器
                        pipeline.addLast(gameMessageHandler);
                    }
                });

        ChannelFuture future = bootstrap.bind(port);
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                log.info("Netty WebSocket服务器启动成功，端口: {}, 路径: /ws", port);
            } else {
                log.error("Netty服务器启动失败", f.cause());
            }
        });
    }

    @PreDestroy
    public void stop() {
        if (channel != null) {
            channel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty服务器已关闭");
    }
}
