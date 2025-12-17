package cn.jascript.zt808.session;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.SocketAddress;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话信息：终端号、通道与鉴权状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    /**
     * 终端号（可在鉴权后重新绑定）
     */
    private String terminalId;

    /**
     * Netty 通道
     */
    private Channel channel;

    /**
     * 远端地址
     */
    private SocketAddress remoteAddress;

    /**
     * 鉴权状态
     */
    private boolean authorized;

    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;

    /**
     * 通道唯一标识，便于根据 channelId 快速移除。
     */
    private String channelId;

    @Builder.Default
    private AtomicInteger flowIdCounter = new AtomicInteger(0);
}
