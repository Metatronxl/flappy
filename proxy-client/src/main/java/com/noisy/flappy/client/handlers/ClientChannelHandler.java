package com.noisy.flappy.client.handlers;

import com.noisy.flappy.client.listener.ChannelStatusListener;
import com.noisy.flappy.protocol.ProxyMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lei.X
 * @date 2019/7/3
 *
 * 代理客户端处理
 */
@Slf4j
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    /**
     * 真实服务的BootStrap
     */
    private Bootstrap bootstrap;

    /**
     * 客户端与代理客户端相连接的BootStrap
     */
    private Bootstrap proxyBootstrap;

    private ChannelStatusListener channelStatusListener;


    public ClientChannelHandler(io.netty.bootstrap.Bootstrap bootstrap, io.netty.bootstrap.Bootstrap proxyBootstrap, ChannelStatusListener channelStatusListener) {
        this.bootstrap = bootstrap;
        this.proxyBootstrap = proxyBootstrap;
        this.channelStatusListener = channelStatusListener;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {

    }
}
