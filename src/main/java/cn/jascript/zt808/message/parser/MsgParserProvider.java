package cn.jascript.zt808.message.parser;

import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import io.netty.channel.Channel;

import java.util.List;

/**
 * 按 msgId 自定义解析入口：返回 DTO 列表，空列表表示不转发。
 */
public interface MsgParserProvider {

    List<BaseDTO> parse(Channel channel, TerminalMessage message);
}
