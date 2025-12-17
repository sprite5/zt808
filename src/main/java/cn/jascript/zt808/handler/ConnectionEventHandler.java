package cn.jascript.zt808.handler;

import cn.jascript.zt808.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理连接建立/断开的事件：注册/清理会话并输出日志。
 */
@Slf4j
public class ConnectionEventHandler extends ChannelInboundHandlerAdapter {

    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        var session = sessionManager.register(ctx.channel());
        log.info("channel active, remote={}", session.getRemoteAddress());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        var remote = ctx.channel().remoteAddress();
        sessionManager.unregister(ctx.channel());
        log.info("channel inactive, remote={}, online={}", remote, sessionManager.onlineCount());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        var remote = ctx.channel().remoteAddress();
        sessionManager.unregister(ctx.channel());
        log.error("channel exception, remote={}, online={}", remote, sessionManager.onlineCount(), cause);
        ctx.close();
    }
}
