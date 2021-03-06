package com.noisy.flappy.client;

import com.noisy.flappy.client.handlers.ClientChannelHandler;
import com.noisy.flappy.client.handlers.RealServerChannelHandler;
import com.noisy.flappy.client.listener.ChannelStatusListener;
import com.noisy.flappy.common.Config;
import com.noisy.flappy.common.container.Container;
import com.noisy.flappy.protocol.IdleCheckHandler;
import com.noisy.flappy.protocol.ProxyMessage;
import com.noisy.flappy.protocol.ProxyMessageDecoder;
import com.noisy.flappy.protocol.ProxyMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;

/**
 * @author lei.X
 * @date 2019/6/30
 */
@Slf4j
@Component
public class ProxyClientContainer implements Container, ChannelStatusListener {

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;

    private static final int LENGTH_FIELD_OFFSET = 0;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private static final int LENGTH_ADJUSTMENT = 0;

    private NioEventLoopGroup workerGroup;

    private Bootstrap bootstrap;

    private Bootstrap realServerBootstrap;

    private Config config = Config.getInstance();

    private SSLContext sslContext;

    private long sleepTimeMill = 1000;


    public ProxyClientContainer() {
        workerGroup = new NioEventLoopGroup();
        realServerBootstrap = new Bootstrap();
        realServerBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new RealServerChannelHandler());
                    }
                });

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //TODO SSL encrypted
//                        if (Config.getInstance().getBooleanValue("ssl.enable", false)) {
//                            if (sslContext == null) {
//                                sslContext = SslContextCreator.createSSLContext();
//                            }
//
//                            ch.pipeline().addLast(createSslHandler(sslContext));
//                        }
                        ch.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                        ch.pipeline().addLast(new ProxyMessageEncoder());
                        ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME - 10, 0));
                        ch.pipeline().addLast(new ClientChannelHandler(realServerBootstrap, bootstrap, ProxyClientContainer.this));
                    }
                });

    }


    @Override
    public void start() {
        connectProxyServer();
    }

    @Override
    public void stop() {
        workerGroup.shutdownGracefully();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        reconnectWait();
        connectProxyServer();
    }


    private void connectProxyServer(){
        bootstrap.connect(config.getStringValue("server.host"),config.getIntValue("server.port")).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {

                    // 连接成功，向服务器发送客户端认证信息（clientKey）
                    ClientChannelManager.setCmdChannel(future.channel());
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.C_TYPE_AUTH);
                    proxyMessage.setUri(config.getStringValue("client.key"));
                    future.channel().writeAndFlush(proxyMessage);
                    sleepTimeMill = 1000;
                    log.info("connect proxy server success, {}", future.channel());
                } else {
                    log.warn("connect proxy server failed", future.cause());

                    // 连接失败，发起重连
                    reconnectWait();
                    connectProxyServer();
                }
            }
        });
    }


    private void reconnectWait() {
        try {
            if (sleepTimeMill > 60000) {
                sleepTimeMill = 1000;
            }

            synchronized (this) {
                sleepTimeMill = sleepTimeMill * 2;
                wait(sleepTimeMill);
            }
        } catch (InterruptedException e) {
        }
    }


}
