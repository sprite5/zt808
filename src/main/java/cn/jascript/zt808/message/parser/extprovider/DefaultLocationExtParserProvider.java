package cn.jascript.zt808.message.parser.extprovider;

import cn.jascript.zt808.model.dto.LocationDTO;
import cn.jascript.zt808.util.CodecUtil;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认实现：解析 extra 扩展字段里的常用 TLV（如 0x30/0x31）。
 */
public class DefaultLocationExtParserProvider implements LocationExtParserProvider {

    @Override
    public void parse(LocationDTO dto, String statusHex,String alarmHex, String extraHex, ByteBuf body) {
        if (Objects.isNull(dto) || Objects.isNull(extraHex) || extraHex.isBlank())
            return;

        var map = getExtIdHexMap(extraHex);
        if (map.isEmpty())
            return;

        var mileageBytes = map.get(0x01);
        if (Objects.nonNull(mileageBytes) && mileageBytes.length >= 4) {
            try {
                long mileage = CodecUtil.readUnsignedInt32BE(mileageBytes, 0);
                dto.getExt().put("mileage", String.valueOf(mileage));
                dto.getExt().put("mileageUnit", "0.1km");
            } catch (Exception ignore) {
            }
        }

        var gpsBytes = map.get(0x30);
        if (Objects.nonNull(gpsBytes) && gpsBytes.length >= 1) {
            try {
                dto.setGpsSignal(CodecUtil.toUnsignedByte(gpsBytes[0]));
            } catch (Exception ignore) {
            }
        }

        var netBytes = map.get(0x31);
        if (Objects.nonNull(netBytes) && netBytes.length >= 1) {
            try {
                dto.setNetworkSignal(CodecUtil.toUnsignedByte(netBytes[0]));
            } catch (Exception ignore) {
            }
        }
    }
    //从扩展中拆出扩展id和具体数据,TLV拆分
    private Map<Integer, byte[]> getExtIdHexMap(String extraHex){
        if (Objects.isNull(extraHex) || extraHex.isBlank()) {
            return Map.of();
        }

        final byte[] bytes;
        try {
            bytes = CodecUtil.hexToBytes(extraHex);
        } catch (Exception e) {
            return Map.of();
        }

        var map = new HashMap<Integer, byte[]>();
        int i = 0;
        while (i + 2 <= bytes.length) {
            int id = CodecUtil.toUnsignedByte(bytes[i++]);
            int len = CodecUtil.toUnsignedByte(bytes[i++]);
            if (i + len > bytes.length) {
                break;
            }

            map.put(id, Arrays.copyOfRange(bytes, i, i + len));
            i += len;
        }

        return map;
    }

}
