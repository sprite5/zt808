package cn.jascript.zt808.message.parser.provider;

import cn.jascript.zt808.config.AppConfig;
import cn.jascript.zt808.constants.MsgId;
import cn.jascript.zt808.message.parser.MsgParserProvider;
import cn.jascript.zt808.model.PlatformMessage;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.model.dto.RegisterDTO;
import cn.jascript.zt808.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class RegisterMsgParserProvider implements MsgParserProvider {

    private static final int LEN_MANUFACTURER = 5;
    private static final int LEN_MODEL_LONG = 20;
    private static final int LEN_MODEL_SHORT = 8;
    private static final int LEN_TERMINAL_ID = 7;
    private static final int LEN_SKIP_PROVINCE_CITY = 4;

    private static final int MIN_LEN_LONG = LEN_SKIP_PROVINCE_CITY + LEN_MANUFACTURER + LEN_MODEL_LONG + LEN_TERMINAL_ID;
    private static final int MIN_LEN_SHORT = LEN_SKIP_PROVINCE_CITY + LEN_MANUFACTURER + LEN_MODEL_SHORT + LEN_TERMINAL_ID;

    @Override
    public List<BaseDTO> parse(Channel channel, TerminalMessage message) {
        if (Objects.isNull(message))
            return List.of();

        // 1. 发送注册应答
        var authCode = AppConfig.get().getAuth().getCode();
        sendRegisterReply(channel, message, authCode);

        // 2. 解析注册消息体
        var terminalId = message.getTerminalId();
        var dto = new RegisterDTO();
        dto.setTerminalId(terminalId);
        dto.setReceiveTime(new Date());

        var body = message.getBody();
        if (Objects.nonNull(body) && body.readableBytes() >= MIN_LEN_SHORT) {
            var buf = body.duplicate();
            buf.skipBytes(LEN_SKIP_PROVINCE_CITY);

            dto.setManufacturerId(CodecUtil.readString(buf, LEN_MANUFACTURER));
            int modelLen = (buf.readableBytes() >= (LEN_MODEL_LONG + LEN_TERMINAL_ID)) ? LEN_MODEL_LONG : LEN_MODEL_SHORT;
            dto.setDeviceModel(CodecUtil.readString(buf, modelLen));
        }
        return List.of(dto);
    }

    /**
     * 发送终端注册应答 (0x8100)
     */
    void sendRegisterReply(Channel channel, TerminalMessage message, String authCode) {
        if (Objects.isNull(channel) || !channel.isActive())
            return;

        ByteBuf body = Unpooled.buffer();
        body.writeShort(message.getFlowId());
        body.writeByte(0); // 0:成功, 1:车辆已被注册, 2:数据库中无该车辆, 3:终端已被注册, 4:数据库中无该终端
        if (Objects.nonNull(authCode) && !authCode.isBlank()) {
            body.writeBytes(authCode.getBytes(java.nio.charset.Charset.forName("GBK")));
        }

        var reply = new PlatformMessage();
        reply.setTerminalId(message.getTerminalId());
        reply.setMsgId(MsgId.REGISTER_REPLY);
        reply.setFlowId(message.getFlowId());
        reply.setBody(body);
        channel.writeAndFlush(reply);
    }
}
