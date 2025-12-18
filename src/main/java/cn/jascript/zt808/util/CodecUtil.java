package cn.jascript.zt808.util;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public final class CodecUtil {

    private CodecUtil() {
    }

    // 计算 ByteBuf 的 BCC（按字节异或）
    public static byte calculateBcc(ByteBuf buf) {
        byte bcc = 0;
        for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {
            bcc ^= buf.getByte(i);
        }
        return bcc;
    }

    // 转换为无符号 short（0~65535）
    public static int toUnsignedShort(int value) {
        return value & 0xFFFF;
    }

    // 转换为无符号 byte（0~255）
    public static int toUnsignedByte(byte value) {
        return value & 0xFF;
    }

    // 读取 4 字节大端无符号整数（uint32）
    public static long readUnsignedInt32BE(byte[] bytes, int offset) {
        if (Objects.isNull(bytes) || offset < 0 || bytes.length < offset + 4) {
            throw new IllegalArgumentException("need at least 4 bytes from offset");
        }
        return ((long) toUnsignedByte(bytes[offset]) << 24)
                | ((long) toUnsignedByte(bytes[offset + 1]) << 16)
                | ((long) toUnsignedByte(bytes[offset + 2]) << 8)
                | ((long) toUnsignedByte(bytes[offset + 3]));
    }

    // 将大端字节数组转为 int
    public static int toInt(byte[] bytes) {
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    // 将字节数组转为大写 HEX 字符串
    public static String bytesToHex(byte[] data) {
        if (Objects.isNull(data) || data.length == 0)
            return "";
        var sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    // 判断某个 bit 位是否为 1
    public static boolean checkBit(int value, int bitIndex) {
        if ((value & (1 << bitIndex)) != 0)
            return true;
        return false;
    }

    // 从 ByteBuf 读取定长字符串（GBK）并去除末尾 0x00
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

    // 从 HEX 字符串解析无符号 byte（取前 2 个字符）
    public static int parseUnsignedByteHex(String hex) {
        if (Objects.isNull(hex) || hex.length() < 2) {
            throw new IllegalArgumentException("hex must have at least 2 chars");
        }
        return Integer.parseInt(hex.substring(0, 2), 16) & 0xFF;
    }

    // 从 HEX 字符串解析无符号 uint32（取前 8 个字符，大端）
    public static long parseUnsignedInt32Hex(String hex) {
        if (Objects.isNull(hex) || hex.length() < 8) {
            throw new IllegalArgumentException("hex must have at least 8 chars");
        }
        return Long.parseLong(hex.substring(0, 8), 16) & 0xFFFF_FFFFL;
    }

    // 将 HEX 字符串转为 byte[]（允许包含空白字符）
    public static byte[] hexToBytes(String hex) {
        String s = hex.replaceAll("\\s+", "");
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException("hex length must be even");
        }
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex character");
            }
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
