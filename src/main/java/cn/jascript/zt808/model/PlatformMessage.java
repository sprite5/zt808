package cn.jascript.zt808.model;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class PlatformMessage {
    /**
     * 终端号
     */
    private String terminalId;

    /**
     * 平台消息ID(协议头msgId)
     */
    private int msgId;

    /**
     * 流水号
     */
    private int flowId;

    /**
     * 具体消息体
     */
    private ByteBuf body;
}
