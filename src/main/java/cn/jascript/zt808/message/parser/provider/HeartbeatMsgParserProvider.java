package cn.jascript.zt808.message.parser.provider;

import cn.jascript.zt808.message.parser.MsgParserProvider;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.model.dto.HeartbeatDTO;
import cn.jascript.zt808.session.SessionManager;
import io.netty.channel.Channel;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HeartbeatMsgParserProvider implements MsgParserProvider {

    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public List<BaseDTO> parse(Channel channel, TerminalMessage message) {
        if (Objects.isNull(message))
            return List.of();
        var terminalId = message.getTerminalId();
        var dto = new HeartbeatDTO();
        dto.setTerminalId(terminalId);
        dto.setReceiveTime(new Date());
        return List.of(dto);
    }
}
