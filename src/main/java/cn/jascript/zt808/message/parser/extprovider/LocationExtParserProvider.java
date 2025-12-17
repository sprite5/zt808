package cn.jascript.zt808.message.parser.extprovider;

import cn.jascript.zt808.message.parser.MsgExtParserProvider;
import cn.jascript.zt808.model.TerminalMessage;
import cn.jascript.zt808.model.dto.BaseDTO;
import cn.jascript.zt808.model.dto.LocationDTO;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;
import java.util.Objects;

public interface LocationExtParserProvider extends MsgExtParserProvider {

    void parse(LocationDTO dto, String statusHex, String alarmHex, String extraHex, ByteBuf body);

    @Override
    default void apply(TerminalMessage message, List<BaseDTO> dtos) {
        if (Objects.isNull(dtos) || dtos.isEmpty())
            return;
        for (var dto : dtos) {
            if (dto instanceof LocationDTO) {
                LocationDTO locationDTO = (LocationDTO) dto;
                var ext = locationDTO.getExt();
                var statusHex = (Objects.isNull(ext) ? null : ext.get("statusHex"));
                var extraHex = (Objects.isNull(ext) ? null : ext.get("extraHex"));
                var alarmHex = (Objects.isNull(ext) ? null : ext.get("alarmHex"));
                ByteBuf body = (Objects.isNull(message) || Objects.isNull(message.getBody())) ? Unpooled.EMPTY_BUFFER : message.getBody().duplicate();
                parse(locationDTO, statusHex, alarmHex, extraHex, body);
            }
        }
    }
}
