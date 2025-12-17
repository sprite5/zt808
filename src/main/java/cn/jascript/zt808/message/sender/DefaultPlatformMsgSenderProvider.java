package cn.jascript.zt808.message.sender;

import cn.jascript.zt808.constants.MsgId;
import cn.jascript.zt808.model.PlatformMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.Objects;

public class DefaultPlatformMsgSenderProvider implements MsgSenderProvider {

    public static final String PARAM_MSG_ID = "msgId";

    @Override
    public boolean sendMessage(Channel channel, String terminalId, Integer flowId, byte[] message, Map<String, Object> params) {
        if (Objects.isNull(channel) || !channel.isActive())
            return false;
        if (Objects.isNull(terminalId) || terminalId.isBlank())
            return false;
        if (Objects.isNull(flowId))
            return false;
        if (Objects.isNull(message))
            return false;

        int msgId = resolveMsgIdOrDefault(params);

        var platformMessage = new PlatformMessage();
        platformMessage.setTerminalId(terminalId);
        platformMessage.setMsgId(msgId);
        platformMessage.setFlowId(flowId);
        platformMessage.setBody(Unpooled.wrappedBuffer(message));
        channel.writeAndFlush(platformMessage);
        return true;
    }

    private static int resolveMsgIdOrDefault(Map<String, Object> params) {
        if (Objects.isNull(params) || params.isEmpty()) {
            return MsgId.PUSH_TEXT;
        }
        var msgIdObj = params.get(PARAM_MSG_ID);
        if (msgIdObj instanceof Number) {
            Number n = (Number) msgIdObj;
            return n.intValue();
        }
        if (msgIdObj instanceof String) {
            String s = (String) msgIdObj;
            try {
                String v = s.trim().toLowerCase();
                int id = v.startsWith("0x") ? Integer.parseInt(v.substring(2), 16) : Integer.parseInt(v);
                return id;
            } catch (Exception ignore) {
                return MsgId.PUSH_TEXT;
            }
        }
        return MsgId.PUSH_TEXT;
    }
}
