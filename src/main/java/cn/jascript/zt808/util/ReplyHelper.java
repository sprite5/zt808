package cn.jascript.zt808.util;

import cn.jascript.zt808.constants.MsgId;
import cn.jascript.zt808.model.PlatformMessage;
import cn.jascript.zt808.model.TerminalMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.util.Objects;

/**
 * 通用应答/特殊应答辅助工具
 */
public class ReplyHelper {

    private ReplyHelper() {
    }

    /**
     * 发送平台通用应答 (0x8001)
     */
    public static void sendGeneralReply(Channel channel, TerminalMessage message) {
        sendGeneralReply(channel, message, 0);
    }

    public static void sendGeneralReply(Channel channel, TerminalMessage message, boolean success){
        sendGeneralReply(channel, message, success ? 0 : 1);
    }

    public static void sendGeneralReply(Channel channel, TerminalMessage message, int result) {
        if (Objects.isNull(channel) || !channel.isActive())
            return;

        ByteBuf body = Unpooled.buffer(5);
        body.writeShort(message.getFlowId());
        body.writeShort(message.getMsgId());
        body.writeByte(result & 0xFF); // 0:成功, 1:失败, 2:消息有误, 3:不支持

        var reply = new PlatformMessage();
        reply.setTerminalId(message.getTerminalId());
        reply.setMsgId(MsgId.PLATFORM_GENERAL_REPLY);
        reply.setFlowId(message.getFlowId());
        reply.setBody(body);
        channel.writeAndFlush(reply);
    }
}
