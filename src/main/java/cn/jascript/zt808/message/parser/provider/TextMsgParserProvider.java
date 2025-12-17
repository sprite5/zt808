package cn.jascript.zt808.message.parser.provider;

import cn.jascript.zt808.message.parser.MsgParserProvider;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.model.dto.TextDTO;
import io.netty.channel.Channel;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TextMsgParserProvider implements MsgParserProvider {

    @Override
    public List<BaseDTO> parse(Channel channel, TerminalMessage message) {
        if (Objects.isNull(message))
            return List.of();
        var body = message.getBody();
        if (Objects.isNull(body))
            return List.of();
        var terminalId = message.getTerminalId();
        var text = body.toString(Charset.forName("GBK")).trim();
        var dto = new TextDTO();
        dto.setTerminalId(terminalId);
        dto.setReceiveTime(new Date());
        dto.setText(text);
        return List.of(dto);
    }
}
