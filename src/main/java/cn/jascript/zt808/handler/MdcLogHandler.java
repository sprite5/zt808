package cn.jascript.zt808.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.MDC;

import java.net.InetSocketAddress;

public class MdcLogHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        var addr = ctx.channel().remoteAddress();
        if (addr instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) addr;
            MDC.put("Ip", inet.getHostString());
            MDC.put("Port", String.valueOf(inet.getPort()));
        }
        try {
            super.channelRead(ctx, msg);
        } finally {
            MDC.remove("Ip");
            MDC.remove("Port");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        var addr = ctx.channel().remoteAddress();
        if (addr instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) addr;
            MDC.put("Ip", inet.getHostString());
            MDC.put("Port", String.valueOf(inet.getPort()));
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        MDC.remove("Ip");
        MDC.remove("Port");
        super.channelInactive(ctx);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        MDC.remove("Ip");
        MDC.remove("Port");
        super.exceptionCaught(ctx, cause);
    }
}
