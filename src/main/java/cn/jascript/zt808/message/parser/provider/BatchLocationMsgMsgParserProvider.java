package cn.jascript.zt808.message.parser.provider;

import cn.jascript.zt808.message.parser.MsgParserProvider;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.model.dto.LocationDTO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class BatchLocationMsgMsgParserProvider extends LocationMsgParserProvider implements MsgParserProvider {

    private static final int BATCH_HEADER_LEN = 2 + 1;


    @Override
    public List<BaseDTO> parse(Channel channel, TerminalMessage message) {
        if (Objects.isNull(message))
            return List.of();
        var body = message.getBody();
        if (Objects.isNull(body))
            return List.of();
        var buf = body.duplicate();
        if (buf.readableBytes() < BATCH_HEADER_LEN)
            return List.of();
        var terminalId = message.getTerminalId();
        int count = buf.readUnsignedShort();
        int type = buf.readUnsignedByte();
        var list = new ArrayList<BaseDTO>();
        for (int i = 0; i < count && buf.isReadable(2); i++) {
            int len = buf.readUnsignedShort();
            if (buf.readableBytes() < len)
                break;
            ByteBuf slice = buf.readSlice(len);
            //复用LocationMsgParserProvider解析位置
            LocationDTO dto = parseSingleLocation(slice, terminalId);
            dto.setReceiveTime(new Date());
            dto.getExt().put("batchType", String.valueOf(type));
            list.add(dto);
        }
        return list;
    }
}
