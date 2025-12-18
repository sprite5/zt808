package cn.jascript.zt808.message.parser.provider;

import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.message.helper.ReplyHelper;
import cn.jascript.zt808.message.parser.MsgParserProvider;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.AuthDTO;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.session.SessionManager;
import io.netty.channel.Channel;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AuthMsgParserProvider implements MsgParserProvider {

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AppConfig.AuthConfig authConfig = AppConfig.get().getAuth();

    @Override
    public List<BaseDTO> parse(Channel channel, TerminalMessage message) {
        if (Objects.isNull(message))
            return List.of();
        var terminalId = message.getTerminalId();
        var body = message.getBody();
        // JT/T 808 protocol uses GBK encoding for strings
        var authCode = Objects.nonNull(body) ? body.toString(java.nio.charset.Charset.forName("GBK")).trim() : "";

        var blackList = authConfig.getBlackList();
        var inBlackList = Objects.nonNull(blackList) && blackList.contains(terminalId);
        var success = authConfig.getCode().equals(authCode) && !inBlackList;
        var reason = success ? "ok" : (inBlackList ? "blacklist" : "code_mismatch");

        // JT/T 808: use platform general reply (0x8001) result code to indicate auth success/failure
        // 0:成功, 1:失败
        ReplyHelper.sendGeneralReply(channel, message, success ? 0 : 1);
        //鉴权成功绑定设备id和channel
        if (success) {
            sessionManager.bindTerminal(terminalId, channel);
        }
        sessionManager.authorize(terminalId, success);
        var dto = new AuthDTO();
        dto.setTerminalId(terminalId);
        dto.setReceiveTime(new Date());
        dto.setSuccess(success);
        dto.setReason(reason);
        return List.of(dto);
    }
}
