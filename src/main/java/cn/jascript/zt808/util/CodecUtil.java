package cn.jascript.zt808.util;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public final class CodecUtil {

    private CodecUtil() {
    }

    public static byte calculateBcc(ByteBuf buf) {
        byte bcc = 0;
        for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {
            bcc ^= buf.getByte(i);
        }
        return bcc;
    }

    public static int toUnsignedShort(int value) {
        return value & 0xFFFF;
    }

    public static int toInt(byte[] bytes) {
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    public static String bytesToHex(byte[] data) {
        if (Objects.isNull(data) || data.length == 0)
            return "";
        var sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public static boolean checkBit(int value, int bitIndex) {
        if ((value & (1 << bitIndex)) != 0)
            return true;
        return false;
    }

    public static String readString(ByteBuf buf, int length) {
        if (buf.readableBytes() < length)
            return "";
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        // JT/T 808 uses GBK
        int end = bytes.length;
        while (end > 0 && bytes[end - 1] == 0x00) {
            end--;
        }
        return new String(bytes, 0, end, java.nio.charset.Charset.forName("GBK")).trim();
    }

    // 取低10位数值
    public static int getLowTenBitValue(short in){
        return in & 0x3FF;
    }
}
