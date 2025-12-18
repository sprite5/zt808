package cn.jascript.zt808.util;

import java.util.Objects;

public class HexUtil {

    private static final char[] HEX_UPPER = "0123456789ABCDEF".toCharArray();

    private HexUtil() {
    }

    // 将字节数组转为大写 HEX 字符串
    public static String toUpperHex(byte[] bytes) {
        if (Objects.isNull(bytes) || bytes.length == 0)
            return "";
        char[] out = new char[bytes.length * 2];
        int i = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            out[i++] = HEX_UPPER[v >>> 4];
            out[i++] = HEX_UPPER[v & 0x0F];
        }
        return new String(out);
    }
}
