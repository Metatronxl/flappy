package com.noisy.flappy.client.handlers;

import com.noisy.flappy.client.ClientChannelManager;
import com.noisy.flappy.client.listener.ChannelStatusListener;
import com.noisy.flappy.client.listener.ProxyChannelBorrowListener;
import com.noisy.flappy.common.Config;
import com.noisy.flappy.protocol.Constants;
import com.noisy.flappy.protocol.ProxyMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import javax.print.DocFlavor;

/**
 * @author lei.X
 * @date 2019/7/3
 * <p>
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
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {

        log.debug("recieved proxy message, type is {}", proxyMessage.getType());

        switch (proxyMessage.getType()) {
            case ProxyMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.P_TYPE_TRANSFER:
                handleTransferMessage(ctx,proxyMessage);
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx,proxyMessage);

            default:
                break;


        }

    }

    /**
     * 处理断开连接的逻辑
     * @param ctx
     * @param proxyMessage
     */
    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {

        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        log.debug("handleDisconnectMessage, {}", realServerChannel);

        if (realServerChannel != null){
            ctx.channel().attr(Constants.NEXT_CHANNEL).set(null);
            ClientChannelManager.returnProxyChanel(ctx.channel());
            realServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 处理数据传输
     * @param ctx
     * @param proxyMessage
     */
    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null) {

            ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
            buf.writeBytes(proxyMessage.getData());
            log.debug("write data to real server, {}",realServerChannel);
            realServerChannel.writeAndFlush(buf);
        }


    }

    /**
     * connect ip and port provided by server
     *
     *        example : 127.0.0.1:22 (lanInfo)
     *
     * @param ctx
     * @param proxyMessage
     */
    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {

        final Channel cmdChannel = ctx.channel();
        final String userId = proxyMessage.getUri();
        String[] serverInfo = new String(proxyMessage.getData()).split(":");
        // 127.0.0.1:22
        String ip = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);

        // connect to realserver ip:port
        bootstrap.connect(ip, port).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {

                // 连接后端服务器成功
                if (future.isSuccess()) {
                    final Channel realServerChannel = future.channel();
                    log.debug("connect realserver success, {}", realServerChannel);

                    realServerChannel.config().setOption(ChannelOption.AUTO_READ, false);

                    // 获取连接
                    /**
                     * 为什么要采用cmdChannel（控制连接）和ctx.channel()  （真实数据的传输连接）

                       Ans：1.这是因为如果所有的数据都走cmdChannel的话，一旦这是一个传输数据量较大的请求
                       那么channel就会被堵死，在数据没有发送完毕之前无法再互相通信
                            2. 单一的cmdChannel无法建立多用户机制（即无法新建窗口，创立一个新的连接）

                       所以我们采用连接池的方式，一方面，一个新的请求即创建一个新的ctx.channel(真实数据的传输连接)
                       一方面将所有的channel放进连接池中，方便复用且减小创建新的channel的开销
                     */

                    ClientChannelManager.borrowProxyChannel(proxyBootstrap, new ProxyChannelBorrowListener() {

                        @Override
                        public void success(Channel channel) {
                            // 连接绑定
                            channel.attr(Constants.NEXT_CHANNEL).set(realServerChannel);
                            realServerChannel.attr(Constants.NEXT_CHANNEL).set(channel);

                            // 远程绑定
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
                            proxyMessage.setUri(userId + "@" + Config.getInstance().getStringValue("client.key"));
                            channel.writeAndFlush(proxyMessage);

                            realServerChannel.config().setOption(ChannelOption.AUTO_READ, true);
                            ClientChannelManager.addRealServerChannel(userId, realServerChannel);
                            ClientChannelManager.setRealServerChannelUserId(realServerChannel, userId);
                        }

                        @Override
                        public void error(Throwable cause) {
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                            proxyMessage.setUri(userId);
                            cmdChannel.writeAndFlush(proxyMessage);
                        }
                    });

                } else {
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                    proxyMessage.setUri(userId);
                    cmdChannel.writeAndFlush(proxyMessage);
                }
            }
        });
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null){
            realServerChannel.config().setOption(ChannelOption.AUTO_READ,ctx.channel().isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    /**
     * 因为连接分为cmdChannel（控制连接）和ctx.channel()  （真实数据的传输连接）
     * 所以在inActive的处理上需要分开
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        //控制连接
        if (ClientChannelManager.getCmdChannel() == ctx.channel()){
            ClientChannelManager.setCmdChannel(null);
            ClientChannelManager.clearRealServerChannels();
            // 通知重新连接
            channelStatusListener.channelInactive(ctx);
        }else{
        // 数据传输连接
            Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
            if (realServerChannel != null && realServerChannel.isActive()) {
                realServerChannel.close();
            }
        }

        ClientChannelManager.removeProxyChanel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }




}
