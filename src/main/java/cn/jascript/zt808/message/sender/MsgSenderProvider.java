package cn.jascript.zt808.message.sender;

import io.netty.channel.Channel;

import java.nio.charset.Charset;
import java.util.Map;

/**
 *
 */
public interface MsgSenderProvider {

    boolean sendMessage(Channel channel, String terminalId, Integer flowId, byte[] message, Map<String,Object> params);

    default boolean sendMessage(Channel channel, String terminalId, Integer flowId, String message, Map<String,Object> params){
        return this.sendMessage(channel, terminalId, flowId, message.getBytes(Charset.forName("GBK")), params);
    }
}
