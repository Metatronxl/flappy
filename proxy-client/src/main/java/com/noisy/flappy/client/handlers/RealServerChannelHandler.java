package com.noisy.flappy.client.handlers;

import com.noisy.flappy.client.ClientChannelManager;
import com.noisy.flappy.protocol.Constants;
import com.noisy.flappy.protocol.ProxyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lei.X
 * @date 2019/7/3
 */
@Slf4j
public class RealServerChannelHandler extends SimpleChannelInboundHandler<ByteBuf>{

    /**
     * 获取客户端对应端口返回的数据，并用数据传输通道返回回去
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Channel realServerChannel = ctx.channel();
        // 数据传输通道
        Channel channel = realServerChannel.attr(Constants.NEXT_CHANNEL).get();
        if (channel == null){
            // 代理客户端连接断开
            ctx.channel().close();
        }else {
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            String userId = ClientChannelManager.getRealServerChannelUserId(realServerChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.P_TYPE_TRANSFER);
            proxyMessage.setUri(userId);
            proxyMessage.setData(bytes);
            channel.writeAndFlush(proxyMessage);
            log.debug("write data to proxy server, {}, {}", realServerChannel, channel);
        }

    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        Channel realServerChannel = ctx.channel();
        String userId = ClientChannelManager.getRealServerChannelUserId(realServerChannel);
        ClientChannelManager.removeRealServerChannel(userId);
        Channel channel = realServerChannel.attr(Constants.NEXT_CHANNEL).get();

        if (channel != null){
            log.debug("channelInactive,{}",realServerChannel);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
            proxyMessage.setUri(userId);
            channel.writeAndFlush(proxyMessage);
        }

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel realServerChannel = ctx.channel();
        Channel proxyChannel = realServerChannel.attr(Constants.NEXT_CHANNEL).get();
        if (proxyChannel != null) {
            proxyChannel.config().setOption(ChannelOption.AUTO_READ, realServerChannel.isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }
}
