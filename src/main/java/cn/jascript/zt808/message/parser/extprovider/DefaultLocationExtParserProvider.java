package cn.jascript.zt808.message.parser.extprovider;

import cn.jascript.zt808.model.dto.LocationDTO;
import io.netty.buffer.ByteBuf;

/**
 * 默认空实现：不解析 status/extra 扩展字段。
 */
public class DefaultLocationExtParserProvider implements LocationExtParserProvider {

    @Override
    public void parse(LocationDTO dto, String statusHex,String alarmHex, String extraHex, ByteBuf body) {
        // no-op
    }
}
