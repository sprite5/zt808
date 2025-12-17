package cn.jascript.zt808.testkit;

import cn.jascript.zt808.constants.Protocol;
import cn.jascript.zt808.util.BcdUtil;
import cn.jascript.zt808.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public final class JT808TestKit {

    private static final int PHONE_LENGTH_BYTES = 6;

    private JT808TestKit() {
    }

    public static byte[] buildFrame(int msgId, String terminalId, int flowId, byte[] body) {
        ByteBuf payload = Unpooled.buffer();
        ByteBuf out = Unpooled.buffer();
        try {
            payload.writeShort(msgId);
            int bodyLength = Objects.isNull(body) ? 0 : body.length;
            int attr = bodyLength & Protocol.MAX_BODY_LENGTH;
            payload.writeShort(attr);
            payload.writeBytes(BcdUtil.fromString(terminalId, PHONE_LENGTH_BYTES));
            payload.writeShort(CodecUtil.toUnsignedShort(flowId));
            if (bodyLength > 0) {
                payload.writeBytes(body);
            }

            byte bcc = CodecUtil.calculateBcc(payload);

            out.writeByte(Protocol.HEADER);
            writeEscaped(out, payload);
            writeEscaped(out, bcc);
            out.writeByte(Protocol.TAIL);

            byte[] bytes = new byte[out.readableBytes()];
            out.getBytes(out.readerIndex(), bytes);
            return bytes;
        } finally {
            payload.release();
            out.release();
        }
    }

    public static byte[] authFrame(String terminalId, int flowId, String authCode) {
        String code = Objects.isNull(authCode) ? "" : authCode;
        byte[] body = code.getBytes(Charset.forName("GBK"));
        return buildFrame(0x0102, terminalId, flowId, body);
    }

    public static byte[] registerFrame(String terminalId,
                                       int flowId,
                                       int province,
                                       int city,
                                       String manufacturerId,
                                       String terminalModel,
                                       String terminalCode) {
        ByteBuf body = Unpooled.buffer();
        try {
            body.writeShort(province & 0xFFFF);
            body.writeShort(city & 0xFFFF);
            writeFixedGbk(body, manufacturerId, 5);
            writeFixedGbk(body, terminalModel, 8);
            writeFixedGbk(body, terminalCode, 7);

            byte[] bytes = new byte[body.readableBytes()];
            body.getBytes(body.readerIndex(), bytes);
            return buildFrame(0x0100, terminalId, flowId, bytes);
        } finally {
            body.release();
        }
    }

    public static byte[] locationFrame(String terminalId,
                                       int flowId,
                                       int alarm,
                                       int status,
                                       double latitude,
                                       double longitude,
                                       int height,
                                       int speed,
                                       int direction,
                                       LocalDateTime time) {
        // Location body minimum 28 bytes
        ByteBuf body = Unpooled.buffer();
        try {
            body.writeInt(alarm);
            body.writeInt(status);
            body.writeInt((int) Math.round(latitude * 1_000_000.0));
            body.writeInt((int) Math.round(longitude * 1_000_000.0));
            body.writeShort(height & 0xFFFF);
            body.writeShort(speed & 0xFFFF);
            body.writeShort(direction & 0xFFFF);
            body.writeBytes(bcdTime6(time));

            byte[] bytes = new byte[body.readableBytes()];
            body.getBytes(body.readerIndex(), bytes);
            return buildFrame(0x0200, terminalId, flowId, bytes);
        } finally {
            body.release();
        }
    }

    public static byte[] heartbeatFrame(String terminalId, int flowId) {
        return buildFrame(0x0002, terminalId, flowId, null);
    }

    public static byte[] unescape(byte[] escaped) {
        if (Objects.isNull(escaped) || escaped.length == 0) {
            return new byte[0];
        }
        byte[] tmp = new byte[escaped.length];
        int j = 0;
        for (int i = 0; i < escaped.length; i++) {
            byte b = escaped[i];
            if (b == Protocol.ESCAPE && i + 1 < escaped.length) {
                byte next = escaped[++i];
                if (next == Protocol.ESCAPE_FOR_HEADER) {
                    tmp[j++] = Protocol.HEADER;
                } else if (next == Protocol.ESCAPE_FOR_ESCAPE) {
                    tmp[j++] = Protocol.ESCAPE;
                } else {
                    tmp[j++] = b;
                    tmp[j++] = next;
                }
            } else {
                tmp[j++] = b;
            }
        }
        return Arrays.copyOf(tmp, j);
    }

    private static void writeEscaped(ByteBuf out, ByteBuf data) {
        for (int i = data.readerIndex(); i < data.writerIndex(); i++) {
            writeEscaped(out, data.getByte(i));
        }
    }

    private static void writeEscaped(ByteBuf out, byte value) {
        if (value == Protocol.HEADER) {
            out.writeByte(Protocol.ESCAPE).writeByte(Protocol.ESCAPE_FOR_HEADER);
        } else if (value == Protocol.ESCAPE) {
            out.writeByte(Protocol.ESCAPE).writeByte(Protocol.ESCAPE_FOR_ESCAPE);
        } else {
            out.writeByte(value);
        }
    }

    private static void writeFixedGbk(ByteBuf out, String s, int len) {
        byte[] bytes = Objects.isNull(s) ? new byte[0] : s.getBytes(Charset.forName("GBK"));
        int copyLen = Math.min(bytes.length, len);
        if (copyLen > 0) {
            out.writeBytes(bytes, 0, copyLen);
        }
        for (int i = copyLen; i < len; i++) {
            out.writeByte(0x00);
        }
    }

    private static byte[] bcdTime6(LocalDateTime time) {
        LocalDateTime t = Objects.isNull(time) ? LocalDateTime.of(2025, 1, 1, 0, 0, 0) : time;
        byte[] bytes = new byte[6];
        bytes[0] = toBcd(t.getYear() % 100);
        bytes[1] = toBcd(t.getMonthValue());
        bytes[2] = toBcd(t.getDayOfMonth());
        bytes[3] = toBcd(t.getHour());
        bytes[4] = toBcd(t.getMinute());
        bytes[5] = toBcd(t.getSecond());
        return bytes;
    }

    private static byte toBcd(int value) {
        int v = Math.max(0, Math.min(99, value));
        int high = v / 10;
        int low = v % 10;
        return (byte) ((high << 4) | low);
    }
}
